package com.example.rubix.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.ui.components.BreadcrumbBar
import com.example.rubix.ui.components.CreateFolderDialog
import com.example.rubix.ui.components.FabMenu
import com.example.rubix.ui.components.GradientBackground
import com.example.rubix.ui.components.GlideMenuBox
import com.example.rubix.ui.components.GlideMenuOption
import com.example.rubix.ui.components.NodeItemView
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNodeClick: (NodeEntity) -> Unit,
    onCreateNote: () -> Unit,
    onSearchClick: () -> Unit,
    onTakePhoto: () -> Unit,
    onNavigateToFolder: ((folderId: String?) -> Unit)? = null,
    onNavigateToArchive: (() -> Unit)? = null,
    onNavigateToTrash: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val nodes by viewModel.nodes.collectAsState()
    val breadcrumbs by viewModel.breadcrumbPath.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Photo Picker
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.importImage(uri)
        }
    }
    
    // Generic File Picker (all types)
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.importFile(uri)
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Rubix",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                // Notes (Home)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Notes, contentDescription = "Notes") },
                    label = { Text("Notes") },
                    selected = true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToFolder?.invoke(null)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                // Archive
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Archive, contentDescription = "Archive") },
                    label = { Text("Archive") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToArchive?.invoke()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                // Trash
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Delete, contentDescription = "Trash") },
                    label = { Text("Trash") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTrash?.invoke()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        GradientBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    ) {
                        // Google Keep-style Search Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(28.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        RoundedCornerShape(28.dp)
                                    )
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { onSearchClick() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hamburger Menu - Opens Drawer
                                IconButton(onClick = { 
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Search Text
                                Text(
                                    text = "Search your notes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Profile Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                        
                        // Breadcrumb Bar (only show when not at root or navigating)
                        if (breadcrumbs.isNotEmpty() || currentFolder != null) {
                            BreadcrumbBar(
                                breadcrumbs = breadcrumbs,
                                currentFolderName = currentFolder?.title,
                                onNavigate = { folderId ->
                                    onNavigateToFolder?.invoke(folderId)
                                },
                                onDropItem = { itemId, targetFolderId ->
                                    viewModel.moveItem(itemId, targetFolderId)
                                }
                            )
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
                        onImportFile = {
                            pickFile.launch("*/*")
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
                    if (nodes.isEmpty()) {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Notes you add appear here",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            contentPadding = PaddingValues(12.dp),
                            verticalItemSpacing = 10.dp,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(nodes, key = { it.id }) { node ->
                                GlideMenuBox(
                                    options = listOf(
                                        GlideMenuOption(
                                            icon = Icons.Filled.PushPin,
                                            label = if (node.isPinned) "Unpin" else "Pin",
                                            color = Color(0xFFFFA000), // Amber
                                            onClick = { viewModel.togglePin(node.id) }
                                        ),
                                        GlideMenuOption(
                                            icon = Icons.Filled.Palette,
                                            label = "Color",
                                            color = Color(0xFF7E57C2), // Purple
                                            onClick = { /* TODO: Color picker */ }
                                        ),
                                        GlideMenuOption(
                                            icon = Icons.Filled.Archive,
                                            label = "Archive",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            onClick = { viewModel.moveToArchive(node.id) }
                                        ),
                                        GlideMenuOption(
                                            icon = Icons.Filled.Delete,
                                            label = "Delete",
                                            color = Color(0xFFE53935), // Red
                                            onClick = { viewModel.moveToTrash(node.id) }
                                        )
                                    )
                                ) {
                                    NodeItemView(
                                        node = node,
                                        onClick = { onNodeClick(node) },
                                        onDrop = { draggedNodeId ->
                                            viewModel.moveItem(draggedNodeId, node.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
