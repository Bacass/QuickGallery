package com.lee.quickgallery.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lee.quickgallery.model.FolderItem
import com.lee.quickgallery.util.MediaItem
import com.lee.quickgallery.util.MediaStoreUtil
import com.lee.quickgallery.util.PermissionUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "GalleryViewModel"
    }
    
    private val mediaStoreUtil = MediaStoreUtil(application)
    
    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()
    
    private val _folderList = MutableStateFlow<List<FolderItem>>(emptyList())
    val folderList: StateFlow<List<FolderItem>> = _folderList.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()
    
    init {
        checkPermission()
    }
    
    fun checkPermission() {
        val hasRequiredPermissions = PermissionUtil.hasRequiredPermissions(getApplication())
        _hasPermission.value = hasRequiredPermissions
        
        if (hasRequiredPermissions) {
            loadFolders()
        } else {
            Timber.tag(TAG).w("필요한 권한이 없습니다.")
            _errorMessage.value = "미디어 접근 권한이 필요합니다."
        }
    }
    
    fun loadAllMedia() {
        if (!_hasPermission.value) {
            Timber.tag(TAG).w("권한이 없어 미디어를 로드할 수 없습니다.")
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                Timber.tag(TAG).d("미디어 로딩 시작")
                val media = mediaStoreUtil.getAllMedia()
                _mediaList.value = media
                
                Timber.tag(TAG).d("미디어 로딩 완료: ${media.size}개")
                
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "미디어 로딩 중 오류 발생")
                _errorMessage.value = "미디어를 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadFolders() {
        if (!_hasPermission.value) {
            Timber.tag(TAG).w("권한이 없어 폴더를 로드할 수 없습니다.")
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                Timber.tag(TAG).d("폴더 로딩 시작")
                
                // 이미지 수에 따라 캐시 크기 설정
                val totalImageCount = mediaStoreUtil.getTotalImageCount()
                mediaStoreUtil.thumbnailCacheManager.setCacheSizeBasedOnImageCount(totalImageCount)
                
                val folderMap = mediaStoreUtil.getMediaGroupedByFolder()
                
                val folderItems = folderMap.mapNotNull { (folderPath, mediaList) ->
                    FolderItem.create(folderPath, mediaList)
                }.sortedByDescending { it.latestMedia.dateAdded }
                
                _folderList.value = folderItems
                
                Timber.tag(TAG).d("폴더 로딩 완료: ${folderItems.size}개")
                
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "폴더 로딩 중 오류 발생")
                _errorMessage.value = "폴더를 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadImagesOnly() {
        if (!_hasPermission.value) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val images = mediaStoreUtil.getAllImages()
                _mediaList.value = images
                
                Timber.tag(TAG).d("이미지 로딩 완료: ${images.size}개")
                
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "이미지 로딩 중 오류 발생")
                _errorMessage.value = "이미지를 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadVideosOnly() {
        if (!_hasPermission.value) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val videos = mediaStoreUtil.getAllVideos()
                _mediaList.value = videos
                
                Timber.tag(TAG).d("비디오 로딩 완료: ${videos.size}개")
                
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "비디오 로딩 중 오류 발생")
                _errorMessage.value = "비디오를 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadMediaByFolder(folderPath: String) {
        if (!_hasPermission.value) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val media = mediaStoreUtil.getMediaByFolder(folderPath)
                _mediaList.value = media
                
                Timber.tag(TAG).d("폴더별 미디어 로딩 완료: ${media.size}개")
                
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "폴더별 미디어 로딩 중 오류 발생")
                _errorMessage.value = "폴더별 미디어를 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshMedia() {
        loadFolders()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
} 