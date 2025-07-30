package com.lee.quickgallery.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lee.quickgallery.model.FolderItem
import com.lee.quickgallery.util.MediaItem
import com.lee.quickgallery.util.MediaStoreUtil
import com.lee.quickgallery.util.PermissionUtil
import com.lee.quickgallery.util.AppPrefs
import com.lee.quickgallery.util.SortType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    
    private val _totalMediaCount = MutableStateFlow(0)
    val totalMediaCount: StateFlow<Int> = _totalMediaCount.asStateFlow()
    
    private val _currentSortType = MutableStateFlow(SortType.fromString(AppPrefs.mediaSortType))
    val currentSortType: StateFlow<SortType> = _currentSortType.asStateFlow()
    
    // 폴더 순서 관리를 위한 상태
    private val _folderOrder = MutableStateFlow<List<String>>(emptyList())
    val folderOrder: StateFlow<List<String>> = _folderOrder.asStateFlow()
    
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
                
                // 현재 설정된 정렬 방식 가져오기
                val sortType = SortType.fromString(AppPrefs.mediaSortType)
                
                Timber.tag(TAG).d("폴더 로딩 시작, 정렬: ${sortType.displayName}")
                
                // 이미지 수에 따라 캐시 크기 설정
                val totalImageCount = mediaStoreUtil.getTotalImageCount()
                mediaStoreUtil.thumbnailCacheManager.setCacheSizeBasedOnImageCount(totalImageCount)
                
                val folderMap = mediaStoreUtil.getMediaGroupedByFolder(sortType)
                
                val folderItems = folderMap.mapNotNull { (folderPath, mediaList) ->
                    FolderItem.create(folderPath, mediaList)
                }.sortedByDescending { it.latestMedia.dateAdded }
                
                _folderList.value = folderItems
                
                // 폴더 순서 초기화 (저장된 순서 복원 또는 기본 순서)
                initializeFolderOrder()
                
                Timber.tag(TAG).d("폴더 로딩 완료: ${folderItems.size}개, 정렬: ${sortType.displayName}")
                
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
    
    fun loadMediaByFolder(folderPath: String, forceRefresh: Boolean = false) {
        if (!_hasPermission.value) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // 현재 설정된 정렬 방식 가져오기
                val sortType = SortType.fromString(AppPrefs.mediaSortType)
                
                Timber.tag(TAG).d("폴더별 미디어 로딩 시작: $folderPath, 정렬: ${sortType.displayName}, 강제새로고침: $forceRefresh")
                
                // 폴더별 전체 미디어 수와 실제 미디어를 병렬로 조회
                val (media, totalCount) = coroutineScope {
                    val mediaDeferred = async { mediaStoreUtil.getMediaByFolder(folderPath, sortType) }
                    val totalCountDeferred = async { mediaStoreUtil.getMediaCountByFolder(folderPath) }
                    
                    mediaDeferred.await() to totalCountDeferred.await()
                }
                
                _mediaList.value = media
                _totalMediaCount.value = totalCount
                
                Timber.tag(TAG).d("폴더별 미디어 로딩 완료: ${media.size}개 (전체: ${totalCount}개), 정렬: ${sortType.displayName}")
                
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
    
    // 정렬 방식이 변경되었을 때 강제로 폴더 새로고침
    fun refreshFoldersWithNewSort() {
        viewModelScope.launch {
            _isLoading.value = true
            loadFolders()
        }
    }
    
    // 정렬 방식 업데이트 (SettingScreen에서 호출)
    fun updateSortType(newSortType: SortType) {
        if (_currentSortType.value != newSortType) {
            _currentSortType.value = newSortType
            AppPrefs.mediaSortType = newSortType.name
            // 폴더 목록 자동 새로고침
            refreshFoldersWithNewSort()
        }
    }
    
    // 정렬 방식 동기화 (MainScreen에서 호출)
    fun syncSortType() {
        val currentPrefSortType = SortType.fromString(AppPrefs.mediaSortType)
        if (_currentSortType.value != currentPrefSortType) {
            _currentSortType.value = currentPrefSortType
            refreshFoldersWithNewSort()
        }
    }
    
    // 폴더 순서 변경
    fun reorderFolders(fromIndex: Int, toIndex: Int) {
        val currentOrder = _folderOrder.value.toMutableList()
        if (fromIndex in currentOrder.indices && toIndex in currentOrder.indices) {
            val item = currentOrder.removeAt(fromIndex)
            currentOrder.add(toIndex, item)
            _folderOrder.value = currentOrder
            saveFolderOrder(currentOrder)
            Timber.tag(TAG).d("폴더 순서 변경: $fromIndex -> $toIndex")
        }
    }
    
    // 폴더 순서 초기화
    fun initializeFolderOrder() {
        val currentFolders = _folderList.value
        if (currentFolders.isNotEmpty()) {
            // 저장된 순서가 있으면 복원, 없으면 기본 순서 사용
            val savedOrder = loadFolderOrder()
            val currentFolderPaths = currentFolders.map { it.folderPath }
            
            if (savedOrder.isNotEmpty()) {
                // 저장된 순서에서 현재 존재하는 폴더만 필터링
                val validOrder = savedOrder.filter { it in currentFolderPaths }
                // 새로 추가된 폴더들을 뒤에 추가
                val newFolders = currentFolderPaths.filter { it !in validOrder }
                val finalOrder = validOrder + newFolders
                _folderOrder.value = finalOrder
                Timber.tag(TAG).d("저장된 폴더 순서 복원: ${finalOrder.size}개")
            } else {
                // 저장된 순서가 없으면 기본 순서 사용
                _folderOrder.value = currentFolderPaths
                Timber.tag(TAG).d("기본 폴더 순서 초기화: ${currentFolderPaths.size}개")
            }
        }
    }
    
    // 폴더 순서 저장
    private fun saveFolderOrder(folderOrder: List<String>) {
        val orderString = folderOrder.joinToString(",")
        AppPrefs.folderOrder = orderString
        Timber.tag(TAG).d("폴더 순서 저장: ${folderOrder.size}개")
    }
    
    // 폴더 순서 복원
    private fun loadFolderOrder(): List<String> {
        val orderString = AppPrefs.folderOrder
        return if (orderString.isNotEmpty()) {
            orderString.split(",").filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }
    
    // 현재 폴더 순서에 따라 정렬된 폴더 목록 반환
    fun getOrderedFolderList(): List<FolderItem> {
        val order = _folderOrder.value
        val folders = _folderList.value.associateBy { it.folderPath }
        
        return order.mapNotNull { folderPath ->
            folders[folderPath]
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
} 