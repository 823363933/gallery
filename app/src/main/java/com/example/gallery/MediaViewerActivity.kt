package com.example.gallery

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.launch

class MediaViewerActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏显示
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

        setContent {
            GalleryTheme {
                if (type == MediaType.IMAGE && allImageUris.isNotEmpty()) {
                    // 图片浏览器（支持左右滑动）
                    ImageGalleryViewer(
                        initialIndex = currentIndex,
                        imageUris = allImageUris.map { Uri.parse(it) },
                        imageNames = allImageNames,
                        onClose = { finish() }
                    )
                } else {
                    // 单个媒体查看器
                    MediaViewerScreen(
                        mediaUri = mediaUri,
                        mediaType = type,
                        mediaName = mediaName,
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.release()
        exoPlayer = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGalleryViewer(
    initialIndex: Int,
    imageUris: List<Uri>,
    imageNames: List<String>,
    onClose: () -> Unit
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageUris.size }
    )
    val scope = rememberCoroutineScope()

    // 自动隐藏控制栏
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            kotlinx.coroutines.delay(3000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 图片浏览器
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

        // 顶部控制栏
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
                    IconButton(onClick = onClose) {
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

        // 底部导航控制
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    mediaUri: Uri,
    mediaType: MediaType,
    mediaName: String,
    onClose: () -> Unit
) {
    var isControlsVisible by remember { mutableStateOf(true) }

    // 自动隐藏控制栏
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible && mediaType == MediaType.IMAGE) {
            kotlinx.coroutines.delay(3000)
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
                SimpleImage(
                    imageUri = mediaUri
                )
            }
            MediaType.VIDEO -> {
                VideoPlayer(
                    videoUri = mediaUri
                )
            }
            else -> {
                // 不应该到达这里
                Text("不支持的媒体类型", color = Color.White)
            }
        }

        // 顶部控制栏
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
                    IconButton(onClick = onClose) {
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
    }
}

@Composable
fun SimpleImage(
    imageUri: Uri
) {
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
    videoUri: Uri
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}