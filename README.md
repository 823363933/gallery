# Android Gallery App

一个基于 Android Jetpack Compose 构建的现代化图库应用，支持浏览图片、播放视频和幻灯片播放。

## 功能特性

### 核心功能
- **文件夹浏览**: 支持选择和浏览设备上的任意文件夹
- **图片查看**: 支持常见图片格式的查看和缩放
- **视频播放**: 内置视频播放器，支持常见视频格式
- **幻灯片播放**: 自动播放图片，支持多种转场效果
- **文件管理**: 支持删除文件和文件夹

### 高级功能
- **默认文件夹**: 设置默认启动文件夹，免去每次重新选择
- **自定义播放速度**: 可调节幻灯片播放间隔（1-10秒）
- **多种转场效果**: 淡入淡出、滑动、缩放等转场效果
- **权限管理**: 智能的文件访问权限管理和验证

## 技术栈

- **UI框架**: Jetpack Compose
- **架构**: MVVM 架构模式
- **图片加载**: Coil
- **视频播放**: ExoPlayer
- **文件访问**: Storage Access Framework (SAF)
- **数据存储**: SharedPreferences
- **权限管理**: Activity Result API

## 系统要求

- Android API 24+ (Android 7.0)
- 目标 SDK: Android 14 (API 34)
- Kotlin 1.9.20+
- Gradle 8.1.4+

## 项目结构

```
app/src/main/java/com/example/gallery/
├── MainActivity.kt              # 主界面，文件浏览
├── MediaViewerActivity.kt       # 媒体查看界面
├── SlideshowActivity.kt         # 幻灯片播放界面
├── SettingsActivity.kt          # 设置界面
├── SettingsManager.kt           # 设置数据管理
├── UriPermissionHelper.kt       # URI权限辅助类
└── ui/theme/                    # UI主题相关
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## 核心组件

### MainActivity
- 文件夹选择和导航
- 媒体文件列表显示
- 文件删除功能
- 设置页面入口

### MediaViewerActivity
- 单张图片查看
- 图片集浏览（左右滑动）
- 视频播放
- 全屏显示

### SlideshowActivity  
- 自动幻灯片播放
- 播放控制（播放/暂停/上一张/下一张）
- 转场效果设置
- 播放速度调节

### SettingsActivity
- 默认文件夹设置
- 播放速度配置
- 设置数据持久化

## 权限要求

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.INTERNET" />
```

## 构建说明

### 克隆项目
```bash
git clone <repository-url>
cd gallery-app
```

### 构建应用
```bash
./gradlew assembleDebug
```

### 安装到设备
```bash
./gradlew installDebug
```

## 使用说明

### 首次使用
1. 启动应用后，点击"选择文件夹"按钮
2. 在文件选择器中选择包含图片或视频的文件夹
3. 授予应用访问该文件夹的权限

### 设置默认文件夹
1. 点击工具栏的设置图标
2. 选择"选择默认文件夹"
3. 选择常用的文件夹
4. 下次启动应用会自动加载该文件夹

### 幻灯片播放
1. 在包含图片的文件夹中，点击幻灯片图标
2. 使用底部控制栏控制播放
3. 点击设置图标可调整播放间隔和转场效果

### 删除文件
1. 在文件列表中，点击文件右侧的删除图标
2. 确认删除操作
3. 文件将被永久删除

## 依赖库

```gradle
// Android 核心库
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
implementation 'androidx.activity:activity-compose:1.8.2'

// Compose BOM
implementation platform('androidx.compose:compose-bom:2023.10.01')
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.material3:material3'
implementation 'androidx.compose.foundation:foundation'

// 导航
implementation 'androidx.navigation:navigation-compose:2.7.5'

// 图片加载
implementation 'io.coil-kt:coil-compose:2.5.0'

// 视频播放
implementation 'com.google.android.exoplayer:exoplayer:2.19.1'
```

## 注意事项

### 存储访问
- 使用 Storage Access Framework (SAF) 进行文件访问
- 需要用户主动授权文件夹访问权限
- 支持跨设备的文件夹访问（如SD卡、USB存储等）

### 性能优化
- 使用 Coil 进行图片懒加载和缓存
- ExoPlayer 提供高效的视频播放
- Compose 提供声明式UI和高效重组

### 兼容性
- 支持 Android 7.0+ 设备
- 适配横竖屏切换
- 支持不同屏幕尺寸

## 开发说明

### 添加新的媒体格式支持
在 `MediaType` 枚举中添加新类型，并在 `loadMediaFiles` 方法中添加相应的 MIME 类型判断。

### 扩展转场效果
在 `TransitionEffect` 枚举中添加新效果，并在 `SlideshowImage` 组件中实现相应动画。

### 自定义主题
修改 `ui/theme/` 目录下的主题文件来调整应用外观。

## 许可证

此项目仅供学习和个人使用。
