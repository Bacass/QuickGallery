package com.lee.quickgallery.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val relativePath: String,
    val duration: Long? = null, // 비디오인 경우에만 사용
    val thumbnailUri: Uri? = null // 썸네일 URI (비디오인 경우)
)

class MediaStoreUtil(private val context: Context) {
    val thumbnailCacheManager = ThumbnailCacheManager(context)
    
    companion object {
        private const val TAG = "MediaStoreUtil"
        
        // 쿼리할 컬럼들
        private val PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        
        // 비디오용 추가 컬럼
        private val VIDEO_PROJECTION = PROJECTION + MediaStore.Video.Media.DURATION
    }
    
    /**
     * 모든 이미지를 조회합니다.
     */
    suspend fun getAllImages(): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = mutableListOf<MediaItem>()
        
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val selection = "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'image/%'"
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            
            context.contentResolver.query(
                collection,
                PROJECTION,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val relativePath = cursor.getString(relativePathColumn) ?: ""
                    
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    
                    images.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            size = size,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            relativePath = relativePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "이미지 조회 중 오류 발생")
        }
        
        Timber.tag(TAG).d("총 ${images.size}개의 이미지를 찾았습니다.")
        images
    }
    
    /**
     * 모든 비디오를 조회합니다.
     */
    suspend fun getAllVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<MediaItem>()
        
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            
            val selection = "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'video/%'"
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            
            context.contentResolver.query(
                collection,
                VIDEO_PROJECTION,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val relativePath = cursor.getString(relativePathColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    
                    videos.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            size = size,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            relativePath = relativePath,
                            duration = duration,
                            thumbnailUri = null // 지연 로딩으로 처리
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "비디오 조회 중 오류 발생")
        }
        
        Timber.tag(TAG).d("총 ${videos.size}개의 비디오를 찾았습니다.")
        videos
    }
    
    /**
     * 모든 미디어(이미지 + 비디오)를 조회합니다.
     */
    suspend fun getAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val allMedia = mutableListOf<MediaItem>()
        
        // 이미지와 비디오를 병렬로 조회
        val images = getAllImages()
        val videos = getAllVideos()
        
        allMedia.addAll(images)
        allMedia.addAll(videos)
        
        // 날짜순으로 정렬
        allMedia.sortByDescending { it.dateAdded }
        
        Timber.tag(TAG).d("총 ${allMedia.size}개의 미디어를 찾았습니다.")
        allMedia
    }
    
    /**
     * 특정 폴더의 미디어를 조회합니다.
     */
    suspend fun getMediaByFolder(folderPath: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val media = mutableListOf<MediaItem>()
        
        try {
            Timber.tag(TAG).d("폴더별 미디어 조회 시작: $folderPath")
            
            // 폴더 경로 정규화 (끝에 슬래시가 있으면 제거)
            val normalizedFolderPath = folderPath.trimEnd('/')
            Timber.tag(TAG).d("정규화된 폴더 경로: $normalizedFolderPath")
            
            // 이미지 조회
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            // Camera 폴더의 경우 다양한 경로 패턴 시도
            val imageSelection = if (normalizedFolderPath.contains("Camera", ignoreCase = true)) {
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'image/%' AND (${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?)"
            } else {
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'image/%' AND (${MediaStore.MediaColumns.RELATIVE_PATH} = ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} = ?)"
            }
            val imageSelectionArgs = if (normalizedFolderPath.contains("Camera", ignoreCase = true)) {
                arrayOf("%Camera%", "%DCIM/Camera%", "%Pictures/Camera%")
            } else {
                arrayOf(normalizedFolderPath, "$normalizedFolderPath/")
            }
            
            Timber.tag(TAG).d("이미지 쿼리: $imageSelection, args: ${imageSelectionArgs.joinToString()}")
            context.contentResolver.query(
                imageCollection,
                PROJECTION,
                imageSelection,
                imageSelectionArgs,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val relativePath = cursor.getString(relativePathColumn) ?: ""
                    
                    val contentUri = ContentUris.withAppendedId(imageCollection, id)
                    
                    media.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            size = size,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            relativePath = relativePath
                        )
                    )
                }
            }
            
            // 비디오 조회
            val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            
            // Camera 폴더의 경우 다양한 경로 패턴 시도
            val videoSelection = if (normalizedFolderPath.contains("Camera", ignoreCase = true)) {
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'video/%' AND (${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?)"
            } else {
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'video/%' AND (${MediaStore.MediaColumns.RELATIVE_PATH} = ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} = ?)"
            }
            val videoSelectionArgs = if (normalizedFolderPath.contains("Camera", ignoreCase = true)) {
                arrayOf("%Camera%", "%DCIM/Camera%", "%Pictures/Camera%")
            } else {
                arrayOf(normalizedFolderPath, "$normalizedFolderPath/")
            }
            
            Timber.tag(TAG).d("비디오 쿼리: $videoSelection, args: ${videoSelectionArgs.joinToString()}")
            context.contentResolver.query(
                videoCollection,
                VIDEO_PROJECTION,
                videoSelection,
                videoSelectionArgs,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val relativePath = cursor.getString(relativePathColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    
                    val contentUri = ContentUris.withAppendedId(videoCollection, id)
                    
                    media.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            size = size,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            relativePath = relativePath,
                            duration = duration
                        )
                    )
                }
            }
            
            Timber.tag(TAG).d("폴더별 미디어 조회 완료: ${media.size}개")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "폴더별 미디어 조회 중 오류 발생")
        }
        
        media
    }
    
    /**
     * 전체 이미지 수를 조회합니다.
     */
    suspend fun getTotalImageCount(): Int = withContext(Dispatchers.IO) {
        var totalCount = 0
        
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val selection = "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'image/%'"
            
            context.contentResolver.query(
                collection,
                arrayOf("COUNT(*)"),
                selection,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    totalCount = cursor.getInt(0)
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "이미지 수 조회 중 오류 발생")
        }
        
        Timber.tag(TAG).d("총 이미지 수: ${totalCount}장")
        totalCount
    }
    
    /**
     * 특정 폴더의 전체 미디어 수를 조회합니다.
     */
    suspend fun getMediaCountByFolder(folderPath: String): Int = withContext(Dispatchers.IO) {
        var totalCount = 0
        
        try {
            Timber.tag(TAG).d("폴더별 미디어 수 조회 시작: $folderPath")
            
            // 폴더 경로 정규화 (끝에 슬래시가 있으면 제거)
            val normalizedFolderPath = folderPath.trimEnd('/')
            
            // 이미지 수 조회
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val imageSelection = if (normalizedFolderPath.contains("Camera", ignoreCase = true)) {
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'image/%' AND (${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?)"
            } else {
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'image/%' AND (${MediaStore.MediaColumns.RELATIVE_PATH} = ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} = ?)"
            }
            val imageSelectionArgs = if (normalizedFolderPath.contains("Camera", ignoreCase = true)) {
                arrayOf("%Camera%", "%DCIM/Camera%", "%Pictures/Camera%")
            } else {
                arrayOf(normalizedFolderPath, "$normalizedFolderPath/")
            }
            
            context.contentResolver.query(
                imageCollection,
                arrayOf("COUNT(*)"),
                imageSelection,
                imageSelectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    totalCount += cursor.getInt(0)
                }
            }
            
            // 비디오 수 조회
            val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            
            val videoSelection = if (normalizedFolderPath.contains("Camera", ignoreCase = true)) {
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'video/%' AND (${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?)"
            } else {
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE 'video/%' AND (${MediaStore.MediaColumns.RELATIVE_PATH} = ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} = ?)"
            }
            val videoSelectionArgs = if (normalizedFolderPath.contains("Camera", ignoreCase = true)) {
                arrayOf("%Camera%", "%DCIM/Camera%", "%Pictures/Camera%")
            } else {
                arrayOf(normalizedFolderPath, "$normalizedFolderPath/")
            }
            
            context.contentResolver.query(
                videoCollection,
                arrayOf("COUNT(*)"),
                videoSelection,
                videoSelectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    totalCount += cursor.getInt(0)
                }
            }
            
            Timber.tag(TAG).d("폴더별 미디어 수 조회 완료: $folderPath - ${totalCount}개")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "폴더별 미디어 수 조회 중 오류 발생")
        }
        
        totalCount
    }
    
    /**
     * 폴더별로 그룹화된 미디어를 조회합니다.
     */
    suspend fun getMediaGroupedByFolder(): Map<String, List<MediaItem>> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, MutableList<MediaItem>>()
        
        try {
            // 모든 미디어를 조회
            val allMedia = getAllMedia()
            
            // 폴더별로 그룹화
            allMedia.forEach { mediaItem ->
                val folderPath = getFolderPathFromRelativePath(mediaItem.relativePath)
                if (folderPath.isNotEmpty()) {
                    folderMap.getOrPut(folderPath) { mutableListOf() }.add(mediaItem)
                }
            }
            
            // 각 폴더 내에서 날짜순 정렬
            folderMap.forEach { (_, mediaList) ->
                mediaList.sortByDescending { it.dateAdded }
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "폴더별 그룹화 중 오류 발생")
        }
        
        Timber.tag(TAG).d("총 ${folderMap.size}개의 폴더를 찾았습니다.")
        folderMap
    }
    
    /**
     * 비디오 썸네일을 가져옵니다 (지연 로딩).
     */
    suspend fun getVideoThumbnail(mediaItem: MediaItem): Uri? {
        return if (mediaItem.mimeType.startsWith("video/")) {
            try {
                Timber.tag(TAG).d("비디오 썸네일 요청: ${mediaItem.id}")
                val thumbnail = thumbnailCacheManager.getThumbnail(mediaItem.uri, mediaItem.id)
                if (thumbnail != null) {
                    Timber.tag(TAG).d("비디오 썸네일 로드 성공: ${mediaItem.id}")
                } else {
                    Timber.tag(TAG).w("비디오 썸네일 로드 실패: ${mediaItem.id}")
                }
                thumbnail
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "비디오 썸네일 가져오기 실패: ${mediaItem.id}")
                null
            }
        } else {
            null
        }
    }
    
    /**
     * RELATIVE_PATH에서 폴더 경로를 추출합니다.
     */
    private fun getFolderPathFromRelativePath(relativePath: String): String {
        return try {
            if (relativePath.isEmpty()) return ""
            
            // RELATIVE_PATH는 "Pictures/Instagram/" 형태
            // 마지막 슬래시를 제거하고 폴더명만 반환
            val trimmedPath = relativePath.trimEnd('/')
            if (trimmedPath.isEmpty()) return ""
            
            trimmedPath
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "RELATIVE_PATH에서 폴더 경로 추출 중 오류")
            ""
        }
    }
} 