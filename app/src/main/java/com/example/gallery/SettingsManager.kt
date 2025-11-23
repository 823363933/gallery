package com.example.gallery

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log

class SettingsManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "gallery_settings"
        private const val KEY_DEFAULT_FOLDER_URI = "default_folder_uri"
        private const val KEY_DEFAULT_FOLDER_NAME = "default_folder_name"
        private const val KEY_DEFAULT_SLIDESHOW_SPEED = "default_slideshow_speed"
        private const val KEY_VIDEO_POSITION_PREFIX = "video_position_"

        // 默认播放速度（秒）
        const val DEFAULT_SLIDESHOW_SPEED = 3

        private const val TAG = "SettingsManager"
    }

    // 保存默认文件夹
    fun saveDefaultFolder(uri: Uri, name: String) {
        try {
            sharedPreferences.edit()
                .putString(KEY_DEFAULT_FOLDER_URI, uri.toString())
                .putString(KEY_DEFAULT_FOLDER_NAME, name)
                .apply()

            Log.d(TAG, "保存默认文件夹成功: $name -> $uri")
        } catch (e: Exception) {
            Log.e(TAG, "保存默认文件夹失败", e)
        }
    }

    // 获取默认文件夹URI
    fun getDefaultFolderUri(): Uri? {
        return try {
            val uriString = sharedPreferences.getString(KEY_DEFAULT_FOLDER_URI, null)
            if (uriString != null) {
                Log.d(TAG, "获取默认文件夹URI: $uriString")
                Uri.parse(uriString)
            } else {
                Log.d(TAG, "没有找到默认文件夹URI")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取默认文件夹URI失败", e)
            null
        }
    }

    // 获取默认文件夹名称
    fun getDefaultFolderName(): String? {
        return try {
            val name = sharedPreferences.getString(KEY_DEFAULT_FOLDER_NAME, null)
            Log.d(TAG, "获取默认文件夹名称: $name")
            name
        } catch (e: Exception) {
            Log.e(TAG, "获取默认文件夹名称失败", e)
            null
        }
    }

    // 清除默认文件夹
    fun clearDefaultFolder() {
        try {
            sharedPreferences.edit()
                .remove(KEY_DEFAULT_FOLDER_URI)
                .remove(KEY_DEFAULT_FOLDER_NAME)
                .apply()

            Log.d(TAG, "清除默认文件夹设置")
        } catch (e: Exception) {
            Log.e(TAG, "清除默认文件夹设置失败", e)
        }
    }

    // 保存默认播放速度
    fun saveDefaultSlideshowSpeed(speed: Int) {
        try {
            sharedPreferences.edit()
                .putInt(KEY_DEFAULT_SLIDESHOW_SPEED, speed)
                .apply()

            Log.d(TAG, "保存默认播放速度: $speed")
        } catch (e: Exception) {
            Log.e(TAG, "保存默认播放速度失败", e)
        }
    }

    // 获取默认播放速度
    fun getDefaultSlideshowSpeed(): Int {
        return try {
            val speed = sharedPreferences.getInt(KEY_DEFAULT_SLIDESHOW_SPEED, DEFAULT_SLIDESHOW_SPEED)
            Log.d(TAG, "获取默认播放速度: $speed")
            speed
        } catch (e: Exception) {
            Log.e(TAG, "获取默认播放速度失败", e)
            DEFAULT_SLIDESHOW_SPEED
        }
    }

    // 检查是否有默认文件夹
    fun hasDefaultFolder(): Boolean {
        val hasFolder = getDefaultFolderUri() != null
        Log.d(TAG, "检查是否有默认文件夹: $hasFolder")
        return hasFolder
    }

    fun saveVideoPlaybackPosition(uri: Uri, positionMs: Long) {
        try {
            sharedPreferences.edit()
                .putLong(KEY_VIDEO_POSITION_PREFIX + uri.toString(), positionMs)
                .apply()
            Log.d(TAG, "保存视频进度: $uri -> $positionMs")
        } catch (e: Exception) {
            Log.e(TAG, "保存视频进度失败: $uri", e)
        }
    }

    fun getVideoPlaybackPosition(uri: Uri): Long {
        return try {
            val pos = sharedPreferences.getLong(KEY_VIDEO_POSITION_PREFIX + uri.toString(), 0L)
            Log.d(TAG, "读取视频进度: $uri -> $pos")
            pos
        } catch (e: Exception) {
            Log.e(TAG, "读取视频进度失败: $uri", e)
            0L
        }
    }
}
