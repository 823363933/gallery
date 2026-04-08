package com.example.gallery

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gallery.ui.theme.GalleryTheme
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaViewerActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val mediaUriString = intent.getStringExtra("media_uri") ?: ""
        val mediaType = intent.getStringExtra("media_type") ?: ""
        val mediaName = intent.getStringExtra("media_name") ?: ""
        val currentIndex = intent.getIntExtra("current_index", 0)
        val allImageUris = intent.getStringArrayListExtra("all_image_uris") ?: arrayListOf()
        val allImageNames = intent.getStringArrayListExtra("all_image_names") ?: arrayListOf()

        val mediaUri = Uri.parse(mediaUriString)
        val type = MediaType.valueOf(mediaType)
        settingsManager = SettingsManager(this)

        setContent {
            GalleryTheme {
                if (type == MediaType.IMAGE && allImageUris.isNotEmpty()) {
                    ImageGalleryViewer(
                        initialIndex = currentIndex,
                        imageUris = allImageUris.map { Uri.parse(it) },
                        imageNames = allImageNames,
                        shouldInstantDelete = settingsManager.isInstantDeleteEnabled(),
                        onClose = { currentImageUri ->
                            handleMediaClose(
                                uri = currentImageUri,
                                shouldDelete = settingsManager.isInstantDeleteEnabled()
                            )
                        }
                    )
                } else {
                    MediaViewerScreen(
                        mediaUri = mediaUri,
                        mediaType = type,
                        mediaName = mediaName,
                        settingsManager = settingsManager,
                        shouldInstantDelete = settingsManager.isInstantDeleteEnabled(),
                        onClose = { shouldDelete ->
                            handleMediaClose(
                                uri = mediaUri,
                                shouldDelete = shouldDelete && settingsManager.isInstantDeleteEnabled()
                            )
                        }
                    )
                }
            }
        }
    }

    private fun handleMediaClose(uri: Uri, shouldDelete: Boolean) {
        var deleted = false
        if (shouldDelete) {
            deleted = deleteMediaFile(uri)
            if (deleted) {
                settingsManager.clearVideoPlaybackPosition(uri)
            }
        }

        if (deleted) {
            setResult(
                Activity.RESULT_OK,
                android.content.Intent().putExtra(EXTRA_MEDIA_DELETED, true)
            )
        }
        finish()
    }

    private fun deleteMediaFile(uri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(contentResolver, uri)
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val EXTRA_MEDIA_DELETED = "media_deleted"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGalleryViewer(
    initialIndex: Int,
    imageUris: List<Uri>,
    imageNames: List<String>,
    shouldInstantDelete: Boolean,
    onClose: (Uri) -> Unit
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageUris.size }
    )
    val scope = rememberCoroutineScope()

    BackHandler {
        onClose(imageUris[pagerState.currentPage])
    }

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(3000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUris[page])
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isControlsVisible = !isControlsVisible },
                contentScale = ContentScale.Fit
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isControlsVisible,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onClose(imageUris[pagerState.currentPage]) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (imageNames.isNotEmpty()) imageNames[pagerState.currentPage] else "图片",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${imageUris.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isControlsVisible && imageUris.size > 1,
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (pagerState.currentPage > 0) {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(
                            Icons.Default.NavigateBefore,
                            contentDescription = "上一张",
                            tint = if (pagerState.currentPage > 0) Color.White else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Text(
                        text = "${pagerState.currentPage + 1} / ${imageUris.size}",
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = {
                            scope.launch {
                                if (pagerState.currentPage < imageUris.size - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < imageUris.size - 1
                    ) {
                        Icon(
                            Icons.Default.NavigateNext,
                            contentDescription = "下一张",
                            tint = if (pagerState.currentPage < imageUris.size - 1) Color.White else Color.Gray
                        )
                    }
                }
            }
        }

        if (shouldInstantDelete && isControlsVisible) {
            Text(
                text = "关闭查看器后将删除当前图片",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    mediaUri: Uri,
    mediaType: MediaType,
    mediaName: String,
    settingsManager: SettingsManager,
    shouldInstantDelete: Boolean,
    onClose: (Boolean) -> Unit
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var canDeleteOnClose by remember { mutableStateOf(mediaType == MediaType.IMAGE) }

    BackHandler {
        onClose(canDeleteOnClose)
    }

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible && mediaType == MediaType.IMAGE) {
            delay(3000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { isControlsVisible = !isControlsVisible }
    ) {
        when (mediaType) {
            MediaType.IMAGE -> {
                SimpleImage(imageUri = mediaUri)
            }

            MediaType.VIDEO -> {
                VideoPlayer(
                    videoUri = mediaUri,
                    settingsManager = settingsManager,
                    onPlaybackCompletedChanged = { canDeleteOnClose = it }
                )
            }

            else -> {
                Text("不支持的媒体类型", color = Color.White)
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isControlsVisible,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onClose(canDeleteOnClose) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = mediaName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (mediaType == MediaType.VIDEO && shouldInstantDelete && canDeleteOnClose) {
            Text(
                text = "视频已播放完，关闭播放器后将删除文件",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun SimpleImage(imageUri: Uri) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUri)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun VideoPlayer(
    videoUri: Uri,
    settingsManager: SettingsManager,
    onPlaybackCompletedChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val initialPosition = remember { settingsManager.getVideoPlaybackPosition(videoUri) }
    var baseSpeed by remember { mutableStateOf(1f) }
    var isBoosted by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var playbackCompleted by remember { mutableStateOf(false) }
    val effectiveSpeed = if (isBoosted) baseSpeed * 2f else baseSpeed

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            if (initialPosition > 0) {
                seekTo(initialPosition)
            }
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val ended = playbackState == Player.STATE_ENDED
                playbackCompleted = ended
                onPlaybackCompletedChanged(ended)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK && playbackCompleted) {
                    playbackCompleted = false
                    onPlaybackCompletedChanged(false)
                }
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            if (playbackCompleted) {
                settingsManager.clearVideoPlaybackPosition(videoUri)
            } else {
                settingsManager.saveVideoPlaybackPosition(videoUri, exoPlayer.currentPosition)
            }
            exoPlayer.release()
        }
    }

    LaunchedEffect(effectiveSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(effectiveSpeed)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true

                    setOnLongClickListener {
                        isBoosted = true
                        false
                    }
                    setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_UP ||
                            event.actionMasked == MotionEvent.ACTION_CANCEL
                        ) {
                            isBoosted = false
                        }
                        false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showSpeedMenu = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = "播放速度",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format("%.1fx", effectiveSpeed),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            DropdownMenu(
                expanded = showSpeedMenu,
                onDismissRequest = { showSpeedMenu = false }
            ) {
                listOf(1f, 1.5f, 2f, 3f).forEach { speed ->
                    DropdownMenuItem(
                        text = { Text("${speed}x") },
                        onClick = {
                            baseSpeed = speed
                            isBoosted = false
                            showSpeedMenu = false
                        }
                    )
                }
            }
        }
    }
}
