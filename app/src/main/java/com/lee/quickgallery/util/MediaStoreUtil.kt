package com.lee.quickgallery.util

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import android.content.IntentSender

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
        
        // 이미지 조회 완료
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
        
        // 비디오 조회 완료
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
        
        // 전체 미디어 조회 완료
        allMedia
    }
    
    /**
     * 특정 폴더의 미디어를 조회합니다.
     */
    suspend fun getMediaByFolder(folderPath: String, sortType: SortType = SortType.TIME_DESC): List<MediaItem> = withContext(Dispatchers.IO) {
        val media = mutableListOf<MediaItem>()
        
        try {
            // 폴더별 미디어 조회 시작
            
            // 폴더 경로 정규화 (끝에 슬래시가 있으면 제거)
            val normalizedFolderPath = folderPath.trimEnd('/')
            // 폴더 경로 정규화 완료
            
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
            
            // 데이터베이스에서 가져올 때 기본 정렬은 ID 순으로 하고, 나중에 메모리에서 정렬
            val baseSortOrder = "${MediaStore.MediaColumns._ID} ASC"
            
            // 이미지 쿼리 실행
            context.contentResolver.query(
                imageCollection,
                PROJECTION,
                imageSelection,
                imageSelectionArgs,
                baseSortOrder
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
            
            // 비디오 쿼리 실행
            context.contentResolver.query(
                videoCollection,
                VIDEO_PROJECTION,
                videoSelection,
                videoSelectionArgs,
                baseSortOrder
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
            
            
            // 폴더별 미디어 조회 완료
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "폴더별 미디어 조회 중 오류 발생")
        }
        
        // 선택된 정렬 방식에 따라 정렬
        val sortedMedia = when (sortType) {
            SortType.NAME_ASC -> media.sortedBy { it.name.lowercase() }
            SortType.NAME_DESC -> media.sortedByDescending { it.name.lowercase() }
            SortType.TIME_ASC -> media.sortedBy { it.dateAdded }
            SortType.TIME_DESC -> media.sortedByDescending { it.dateAdded }
        }
        
        // 정렬 완료
        sortedMedia
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
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                null,
                null
            )?.use { cursor ->
                totalCount = cursor.count
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "이미지 수 조회 중 오류 발생")
        }
        
        // 이미지 수 조회 완료
        totalCount
    }
    
    /**
     * 특정 폴더의 전체 미디어 수를 조회합니다.
     */
    suspend fun getMediaCountByFolder(folderPath: String): Int = withContext(Dispatchers.IO) {
        var totalCount = 0
        
        try {
            // 폴더별 미디어 수 조회 시작
            
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
                arrayOf(MediaStore.MediaColumns._ID),
                imageSelection,
                imageSelectionArgs,
                null
            )?.use { cursor ->
                totalCount += cursor.count
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
                arrayOf(MediaStore.MediaColumns._ID),
                videoSelection,
                videoSelectionArgs,
                null
            )?.use { cursor ->
                totalCount += cursor.count
            }
            
            // 폴더별 미디어 수 조회 완료
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "폴더별 미디어 수 조회 중 오류 발생")
        }
        
        totalCount
    }
    
    /**
     * 폴더별로 그룹화된 미디어를 조회합니다.
     */
    suspend fun getMediaGroupedByFolder(sortType: SortType = SortType.TIME_DESC): Map<String, List<MediaItem>> = withContext(Dispatchers.IO) {
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
            
            // 각 폴더 내에서 선택된 정렬 방식으로 정렬
            folderMap.forEach { (_, mediaList) ->
                when (sortType) {
                    SortType.NAME_ASC -> mediaList.sortBy { it.name.lowercase() }
                    SortType.NAME_DESC -> mediaList.sortByDescending { it.name.lowercase() }
                    SortType.TIME_ASC -> mediaList.sortBy { it.dateAdded }
                    SortType.TIME_DESC -> mediaList.sortByDescending { it.dateAdded }
                }
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "폴더별 그룹화 중 오류 발생")
        }
        
        // 폴더별 그룹화 완료
        folderMap
    }
    
    /**
     * 비디오 썸네일을 가져옵니다 (지연 로딩).
     */
    suspend fun getVideoThumbnail(mediaItem: MediaItem): Uri? {
        return if (mediaItem.mimeType.startsWith("video/")) {
            try {
                // 비디오 썸네일 요청
                val thumbnail = thumbnailCacheManager.getThumbnail(mediaItem.uri, mediaItem.id)
                if (thumbnail != null) {
                    // 비디오 썸네일 로드 성공
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
     * URI에서 실제 파일 객체를 가져옵니다.
     */
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            when (uri.scheme) {
                "file" -> File(uri.path ?: "")
                "content" -> {
                    // Content URI인 경우 임시 파일로 복사
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("temp_", ".tmp", context.cacheDir)
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "URI에서 파일 가져오기 실패: $uri")
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
    
    /**
     * 미디어 파일을 이동합니다.
     */
    suspend fun moveMediaItems(
        mediaItems: List<MediaItem>,
        targetFolderPath: String,
        onRecoverableAction: ((IntentSender) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        
        try {
            for (mediaItem in mediaItems) {
                try {
                    // 대상 폴더 생성
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val targetDir = if (targetFolderPath.contains("/")) {
                        File(Environment.getExternalStorageDirectory(), targetFolderPath)
                    } else {
                        File(picturesDir, targetFolderPath)
                    }
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    
                    val targetFile = File(targetDir, mediaItem.name)
                    
                    // 파일이 이미 존재하는지 확인하고 삭제
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    
                    // MediaStore URI에서 파일 복사
                    val inputStream = context.contentResolver.openInputStream(mediaItem.uri)
                    if (inputStream != null) {
                        inputStream.use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        // 복사 성공 확인
                        if (targetFile.exists() && targetFile.length() > 0) {
                            // MediaStore에서 원본 파일 삭제 시도
                            try {
                                val deleteSuccess = deleteMediaItemFromStore(mediaItem, onRecoverableAction)
                                
                                if (deleteSuccess) {
                                    successCount++
                                    Timber.tag(TAG).d("미디어 이동 성공: ${mediaItem.name}")
                                } else {
                                    // 삭제 실패 시 복사된 파일도 삭제
                                    targetFile.delete()
                                    Timber.tag(TAG).w("원본 삭제 실패로 이동 취소: ${mediaItem.name}")
                                }
                            } catch (e: RecoverableSecurityException) {
                                // 권한이 없는 경우 복사만 수행하고 원본은 유지
                                Timber.tag(TAG).w("권한 부족으로 복사만 수행: ${mediaItem.name}")
                                onRecoverableAction?.invoke(e.userAction.actionIntent.intentSender)
                                successCount++
                                Timber.tag(TAG).d("미디어 복사 성공 (원본 유지): ${mediaItem.name}")
                            }
                        } else {
                            Timber.tag(TAG).w("파일 복사 실패: ${mediaItem.name}")
                        }
                    } else {
                        Timber.tag(TAG).w("원본 파일을 열 수 없음: ${mediaItem.name}")
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "미디어 이동 중 오류: ${mediaItem.name}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "미디어 이동 작업 중 오류 발생")
        }
        
        // MediaStore 새로고침
        if (successCount > 0) {
            refreshMediaStore()
        }
        
        successCount
    }
    
    /**
     * MediaStore에서 미디어 아이템을 삭제합니다.
     */
    private fun deleteMediaItemFromStore(
        mediaItem: MediaItem,
        onRecoverableAction: ((IntentSender) -> Unit)? = null
    ): Boolean {
        return try {
            // 1. URI 기반 삭제 시도
            try {
                val deletedRows = context.contentResolver.delete(mediaItem.uri, null, null)
                if (deletedRows > 0) {
                    return true
                }
            } catch (e: RecoverableSecurityException) {
                Timber.tag(TAG).w("RecoverableSecurityException 발생, 권한 요청 필요: ${mediaItem.name}")
                onRecoverableAction?.invoke(e.userAction.actionIntent.intentSender)
                // 권한 요청을 위해 예외를 다시 던짐
                throw e
            }
            
            // 2. ID 기반 삭제 시도
            val collections = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            
            for (collection in collections) {
                try {
                    val selection = "${MediaStore.MediaColumns._ID} = ?"
                    val selectionArgs = arrayOf(mediaItem.id.toString())
                    
                    val deletedRows = context.contentResolver.delete(collection, selection, selectionArgs)
                    if (deletedRows > 0) {
                        return true
                    }
                } catch (e: RecoverableSecurityException) {
                    Timber.tag(TAG).w("RecoverableSecurityException 발생 (ID 기반): ${mediaItem.name}")
                    onRecoverableAction?.invoke(e.userAction.actionIntent.intentSender)
                    throw e
                }
            }
            
            // 3. 경로 기반 삭제 시도
            val filePath = mediaItem.relativePath + mediaItem.name
            val fullPath = File(Environment.getExternalStorageDirectory(), filePath).absolutePath
            
            for (collection in collections) {
                try {
                    val selection = "${MediaStore.MediaColumns.DATA} = ?"
                    val selectionArgs = arrayOf(fullPath)
                    
                    val deletedRows = context.contentResolver.delete(collection, selection, selectionArgs)
                    if (deletedRows > 0) {
                        return true
                    }
                } catch (e: RecoverableSecurityException) {
                    Timber.tag(TAG).w("RecoverableSecurityException 발생 (경로 기반): ${mediaItem.name}")
                    onRecoverableAction?.invoke(e.userAction.actionIntent.intentSender)
                    throw e
                }
            }
            
            false
        } catch (e: RecoverableSecurityException) {
            onRecoverableAction?.invoke(e.userAction.actionIntent.intentSender)
            // 권한 요청을 위해 예외를 다시 던짐
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "MediaStore에서 미디어 삭제 실패: ${mediaItem.name}")
            false
        }
    }
    
    /**
     * MediaStore를 새로고침합니다.
     */
    private fun refreshMediaStore() {
        try {
            // 시스템에 미디어 스캔 알림 전송
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.parse("file://${Environment.getExternalStorageDirectory()}")
            context.sendBroadcast(intent)
            
            // 추가적인 MediaStore 새로고침
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null
            )?.close()
            
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null
            )?.close()
            
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "MediaStore 새로고침 실패")
        }
    }
    
    /**
     * 미디어 파일을 복사합니다.
     */
    suspend fun copyMediaItems(mediaItems: List<MediaItem>, targetFolderPath: String): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        
        try {
            for (mediaItem in mediaItems) {
                try {
                    // URI에서 실제 파일 경로 가져오기
                    val sourceFile = getFileFromUri(mediaItem.uri)
                    if (sourceFile != null && sourceFile.exists()) {
                        // 원본 파일의 메타데이터 저장
                        val originalLastModified = sourceFile.lastModified()
                        
                        // 대상 폴더 생성 (외부 저장소의 Pictures 폴더 내)
                        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        // targetFolderPath가 이미 전체 경로인지 확인
                        val targetDir = if (targetFolderPath.contains("/")) {
                            // 전체 경로인 경우 (예: "Pictures/Instagram")
                            File(Environment.getExternalStorageDirectory(), targetFolderPath)
                        } else {
                            // 폴더명만 있는 경우 (예: "Instagram")
                            File(picturesDir, targetFolderPath)
                        }
                        if (!targetDir.exists()) {
                            targetDir.mkdirs()
                        }
                        
                        val targetFile = File(targetDir, mediaItem.name)
                        
                        // 파일 복사
                        val copiedFile = sourceFile.copyTo(targetFile, overwrite = true)
                        if (copiedFile.exists()) {
                            // 복사된 파일의 메타데이터를 원본과 동일하게 설정
                            copiedFile.setLastModified(originalLastModified)
                            successCount++
                            // 미디어 복사 성공
                        } else {
                            Timber.tag(TAG).w("미디어 복사 실패: ${mediaItem.name}")
                        }
                    } else {
                        Timber.tag(TAG).w("소스 파일을 찾을 수 없음: ${mediaItem.name}")
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "미디어 복사 중 오류: ${mediaItem.name}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "미디어 복사 작업 중 오류 발생")
        }
        
        // 미디어 복사 완료
        successCount
    }
    
    /**
     * 미디어 파일을 삭제합니다.
     */
    suspend fun deleteMediaItems(
        mediaItems: List<MediaItem>,
        onRecoverableAction: ((IntentSender) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        var permissionErrorCount = 0
        
        try {
            for (mediaItem in mediaItems) {
                try {
                    // MediaStore에서 미디어 아이템 삭제
                    try {
                        val deleteSuccess = deleteMediaItemFromStore(mediaItem, onRecoverableAction)
                        
                        if (deleteSuccess) {
                            successCount++
                            Timber.tag(TAG).d("미디어 삭제 성공: ${mediaItem.name}")
                        } else {
                            Timber.tag(TAG).w("미디어 삭제 실패: ${mediaItem.name}")
                        }
                    } catch (e: RecoverableSecurityException) {
                        permissionErrorCount++
                        Timber.tag(TAG).w("삭제 권한 부족: ${mediaItem.name}")
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "미디어 삭제 중 오류: ${mediaItem.name}")
                }
            }
            
            // 권한 오류가 있는 경우 로그 출력
            if (permissionErrorCount > 0) {
                Timber.tag(TAG).w("삭제 권한이 부족한 파일: ${permissionErrorCount}개")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "미디어 삭제 작업 중 오류 발생")
        }
        
        // MediaStore 새로고침
        if (successCount > 0) {
            refreshMediaStore()
        }
        
        successCount
    }
} 