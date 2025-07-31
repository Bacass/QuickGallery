package com.lee.quickgallery.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.quickgallery.ui.components.MediaThumbnail
import com.lee.quickgallery.ui.viewmodel.GalleryViewModel
import com.lee.quickgallery.util.AppPrefs
import com.lee.quickgallery.util.SortType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 액션 타입 enum
enum class ActionType {
    MOVE, COPY, SHARE, DELETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubListScreen(
    folderPath: String,
    onBackClick: () -> Unit = {},
    onMediaClick: (String) -> Unit = {},
    viewModel: GalleryViewModel = viewModel()
) {
    val mediaList by viewModel.mediaList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val totalMediaCount by viewModel.totalMediaCount.collectAsState()
    val context = LocalContext.current
    
    // 현재 정렬 방식을 State로 관리
    var currentSortType by remember { mutableStateOf(SortType.fromString(AppPrefs.mediaSortType)) }
    
    // 선택된 이미지 관리
    var selectedMediaItems by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    
    // 액션 메뉴 상태
    var showActionMenu by remember { mutableStateOf(false) }
    var showFolderSelectionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf<ActionType?>(null) }
    
    // 폴더 목록 (폴더 선택용)
    val folderList by viewModel.folderList.collectAsState()
    
    // 폴더 경로에서 폴더명 추출
    val folderName = folderPath.substringAfterLast("/", folderPath)
    
    // Back key 처리
    BackHandler {
        if (isSelectionMode) {
            // 선택 모드일 때는 선택 모드 종료
            isSelectionMode = false
            selectedMediaItems = emptySet()
        } else {
            onBackClick()
        }
    }
    
    // 화면 진입 시 해당 폴더의 미디어 로드
    LaunchedEffect(folderPath) {
        viewModel.loadMediaByFolder(folderPath)
    }
    
    // 정렬 방식 변경 감지 및 새로고침
    LaunchedEffect(Unit) {
        while (true) {
            val newSortType = SortType.fromString(AppPrefs.mediaSortType)
            if (currentSortType != newSortType) {
                currentSortType = newSortType
                viewModel.loadMediaByFolder(folderPath, forceRefresh = true)
            }
            kotlinx.coroutines.delay(100) // 100ms마다 체크
        }
    }
    
    // 미디어 클릭 처리 함수
    val handleMediaClick = { mediaItem: com.lee.quickgallery.util.MediaItem ->
        if (isSelectionMode) {
            // 선택 모드일 때는 선택 상태 토글
            val newSelectedItems = if (selectedMediaItems.contains(mediaItem.id)) {
                selectedMediaItems - mediaItem.id
            } else {
                selectedMediaItems + mediaItem.id
            }
            selectedMediaItems = newSelectedItems
            
            if (newSelectedItems.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            // 일반 모드일 때는 기존 동작
            if (mediaItem.mimeType.startsWith("video/")) {
                // 비디오 파일인 경우 외부 영상 플레이어 앱으로 연결
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(mediaItem.uri, mediaItem.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // 영상 플레이어 앱이 없는 경우 기본 앱 선택 다이얼로그 표시
                    val chooserIntent = Intent.createChooser(intent, "영상 재생")
                    context.startActivity(chooserIntent)
                }
            } else {
                // 이미지 파일인 경우 ViewerScreen으로 이동
                onMediaClick(mediaItem.uri.toString())
            }
        }
    }
    
    // 롱클릭 처리 함수
    val handleMediaLongClick = { mediaItem: com.lee.quickgallery.util.MediaItem ->
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedMediaItems = setOf(mediaItem.id)
        }
    }
    
