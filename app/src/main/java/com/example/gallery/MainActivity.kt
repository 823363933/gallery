package com.example.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gallery.ui.theme.GalleryTheme

class MainActivity : ComponentActivity() {

    private var selectedFolderUri by mutableStateOf<Uri?>(null)
    private var currentPath by mutableStateOf<String>("")
    private var mediaFiles by mutableStateOf<List<MediaFile>>(emptyList())
    private var navigationStack by mutableStateOf<List<NavigationItem>>(emptyList())
    private lateinit var settingsManager: SettingsManager

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            selectedFolderUri = it
            // 获取根目录名称
            val rootName = getRootFolderName(it)
            navigationStack = listOf(NavigationItem(it, rootName))
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            loadMediaFiles(it)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // 即便权限未授予，已有的 SAF 持久化权限依然可用，直接尝试加载
        loadDefaultFolderIfExists()
    }

    // 添加设置Activity结果监听器
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("MainActivity", "从设置页面返回，结果码: ${result.resultCode}")
        // 从设置页面返回后，重新检查默认文件夹
        if (selectedFolderUri == null) {
            loadDefaultFolderIfExists()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsManager = SettingsManager(this)
        requestPermissions()

        setContent {
            GalleryTheme {
                GalleryApp(
                    selectedFolderUri = selectedFolderUri,
                    currentPath = currentPath,
                    mediaFiles = mediaFiles,
                    canGoBack = navigationStack.size > 1,
                    onSelectFolder = { folderPickerLauncher.launch(null) },
                    onNavigateToFolder = { folder ->
                        navigateToFolder(folder)
                    },
                    onNavigateBack = {
                        navigateBack()
                    },
                    onOpenMedia = { mediaFile ->
                        openMediaViewer(mediaFile)
                    },
                    onStartSlideshow = {
                        startSlideshow()
                    },
                    onOpenSettings = {
                        openSettings()
                    },
                    onDeleteFile = { mediaFile ->
                        deleteMediaFile(mediaFile)
                    }
                )
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )

            val notGranted = permissions.filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            }

            if (notGranted.isNotEmpty()) {
                permissionLauncher.launch(notGranted.toTypedArray())
            } else {
                loadDefaultFolderIfExists()
            }
        } else {
            // 对旧系统直接依赖 SAF 的持久化 URI 权限
            loadDefaultFolderIfExists()
        }
    }

    private fun loadDefaultFolderIfExists() {
        val defaultUri = settingsManager.getDefaultFolderUri()
        if (defaultUri != null) {
            android.util.Log.d("MainActivity", "尝试加载默认文件夹: $defaultUri")

            if (UriPermissionHelper.isUriAccessible(contentResolver, defaultUri)) {
                selectedFolderUri = defaultUri
                val rootName = settingsManager.getDefaultFolderName()
                    ?: UriPermissionHelper.getFolderDisplayName(contentResolver, defaultUri)
                navigationStack = listOf(NavigationItem(defaultUri, rootName))
                loadMediaFiles(defaultUri)

                android.util.Log.d("MainActivity", "默认文件夹加载完成: $rootName")
            } else {
                android.util.Log.w("MainActivity", "默认文件夹无法访问，清除设置")
                settingsManager.clearDefaultFolder()
            }
        } else {
            android.util.Log.d("MainActivity", "没有设置默认文件夹")
        }
    }

    private fun getRootFolderName(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex) ?: "根目录"
                    } else "根目录"
                } else "根目录"
            } ?: "根目录"
        } catch (e: Exception) {
            Log.e("MainActivity", "获取根目录名称失败", e)
            "根目录"
        }
    }

    private fun loadMediaFiles(folderUri: Uri) {
        val files = mutableListOf<MediaFile>()

        try {
            Log.d("MainActivity", "开始加载文件夹: $folderUri")

            // 构建子文档URI
            val childrenUri = if (folderUri.toString().contains("/document/")) {
                // 这是一个文档URI，需要获取其document ID
                val documentId = DocumentsContract.getDocumentId(folderUri)
                DocumentsContract.buildChildDocumentsUriUsingTree(getTreeUri(folderUri), documentId)
            } else {
                // 这是一个树URI
                val treeDocumentId = DocumentsContract.getTreeDocumentId(folderUri)
                DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeDocumentId)
            }

            Log.d("MainActivity", "查询URI: $childrenUri")

            contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeTypeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

                Log.d("MainActivity", "找到 ${cursor.count} 个项目")

                while (cursor.moveToNext()) {
                    try {
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "未知"
                        val mimeType = if (mimeTypeIndex >= 0) cursor.getString(mimeTypeIndex) else ""
                        val docId = if (documentIdIndex >= 0) cursor.getString(documentIdIndex) else ""

                        val itemUri = DocumentsContract.buildDocumentUriUsingTree(getTreeUri(folderUri), docId)

                        Log.d("MainActivity", "文件: $name, 类型: $mimeType")

                        when {
                            mimeType == DocumentsContract.Document.MIME_TYPE_DIR -> {
                                files.add(MediaFile(name, itemUri, MediaType.FOLDER, mimeType))
                            }
                            mimeType?.startsWith("image/") == true -> {
                                files.add(MediaFile(name, itemUri, MediaType.IMAGE, mimeType))
                            }
                            mimeType?.startsWith("video/") == true -> {
                                files.add(MediaFile(name, itemUri, MediaType.VIDEO, mimeType))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "处理单个文件时出错", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "加载媒体文件失败", e)
        }

        mediaFiles = files.sortedWith(compareBy<MediaFile> { it.type != MediaType.FOLDER }.thenBy { it.name })
        currentPath = navigationStack.lastOrNull()?.name ?: "/"

        Log.d("MainActivity", "最终加载了 ${mediaFiles.size} 个文件")
    }

    private fun getTreeUri(uri: Uri): Uri {
        return if (uri.toString().contains("/tree/")) {
            // 已经是树URI
            uri
        } else {
            // 需要构建树URI
            val treeDocumentId = DocumentsContract.getTreeDocumentId(uri)
            DocumentsContract.buildTreeDocumentUri(uri.authority, treeDocumentId)
        }
    }

    private fun navigateToFolder(folder: MediaFile) {
        if (folder.type == MediaType.FOLDER) {
            Log.d("MainActivity", "导航到文件夹: ${folder.name}, URI: ${folder.uri}")

            // 添加到导航栈
            navigationStack = navigationStack + NavigationItem(folder.uri, folder.name)

            // 加载子文件夹内容
            loadMediaFiles(folder.uri)
        }
    }

    private fun navigateBack() {
        if (navigationStack.size > 1) {
            Log.d("MainActivity", "返回上级目录")
            navigationStack = navigationStack.dropLast(1)
            val previousItem = navigationStack.last()
            loadMediaFiles(previousItem.uri)
        }
    }

    private fun openMediaViewer(mediaFile: MediaFile) {
        val imageFiles = mediaFiles.filter { it.type == MediaType.IMAGE }
        val currentIndex = imageFiles.indexOf(mediaFile)

        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putExtra("media_uri", mediaFile.uri.toString())
            putExtra("media_type", mediaFile.type.name)
            putExtra("media_name", mediaFile.name)
            putExtra("current_index", currentIndex)
            putStringArrayListExtra("all_image_uris", ArrayList(imageFiles.map { it.uri.toString() }))
            putStringArrayListExtra("all_image_names", ArrayList(imageFiles.map { it.name }))
        }
        startActivity(intent)
    }

    private fun startSlideshow() {
        val imageFiles = mediaFiles.filter { it.type == MediaType.IMAGE }
        if (imageFiles.isNotEmpty()) {
            val intent = Intent(this, SlideshowActivity::class.java).apply {
                putStringArrayListExtra("image_uris", ArrayList(imageFiles.map { it.uri.toString() }))
                putExtra("default_speed", settingsManager.getDefaultSlideshowSpeed())
            }
            startActivity(intent)
        }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        settingsLauncher.launch(intent)
    }

    private fun deleteMediaFile(mediaFile: MediaFile) {
        try {
            val deleted = DocumentsContract.deleteDocument(contentResolver, mediaFile.uri)
            if (deleted) {
                // 重新加载当前文件夹
                val currentFolder = navigationStack.lastOrNull()?.uri
                if (currentFolder != null) {
                    loadMediaFiles(currentFolder)
                }
                Log.d("MainActivity", "删除文件成功: ${mediaFile.name}")
            } else {
                Log.w("MainActivity", "删除文件失败: ${mediaFile.name}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "删除文件时出错: ${mediaFile.name}", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryApp(
    selectedFolderUri: Uri?,
    currentPath: String,
    mediaFiles: List<MediaFile>,
    canGoBack: Boolean,
    onSelectFolder: () -> Unit,
    onNavigateToFolder: (MediaFile) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenMedia: (MediaFile) -> Unit,
    onStartSlideshow: () -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteFile: (MediaFile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部工具栏
        TopAppBar(
            title = {
                Text(
                    text = if (selectedFolderUri != null) "图库 - $currentPath" else "图库",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            navigationIcon = {
                if (canGoBack) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回上级")
                    }
                }
            },
            actions = {
                if (selectedFolderUri != null) {
                    IconButton(onClick = onStartSlideshow) {
                        Icon(Icons.Default.Slideshow, contentDescription = "幻灯片播放")
                    }
                }
                IconButton(onClick = onSelectFolder) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "选择文件夹")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (selectedFolderUri == null) {
            // 欢迎界面
            WelcomeScreen(onSelectFolder)
        } else {
            // 文件列表
            MediaFilesList(
                mediaFiles = mediaFiles,
                onNavigateToFolder = onNavigateToFolder,
                onOpenMedia = onOpenMedia,
                onDeleteFile = onDeleteFile
            )
        }
    }
}

@Composable
fun WelcomeScreen(onSelectFolder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "欢迎使用图库",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "请选择一个文件夹来浏览其中的图片和视频",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSelectFolder,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择文件夹", fontSize = 16.sp)
        }
    }
}

@Composable
fun MediaFilesList(
    mediaFiles: List<MediaFile>,
    onNavigateToFolder: (MediaFile) -> Unit,
    onOpenMedia: (MediaFile) -> Unit,
    onDeleteFile: (MediaFile) -> Unit
) {
    if (mediaFiles.isEmpty()) {
        // 显示空状态
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "此文件夹为空",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(mediaFiles) { mediaFile ->
                MediaFileItem(
                    mediaFile = mediaFile,
                    onClick = {
                        when (mediaFile.type) {
                            MediaType.FOLDER -> onNavigateToFolder(mediaFile)
                            MediaType.IMAGE, MediaType.VIDEO -> onOpenMedia(mediaFile)
                        }
                    },
                    onDelete = { onDeleteFile(mediaFile) }
                )
            }
        }
    }
}

@Composable
fun MediaFileItem(
    mediaFile: MediaFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标或缩略图
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when (mediaFile.type) {
                    MediaType.FOLDER -> {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    MediaType.IMAGE -> {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaFile.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(android.R.drawable.ic_menu_gallery)
                        )
                    }
                    MediaType.VIDEO -> {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文件名
            Text(
                text = mediaFile.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // 删除按钮（对于文件和文件夹）
            IconButton(
                onClick = { showDeleteDialog = true }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // 文件类型指示器
            if (mediaFile.type == MediaType.FOLDER) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = {
                Text(
                    when (mediaFile.type) {
                        MediaType.FOLDER -> "确定要删除文件夹 \"${mediaFile.name}\" 吗？这将删除其中的所有内容。"
                        else -> "确定要删除文件 \"${mediaFile.name}\" 吗？"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

data class MediaFile(
    val name: String,
    val uri: Uri,
    val type: MediaType,
    val mimeType: String?
)

data class NavigationItem(
    val uri: Uri,
    val name: String
)

enum class MediaType {
    FOLDER, IMAGE, VIDEO
}