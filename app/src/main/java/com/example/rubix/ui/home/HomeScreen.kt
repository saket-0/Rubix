package com.example.rubix.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.ui.components.CreateFolderDialog
import com.example.rubix.ui.components.FabMenu
import com.example.rubix.ui.components.GradientBackground
import com.example.rubix.ui.components.NodeItemView
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNodeClick: (NodeEntity) -> Unit,
    onCreateNote: () -> Unit,
    onSearchClick: () -> Unit,
    onTakePhoto: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val nodes by viewModel.nodes.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    
    // Drag state
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Photo Picker
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.importImage(uri)
        }
    }
    
    // PDF Picker
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
             viewModel.importPdf(uri)
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Persistent Search Bar (Google Keep style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SearchBar(
                        query = "",
                        onQueryChange = { },
                        onSearch = { },
                        active = false,
                        onActiveChange = { if (it) onSearchClick() },
                        placeholder = { Text("Search notes...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSearchClick() }
                    ) {
                        // Empty content - we navigate to SearchScreen instead
                    }
                }
            },
            floatingActionButton = {
                FabMenu(
                    onCreateFolder = { showCreateFolderDialog = true },
                    onCreateNote = onCreateNote,
                    onImportImage = {
                         pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onImportPdf = {
                         pickPdf.launch("application/pdf")
                    },
                    onTakePhoto = onTakePhoto
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(nodes, key = { _, node -> node.id }) { index, node ->
                        val isDragging = draggedIndex == index
                        
                        // Elevation animation
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "drag_elevation"
                        )
                        
                        Box(
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .offset {
                                    if (isDragging) {
                                        IntOffset(
                                            x = dragOffsetX.roundToInt(),
                                            y = dragOffsetY.roundToInt()
                                        )
                                    } else {
                                        IntOffset.Zero
                                    }
                                }
                                .graphicsLayer {
                                    if (isDragging) {
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                    }
                                }
                                .shadow(elevation)
                                .pointerInput(node.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetX += dragAmount.x
                                            dragOffsetY += dragAmount.y
                                        },
                                        onDragEnd = {
                                            // Calculate target index based on drag position
                                            // Simplified: use Y offset to determine target row
                                            val itemHeight = 200f // Approximate item height
                                            val rowDelta = (dragOffsetY / itemHeight).roundToInt()
                                            val colDelta = if (dragOffsetX > 80) 1 else if (dragOffsetX < -80) -1 else 0
                                            
                                            val targetIndex = (index + rowDelta * 2 + colDelta)
                                                .coerceIn(0, nodes.size - 1)
                                            
                                            if (targetIndex != index) {
                                                viewModel.reorderNodes(index, targetIndex)
                                            }
                                            
                                            draggedIndex = -1
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = -1
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                        ) {
                            NodeItemView(
                                node = node,
                                onClick = { 
                                    if (draggedIndex == -1) {
                                        onNodeClick(node)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}



