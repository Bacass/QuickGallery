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
            
            // 비디오인 경우 썸네일 URI 사용, 이미지인 경우 원본 URI 사용
            val thumbnailUri = if (latestMedia.mimeType.startsWith("video/")) {
                latestMedia.thumbnailUri ?: latestMedia.uri
            } else {
                latestMedia.uri
            }
            
            return FolderItem(
                folderPath = folderPath,
                folderName = folderName,
                thumbnailUri = thumbnailUri,
                mediaCount = mediaList.size,
                latestMedia = latestMedia
            )
        }
        
        private fun getFolderNameFromPath(path: String): String {
            return if (path.isEmpty()) {
                "Unknown"
            } else {
                path.substringAfterLast('/', path)
            }
        }
    }
} 