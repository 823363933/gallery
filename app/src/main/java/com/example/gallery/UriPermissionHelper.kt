package com.example.gallery

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log

object UriPermissionHelper {

    private const val TAG = "UriPermissionHelper"

    /**
     * 检查URI是否有有效的持久化权限并且可以访问
     */
    fun isUriAccessible(contentResolver: ContentResolver, uri: Uri): Boolean {
        return try {
            // 1. 检查是否有持久化权限
            val persistedUris = contentResolver.persistedUriPermissions
            val hasPermission = persistedUris.any {
                it.uri == uri && it.isReadPermission
            }

            if (!hasPermission) {
                Log.w(TAG, "URI没有持久化权限: $uri")
                return false
            }

            // 2. 尝试访问URI来验证它是否真的可用
            val treeDocumentId = DocumentsContract.getTreeDocumentId(uri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, treeDocumentId)

            contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
                Log.d(TAG, "URI可访问: $uri, 子项数量: ${cursor.count}")
                return true
            } ?: run {
                Log.w(TAG, "URI查询返回null: $uri")
                return false
            }

        } catch (e: SecurityException) {
            Log.w(TAG, "URI权限不足: $uri", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "URI访问检查失败: $uri", e)
            false
        }
    }

    /**
     * 获取文件夹的显示名称
     */
    fun getFolderDisplayName(contentResolver: ContentResolver, uri: Uri): String {
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
            Log.e(TAG, "获取文件夹名称失败", e)
            "根目录"
        }
    }
}