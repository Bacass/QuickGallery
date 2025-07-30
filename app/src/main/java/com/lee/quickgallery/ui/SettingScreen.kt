package com.lee.quickgallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.quickgallery.util.AppPrefs
import com.lee.quickgallery.util.SortType
import com.lee.quickgallery.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBackClick: () -> Unit = {},
    viewModel: GalleryViewModel = viewModel()
) {
    var showMediaCount by remember { mutableStateOf(AppPrefs.showMediaCount) }
    var selectedSortType by remember { mutableStateOf(SortType.fromString(AppPrefs.mediaSortType)) }
    var autoRefresh by remember { mutableStateOf(false) }
    
    // Back key 처리
    BackHandler {
        onBackClick()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "설정",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 앱 정보
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.padding(8.dp))
                        
                        Text(
                            text = "앱 정보",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Quick Gallery",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    Text(
                        text = "버전 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 표시 설정
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "표시 설정",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 미디어 개수 표시
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "미디어 개수 표시",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "폴더에 포함된 전체 미디어\n개수(이미지+비디오)를 표시합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = showMediaCount,
                            onCheckedChange = { 
                                showMediaCount = it
                                AppPrefs.showMediaCount = it
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 정렬 방식 선택
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "정렬 방식",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = "폴더 내 미디어 파일의 정렬 순서를 선택합니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // 정렬 옵션들 - 2x2 그리드로 배치
                        val sortTypes = SortType.values()
                        
                        // 시간순 옵션들 (첫 번째 Row)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 시간순(내림차순)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedSortType = SortType.TIME_DESC
                                        viewModel.updateSortType(SortType.TIME_DESC)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSortType == SortType.TIME_DESC,
                                    onClick = {
                                        selectedSortType = SortType.TIME_DESC
                                        viewModel.updateSortType(SortType.TIME_DESC)
                                    }
                                )
                                
                                Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                                
                                Text(
                                    text = SortType.TIME_DESC.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            // 시간순(오름차순)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedSortType = SortType.TIME_ASC
                                        viewModel.updateSortType(SortType.TIME_ASC)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSortType == SortType.TIME_ASC,
                                    onClick = {
                                        selectedSortType = SortType.TIME_ASC
                                        viewModel.updateSortType(SortType.TIME_ASC)
                                    }
                                )
                                
                                Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                                
                                Text(
                                    text = SortType.TIME_ASC.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        // 이름순 옵션들 (두 번째 Row)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 이름순(내림차순)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedSortType = SortType.NAME_DESC
                                        viewModel.updateSortType(SortType.NAME_DESC)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSortType == SortType.NAME_DESC,
                                    onClick = {
                                        selectedSortType = SortType.NAME_DESC
                                        viewModel.updateSortType(SortType.NAME_DESC)
                                    }
                                )
                                
                                Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                                
                                Text(
                                    text = SortType.NAME_DESC.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            // 이름순(오름차순)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedSortType = SortType.NAME_ASC
                                        viewModel.updateSortType(SortType.NAME_ASC)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSortType == SortType.NAME_ASC,
                                    onClick = {
                                        selectedSortType = SortType.NAME_ASC
                                        viewModel.updateSortType(SortType.NAME_ASC)
                                    }
                                )
                                
                                Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                                
                                Text(
                                    text = SortType.NAME_ASC.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            
            // 동작 설정
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "동작 설정",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 자동 새로고침
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "자동 새로고침",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "앱 실행 시 자동으로 미디어를 새로고침합니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = autoRefresh,
                            onCheckedChange = { autoRefresh = it }
                        )
                    }
                }
            }
            
            // 권한 정보
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "권한 정보",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = "이 앱은 기기의 이미지와 비디오에 접근하기 위해 다음 권한이 필요합니다:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "• READ_MEDIA_IMAGES: 이미지 파일 읽기",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "• READ_MEDIA_VIDEO: 비디오 파일 읽기",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 