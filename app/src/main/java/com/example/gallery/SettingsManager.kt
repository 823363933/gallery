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
        private const val KEY_INSTANT_DELETE_ENABLED = "instant_delete_enabled"
        private const val KEY_VIDEO_POSITION_PREFIX = "video_position_"

        const val DEFAULT_SLIDESHOW_SPEED = 3

        private const val TAG = "SettingsManager"
    }

    fun saveDefaultFolder(uri: Uri, name: String) {
        try {
            sharedPreferences.edit()
                .putString(KEY_DEFAULT_FOLDER_URI, uri.toString())
                .putString(KEY_DEFAULT_FOLDER_NAME, name)
                .apply()

            Log.d(TAG, "Saved default folder: $name -> $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save default folder", e)
        }
    }

    fun getDefaultFolderUri(): Uri? {
        return try {
            sharedPreferences.getString(KEY_DEFAULT_FOLDER_URI, null)?.let(Uri::parse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read default folder URI", e)
            null
        }
    }

    fun getDefaultFolderName(): String? {
        return try {
            sharedPreferences.getString(KEY_DEFAULT_FOLDER_NAME, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read default folder name", e)
            null
        }
    }

    fun clearDefaultFolder() {
        try {
            sharedPreferences.edit()
                .remove(KEY_DEFAULT_FOLDER_URI)
                .remove(KEY_DEFAULT_FOLDER_NAME)
                .apply()

            Log.d(TAG, "Cleared default folder")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear default folder", e)
        }
    }

    fun saveDefaultSlideshowSpeed(speed: Int) {
        try {
            sharedPreferences.edit()
                .putInt(KEY_DEFAULT_SLIDESHOW_SPEED, speed)
                .apply()

            Log.d(TAG, "Saved default slideshow speed: $speed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save default slideshow speed", e)
        }
    }

    fun getDefaultSlideshowSpeed(): Int {
        return try {
            sharedPreferences.getInt(KEY_DEFAULT_SLIDESHOW_SPEED, DEFAULT_SLIDESHOW_SPEED)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read default slideshow speed", e)
            DEFAULT_SLIDESHOW_SPEED
        }
    }

    fun hasDefaultFolder(): Boolean {
        return getDefaultFolderUri() != null
    }

    fun saveInstantDeleteEnabled(enabled: Boolean) {
        try {
            sharedPreferences.edit()
                .putBoolean(KEY_INSTANT_DELETE_ENABLED, enabled)
                .apply()
            Log.d(TAG, "Saved instant delete enabled: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save instant delete enabled", e)
        }
    }

    fun isInstantDeleteEnabled(): Boolean {
        return try {
            sharedPreferences.getBoolean(KEY_INSTANT_DELETE_ENABLED, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read instant delete enabled", e)
            false
        }
    }

    fun saveVideoPlaybackPosition(uri: Uri, positionMs: Long) {
        try {
            sharedPreferences.edit()
                .putLong(KEY_VIDEO_POSITION_PREFIX + uri.toString(), positionMs)
                .apply()
            Log.d(TAG, "Saved video playback position: $uri -> $positionMs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video playback position: $uri", e)
        }
    }

    fun getVideoPlaybackPosition(uri: Uri): Long {
        return try {
            sharedPreferences.getLong(KEY_VIDEO_POSITION_PREFIX + uri.toString(), 0L)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read video playback position: $uri", e)
            0L
        }
    }

    fun clearVideoPlaybackPosition(uri: Uri) {
        try {
            sharedPreferences.edit()
                .remove(KEY_VIDEO_POSITION_PREFIX + uri.toString())
                .apply()
            Log.d(TAG, "Cleared video playback position: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear video playback position: $uri", e)
        }
    }
}
