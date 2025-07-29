package com.lee.quickgallery.model

import android.net.Uri
import com.lee.quickgallery.util.MediaItem

data class FolderItem(
    val folderPath: String,
    val folderName: String,
    val thumbnailUri: Uri,
    val mediaCount: Int,
    val latestMedia: MediaItem
) {
    companion object {
        fun create(folderPath: String, mediaList: List<MediaItem>): FolderItem? {
            if (mediaList.isEmpty()) return null
            
            val latestMedia = mediaList.first() // 이미 날짜순으로 정렬되어 있음
            val folderName = getFolderNameFromPath(folderPath)
            
            return FolderItem(
                folderPath = folderPath,
                folderName = folderName,
                thumbnailUri = latestMedia.uri,
                mediaCount = mediaList.size,
                latestMedia = latestMedia
            )
        }
        
        private fun getFolderNameFromPath(path: String): String {
            return path.substringAfterLast('/', "")
        }
    }
} 