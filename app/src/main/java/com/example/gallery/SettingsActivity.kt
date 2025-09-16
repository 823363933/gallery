package com.example.gallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gallery.ui.theme.GalleryTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                // 先获取持久化权限
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                // 获取文件夹名称
                val rootName = UriPermissionHelper.getFolderDisplayName(contentResolver, it)

                // 保存到设置
                settingsManager.saveDefaultFolder(it, rootName)

                android.util.Log.d("SettingsActivity", "默认文件夹设置完成，准备返回")

                // 设置结果并返回，让MainActivity知道需要刷新
                setResult(RESULT_OK)
                finish()

            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "设置默认文件夹失败", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsManager = SettingsManager(this)

        setContent {
            GalleryTheme {
                SettingsScreen(
                    settingsManager = settingsManager,
                    onSelectDefaultFolder = { folderPickerLauncher.launch(null) },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onSelectDefaultFolder: () -> Unit,
    onBack: () -> Unit
) {
    // 使用remember和mutableStateOf来响应式更新UI
    var defaultFolderName by remember { mutableStateOf(settingsManager.getDefaultFolderName()) }
    var defaultSlideshowSpeed by remember { mutableStateOf(settingsManager.getDefaultSlideshowSpeed()) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // 使用LaunchedEffect来监听设置变化
    LaunchedEffect(Unit) {
        defaultFolderName = settingsManager.getDefaultFolderName()
        defaultSlideshowSpeed = settingsManager.getDefaultSlideshowSpeed()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // 设置内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 默认文件夹设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "默认文件夹",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "设置应用启动时自动加载的文件夹",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (defaultFolderName != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "当前默认文件夹:",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = defaultFolderName!!,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            TextButton(
                                onClick = {
                                    settingsManager.clearDefaultFolder()
                                    defaultFolderName = null
                                }
                            ) {
                                Text("清除")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = onSelectDefaultFolder,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (defaultFolderName != null) "更改默认文件夹" else "选择默认文件夹")
                    }
                }
            }

            // 幻灯片设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "幻灯片播放",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "设置默认的播放间隔时间",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "默认播放间隔",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${defaultSlideshowSpeed}秒",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = { showSpeedDialog = true }
                        ) {
                            Text("修改")
                        }
                    }
                }
            }
        }
    }

    // 播放速度设置对话框
    if (showSpeedDialog) {
        var tempSpeed by remember { mutableIntStateOf(defaultSlideshowSpeed) }

        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("设置播放间隔") },
            text = {
                Column {
                    Text("选择幻灯片播放间隔时间: ${tempSpeed}秒")
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = tempSpeed.toFloat(),
                        onValueChange = { tempSpeed = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsManager.saveDefaultSlideshowSpeed(tempSpeed)
                        defaultSlideshowSpeed = tempSpeed
                        showSpeedDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSpeedDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}