    // 액션 처리 함수들
    val handleAction = { selectedAction: ActionType ->
        actionType = selectedAction
        showActionMenu = false
        
        when (selectedAction) {
            ActionType.MOVE, ActionType.COPY -> {
                showFolderSelectionDialog = true
            }
            ActionType.SHARE -> {
                // 공유 기능 구현
                val selectedItems = mediaList.filter { selectedMediaItems.contains(it.id) }
                if (selectedItems.isNotEmpty()) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        type = "image/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedItems.map { it.uri }))
                    }
                    val chooserIntent = Intent.createChooser(shareIntent, "공유")
                    context.startActivity(chooserIntent)
                }
            }
            ActionType.DELETE -> {
                showDeleteConfirmDialog = true
            }
        }
    }
    
    // 폴더 선택 처리
    val handleFolderSelection = { targetFolderPath: String ->
        val selectedItems = mediaList.filter { selectedMediaItems.contains(it.id) }
        when (actionType) {
            ActionType.MOVE -> {
                // 이동 로직 구현
                viewModel.moveMediaItems(selectedItems, targetFolderPath)
            }
            ActionType.COPY -> {
                // 복사 로직 구현
                viewModel.copyMediaItems(selectedItems, targetFolderPath)
            }
            else -> {}
        }
        
        // 선택 모드 종료
        isSelectionMode = false
        selectedMediaItems = emptySet()
        showFolderSelectionDialog = false
        actionType = null
    }
    
    // 삭제 확인 처리
    val handleDeleteConfirm = {
        val selectedItems = mediaList.filter { selectedMediaItems.contains(it.id) }
        viewModel.deleteMediaItems(selectedItems)
        
        // 선택 모드 종료
        isSelectionMode = false
        selectedMediaItems = emptySet()
        showDeleteConfirmDialog = false
    }
    
    val pendingIntentSender by viewModel.pendingIntentSender.collectAsState()
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // 권한 허용 후 재시도 로직 필요시 여기에 추가
        viewModel.clearPendingIntentSender()
    }
    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let { intentSender ->
            intentSenderLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = "${selectedMediaItems.size}개 선택됨",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedMediaItems = emptySet()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // 선택 모드일 때는 액션 메뉴 버튼
                        IconButton(
                            onClick = { showActionMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "액션 메뉴"
                            )
                        }
                        
                        // DropdownMenu
                        DropdownMenu(
                            expanded = showActionMenu,
                            onDismissRequest = { showActionMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("이동") },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                onClick = { handleAction(ActionType.MOVE) }
                            )
                            DropdownMenuItem(
                                text = { Text("복사") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = { handleAction(ActionType.COPY) }
                            )
                            DropdownMenuItem(
                                text = { Text("공유") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = { handleAction(ActionType.SHARE) }
                            )
                            DropdownMenuItem(
                                text = { Text("삭제") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = { handleAction(ActionType.DELETE) }
                            )
                        }
                    } else {
                        // 일반 모드일 때는 새로고침 버튼
                        IconButton(
                            onClick = { viewModel.loadMediaByFolder(folderPath) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "새로고침"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                !hasPermission -> {
                    // 권한 없음 상태
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "미디어 접근 권한이 필요합니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                isLoading -> {
                    // 로딩 상태
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                errorMessage != null -> {
                    // 오류 상태
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                mediaList.isEmpty() -> {
                    // 빈 상태
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "이 폴더에 미디어가 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    // 미디어 그리드 (3열) with 스크롤바
                    val gridState = rememberLazyGridState()
                    val coroutineScope = rememberCoroutineScope()
                    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
                    val density = LocalDensity.current
                    
                    // 미디어 리스트가 있을 때만 스크롤바 관련 처리
                    val shouldShowScrollbar = remember(mediaList.size) {
                        mediaList.size > 6 // 6개 이상일 때 스크롤바 표시
                    }
                    
                    // 실제로 스크롤이 가능한지 확인
                    val canScroll = remember(mediaList.size) {
                        mediaList.size > 9 // 3열 그리드에서 3행 이상일 때 스크롤 가능
                    }
                    
                    // 스크롤바 표시 상태
                    var showScrollbar by remember { mutableStateOf(false) }
                    var isScrollbarDragging by remember { mutableStateOf(false) }
                    var showDatePopup by remember { mutableStateOf(false) }
                    var containerHeightPx by remember { mutableStateOf(0f) }
                    
                    // 터치 드래그 전용 상태 추가
                    var isTouchDragging by remember { mutableStateOf(false) }
                    var dragStartY by remember { mutableStateOf(0f) }
                    var dragCurrentY by remember { mutableStateOf(0f) }
                    
                    // 스크롤 진행도 계산 (example.kt 방식)
                    val scrollProgress by remember {
                        derivedStateOf {
                            val layoutInfo = gridState.layoutInfo
                            val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                            if (visibleItem != null && mediaList.isNotEmpty()) {
                                val totalItems = mediaList.size
                                val lastIndex = totalItems - layoutInfo.visibleItemsInfo.size
                                if (lastIndex > 0) {
                                    (visibleItem.index.toFloat() / lastIndex).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                            } else {
                                0f
                            }
                        }
                    }
                    
                    // 현재 날짜 계산
                    val currentDate by remember {
                        derivedStateOf {
                            if (mediaList.isNotEmpty()) {
                                val targetIndex = (scrollProgress * (mediaList.size - 1)).toInt()
                                    .coerceIn(0, mediaList.size - 1)
                                val mediaItem = mediaList[targetIndex]
                                val date = Date(mediaItem.dateAdded * 1000)
                                dateFormat.format(date)
                            } else {
                                ""
                            }
                        }
                    }
                    
                    // 스크롤 상태 감지
                    LaunchedEffect(gridState) {
                        snapshotFlow { gridState.isScrollInProgress }
                            .collect { isScrolling ->
                                if (canScroll) { // 스크롤 가능한 경우에만 처리
                                    if (isScrolling && !isTouchDragging) {
                                        // 스크롤 시작 시 thumb 표시
                                        showScrollbar = true
                                    } else if (!isScrolling && !isTouchDragging) {
                                        // 스크롤이 멈추고 터치 드래그가 아닐 때 1.5초 후 숨김
                                        kotlinx.coroutines.delay(1500)
                                        if (!isScrollbarDragging && !isTouchDragging) {
                                            showScrollbar = false
                                            showDatePopup = false
                                        }
                                    }
                                }
                            }
                    }
                    
                    // Thumb 높이 계산
                    val baseThumbHeight = 80.dp
                    val minThumbHeight = 40.dp
                    val maxThumbHeight = 120.dp
                    val thumbHeightPx = with(density) {
                        if (mediaList.size > 0) {
                            val ratio = if (totalMediaCount > mediaList.size) {
                                mediaList.size.toFloat() / totalMediaCount
                            } else {
                                0.6f // 기본 비율
                            }
                            (baseThumbHeight.value * ratio.coerceIn(0.3f, 1f))
                                .coerceIn(minThumbHeight.value, maxThumbHeight.value).dp.toPx()
                        } else {
                            baseThumbHeight.toPx()
                        }
                    }
                    
                    // Thumb 위치 계산
                    val absoluteSafeRangePx = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                    val thumbOffsetPx = (scrollProgress * absoluteSafeRangePx).coerceIn(0f, absoluteSafeRangePx)
                    val thumbOffset = with(density) { thumbOffsetPx.toDp() }
                    
                    // 초기 로드 시에는 스크롤바를 표시하지 않음 (스크롤 시작 시에만 표시)
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = mediaList,
                                key = { it.id }
                            ) { mediaItem ->
                                MediaThumbnail(
                                    mediaItem = mediaItem,
                                    onClick = { handleMediaClick(mediaItem) },
                                    isSelected = selectedMediaItems.contains(mediaItem.id),
                                    onLongClick = { handleMediaLongClick(mediaItem) }
                                )
                            }
                        }
                        
                        // 스크롤바 Thumb (스크롤 가능하고 표시 상태일 때만 표시)
                        if (mediaList.isNotEmpty() && canScroll && showScrollbar) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 10.dp, top = 10.dp, bottom = 10.dp)
                                    .width(24.dp)
                                    .fillMaxHeight()
//                                    .background(Color.Red.copy(alpha = 0.2f)) // 디버깅용 배경
                                    .onGloballyPositioned { coordinates ->
                                        containerHeightPx = coordinates.size.height.toFloat()
                                    }
                                    .pointerInput(Unit) {
                                        // 스크롤바 영역의 모든 터치 이벤트를 가로채서 리스트로 전파되지 않도록 함
                                        detectTapGestures { /* 터치 이벤트 소비 */ }
                                    }
                            ) {
                                // 스크롤바 트랙 (배경) - 제거됨
                                // 미디어 수 정보 표시 (터치 드래그일 때만)
                                if (isTouchDragging && totalMediaCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .offset(x = (-50).dp)
                                            .wrapContentSize()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.8f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${mediaList.size}/${totalMediaCount}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                // Thumb (터치 영역과 표시 영역 통합)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(y = thumbOffset)
                                        .width(28.dp) // 전체 터치 영역
                                        .height(with(density) { thumbHeightPx.coerceAtLeast(50.dp.toPx()).toDp() })
                                        // .background(Color.Green.copy(alpha = 0.3f)) // 디버깅용 - 터치 영역
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { offset ->
                                                    // 탭한 위치로 즉시 스크롤
                                                    if (mediaList.isNotEmpty() && containerHeightPx > 0f) {
                                                        val progress = (offset.y / containerHeightPx).coerceIn(0f, 1f)
                                                        val targetIndex = (progress * (mediaList.size - 1).coerceAtLeast(0)).toInt()
                                                            .coerceIn(0, mediaList.size - 1)
                                                        
                                                        // Tap 스크롤 처리
                                                        coroutineScope.launch {
                                                            gridState.animateScrollToItem(targetIndex)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(Unit, mediaList.size) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    // 현재 스크롤 위치를 기반으로 실제 thumb 위치 계산
                                                    val layoutInfo = gridState.layoutInfo
                                                    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                                                    val currentScrollProgress = if (visibleItem != null && mediaList.isNotEmpty()) {
                                                        val totalItems = mediaList.size
                                                        val lastIndex = totalItems - layoutInfo.visibleItemsInfo.size
                                                        if (lastIndex > 0) {
                                                            (visibleItem.index.toFloat() / lastIndex).coerceIn(0f, 1f)
                                                        } else {
                                                            0f
                                                        }
                                                    } else {
                                                        0f
                                                    }
                                                    
                                                    val safeRange = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                                                    val actualThumbPosition = currentScrollProgress * safeRange
                                                    
                                                    // 드래그 시작
                                                    isScrollbarDragging = true
                                                    isTouchDragging = true
                                                    showScrollbar = true
                                                    showDatePopup = true
                                                    // 실제 thumb 위치를 시작점으로 설정
                                                    dragStartY = actualThumbPosition
                                                    dragCurrentY = actualThumbPosition
                                                },
                                                onDragEnd = {
                                                    // 드래그 종료
                                                    isScrollbarDragging = false
                                                    isTouchDragging = false
                                                    // 드래그 상태 리셋
                                                    dragStartY = 0f
                                                    dragCurrentY = 0f
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(1500)
                                                        showScrollbar = false
                                                        showDatePopup = false
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    // 드래그 델타를 누적하여 새로운 thumb 위치 계산
                                                    dragCurrentY += dragAmount.y
                                                    
                                                    if (mediaList.isNotEmpty() && containerHeightPx > 0f) {
                                                        val safeRange = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                                                        // dragCurrentY가 이미 실제 thumb 위치
                                                        val clampedPosition = dragCurrentY.coerceIn(0f, safeRange)
                                                        val progress = if (safeRange > 0f) (clampedPosition / safeRange) else 0f
                                                        val targetIndex = (progress * (mediaList.size - 1).coerceAtLeast(0)).toInt()
                                                            .coerceIn(0, mediaList.size - 1)
                                                        
                                                        // 드래그 진행
                                                        
                                                        coroutineScope.launch {
                                                            gridState.scrollToItem(targetIndex)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    // 실제 보이는 Thumb (우측 정렬)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(12.dp)
                                            .fillMaxHeight()
                                            .background(
                                                Color.White.copy(alpha = if (isTouchDragging) 1.0f else 0.9f), 
                                                RoundedCornerShape(6.dp)
                                            )
                                    )
                                }
                            }
                        }

                        // 날짜 팝업 표시 (터치 드래그일 때만)
                        if (isTouchDragging && currentDate.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(
                                        x = (-50).dp,
                                        y = thumbOffset + with(density) { (thumbHeightPx / 2).toDp() } - 10.dp // 중앙 정렬 + 미세 조정
                                    )
                                    .wrapContentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.85f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = currentDate,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 폴더 선택 다이얼로그
        if (showFolderSelectionDialog) {
            FolderSelectionDialog(
                folderList = folderList,
                currentFolderPath = folderPath,
                onFolderSelected = handleFolderSelection,
                onDismiss = { 
                    showFolderSelectionDialog = false
                    actionType = null
                }
            )
        }
        
        // 삭제 확인 다이얼로그
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("삭제 확인") },
                text = { Text("${selectedMediaItems.size}개의 이미지를 삭제하시겠습니까?") },
                confirmButton = {
                    Button(
                        onClick = handleDeleteConfirm
                    ) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false }
                    ) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

// 폴더 선택 다이얼로그
@Composable
fun FolderSelectionDialog(
    folderList: List<com.lee.quickgallery.model.FolderItem>,
    currentFolderPath: String,
    onFolderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "폴더 선택",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                val availableFolders = folderList.filter { it.folderPath != currentFolderPath }
                
                if (availableFolders.isEmpty()) {
                    Text(
                        text = "이동할 수 있는 다른 폴더가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    availableFolders.forEach { folder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onFolderSelected(folder.folderPath) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = folder.folderName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = folder.folderPath,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${folder.mediaCount}개",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
} 