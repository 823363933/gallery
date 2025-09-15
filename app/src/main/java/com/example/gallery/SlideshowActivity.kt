package com.example.gallery

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gallery.ui.theme.GalleryTheme
import kotlinx.coroutines.delay

class SlideshowActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏显示
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val imageUriStrings = intent.getStringArrayListExtra("image_uris") ?: arrayListOf()
        val imageUris = imageUriStrings.map { Uri.parse(it) }

        setContent {
            GalleryTheme {
                SlideshowScreen(
                    imageUris = imageUris,
                    onClose = { finish() },
                    onFinished = { finish() } // 添加完成回调
                )
            }
        }
    }
}

@Composable
fun SlideshowScreen(
    imageUris: List<Uri>,
    onClose: () -> Unit,
    onFinished: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var slideDuration by remember { mutableIntStateOf(3) } // 秒
    var transitionEffect by remember { mutableStateOf(TransitionEffect.FADE) }
    var showSettings by remember { mutableStateOf(false) }

    // 自动播放逻辑
    LaunchedEffect(isPlaying, currentIndex, slideDuration) {
        if (isPlaying && imageUris.isNotEmpty()) {
            delay(slideDuration * 1000L)
            val nextIndex = currentIndex + 1
            if (nextIndex >= imageUris.size) {
                // 播放完成，自动退出
                onFinished()
            } else {
                currentIndex = nextIndex
            }
        }
    }

    // 自动隐藏控制栏
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(5000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { isControlsVisible = !isControlsVisible }
    ) {
        if (imageUris.isNotEmpty()) {
            SlideshowImage(
                imageUri = imageUris[currentIndex],
                transitionEffect = transitionEffect
            )
        }

        // 顶部控制栏
        androidx.compose.animation.AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            TopControlBar(
                currentIndex = currentIndex,
                totalCount = imageUris.size,
                onClose = onClose,
                onShowSettings = { showSettings = true }
            )
        }

        // 底部控制栏
        androidx.compose.animation.AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            BottomControlBar(
                isPlaying = isPlaying,
                onPlayPause = { isPlaying = !isPlaying },
                onPrevious = {
                    if (imageUris.isNotEmpty()) {
                        currentIndex = if (currentIndex > 0) currentIndex - 1 else imageUris.size - 1
                    }
                },
                onNext = {
                    if (imageUris.isNotEmpty()) {
                        val nextIndex = currentIndex + 1
                        if (nextIndex >= imageUris.size) {
                            // 到达最后一张，询问是否退出
                            onFinished()
                        } else {
                            currentIndex = nextIndex
                        }
                    }
                }
            )
        }

        // 设置对话框
        if (showSettings) {
            SlideshowSettingsDialog(
                slideDuration = slideDuration,
                transitionEffect = transitionEffect,
                onDurationChange = { slideDuration = it },
                onTransitionEffectChange = { transitionEffect = it },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
fun SlideshowImage(
    imageUri: Uri,
    transitionEffect: TransitionEffect
) {
    val transition = updateTransition(targetState = imageUri, label = "image_transition")

    when (transitionEffect) {
        TransitionEffect.FADE -> {
            val alpha by transition.animateFloat(
                transitionSpec = { tween(800) },
                label = "alpha"
            ) { 1f }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = alpha),
                contentScale = ContentScale.Fit
            )
        }

        TransitionEffect.SLIDE -> {
            val offsetX by transition.animateFloat(
                transitionSpec = { tween(800) },
                label = "offsetX"
            ) { 0f }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(translationX = offsetX),
                contentScale = ContentScale.Fit
            )
        }

        TransitionEffect.SCALE -> {
            val scale by transition.animateFloat(
                transitionSpec = { tween(800) },
                label = "scale"
            ) { 1f }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun TopControlBar(
    currentIndex: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onShowSettings: () -> Unit
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onShowSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
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
                onClick = onPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "上一张",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "下一张",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideshowSettingsDialog(
    slideDuration: Int,
    transitionEffect: TransitionEffect,
    onDurationChange: (Int) -> Unit,
    onTransitionEffectChange: (TransitionEffect) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "幻灯片设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 播放速度设置
                Column {
                    Text(
                        text = "播放间隔: ${slideDuration}秒",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = slideDuration.toFloat(),
                        onValueChange = { onDurationChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }

                // 切换效果设置
                Column {
                    Text(
                        text = "切换效果",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TransitionEffect.values().forEach { effect ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = transitionEffect == effect,
                                onClick = { onTransitionEffectChange(effect) }
                            )
                            Text(
                                text = when (effect) {
                                    TransitionEffect.FADE -> "淡入淡出"
                                    TransitionEffect.SLIDE -> "滑动"
                                    TransitionEffect.SCALE -> "缩放"
                                },
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

enum class TransitionEffect {
    FADE, SLIDE, SCALE
}