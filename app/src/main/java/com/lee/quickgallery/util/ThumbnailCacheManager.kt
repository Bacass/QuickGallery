package com.lee.quickgallery.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ThumbnailCacheManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ThumbnailCache"
        private const val CACHE_DIR_NAME = "thumbnails"
        private const val BASE_CACHE_SIZE = 30 * 1024 * 1024 // 30MB 기본
        private const val CACHE_SIZE_PER_1000_IMAGES = 30 * 1024 * 1024 // 1000장당 30MB 추가
    }
    
    private var maxCacheSize = BASE_CACHE_SIZE
    
    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }
    
    /**
     * 사용자의 이미지 수에 따라 캐시 크기를 설정합니다.
     */
    fun setCacheSizeBasedOnImageCount(totalImageCount: Int) {
        val additionalSize = (totalImageCount / 1000) * CACHE_SIZE_PER_1000_IMAGES
        maxCacheSize = BASE_CACHE_SIZE + additionalSize
        
        Timber.tag(TAG).d("캐시 크기 설정: ${totalImageCount}장 이미지 -> ${maxCacheSize / (1024 * 1024)}MB")
    }
    
    /**
     * 썸네일을 가져오거나 생성합니다.
     */
    suspend fun getThumbnail(mediaUri: Uri, mediaId: Long): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(mediaId)
            
            // 캐시에서 먼저 확인
            if (cacheFile.exists()) {
                Timber.tag(TAG).d("캐시에서 썸네일 로드: $mediaId")
                return@withContext Uri.fromFile(cacheFile)
            }
            
            // 캐시가 없으면 생성
            val thumbnailUri = generateThumbnail(mediaUri, mediaId)
            if (thumbnailUri != null) {
                // 캐시 크기 관리
                manageCacheSize()
            }
            
            thumbnailUri
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "썸네일 가져오기 실패: $mediaId")
            null
        }
    }
    
    /**
     * 비디오에서 썸네일을 생성합니다.
     */
    private suspend fun generateThumbnail(mediaUri: Uri, mediaId: Long): Uri? = withContext(Dispatchers.IO) {
        return@withContext try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, mediaUri)
            
            // 비디오의 첫 번째 프레임을 썸네일로 사용
            val bitmap = retriever.frameAtTime
            retriever.release()
            
            if (bitmap != null) {
                val cacheFile = getCacheFile(mediaId)
                saveBitmapToFile(bitmap, cacheFile)
                Uri.fromFile(cacheFile)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "비디오 썸네일 생성 실패: $mediaId")
            null
        }
    }
    
    /**
     * 캐시 파일을 가져옵니다.
     */
    private fun getCacheFile(mediaId: Long): File {
        return File(cacheDir, "thumb_$mediaId.jpg")
    }
    
    /**
     * Bitmap을 파일로 저장합니다.
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            Timber.tag(TAG).d("썸네일 캐시 저장: ${file.name}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "썸네일 저장 실패: ${file.name}")
        }
    }
    
    /**
     * 캐시 크기를 관리합니다.
     */
    private fun manageCacheSize() {
        try {
            val files = cacheDir.listFiles() ?: return
            val totalSize = files.sumOf { it.length() }
            
            if (totalSize > maxCacheSize) {
                // 가장 오래된 파일부터 삭제
                val sortedFiles = files.sortedBy { it.lastModified() }
                var currentSize = totalSize
                
                for (file in sortedFiles) {
                    if (currentSize <= maxCacheSize * 0.8) break // 80%까지 줄임
                    currentSize -= file.length()
                    file.delete()
                    Timber.tag(TAG).d("캐시 파일 삭제: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "캐시 크기 관리 실패")
        }
    }
    
    /**
     * 캐시를 정리합니다.
     */
    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            Timber.tag(TAG).d("캐시 정리 완료")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "캐시 정리 실패")
        }
    }
    
    /**
     * 캐시 크기를 가져옵니다.
     */
    fun getCacheSize(): Long {
        return try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0
        } catch (e: Exception) {
            0
        }
    }
} 