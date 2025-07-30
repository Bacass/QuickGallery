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
            
            val latestMedia = mediaList.first() // 설정된 정렬 방식에 따라 정렬되어 있음
            val folderName = getFolderNameFromPath(folderPath)
            
            // 비디오인 경우 원본 URI를 사용하고, 썸네일은 지연 로딩으로 처리
            val thumbnailUri = latestMedia.uri
            
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