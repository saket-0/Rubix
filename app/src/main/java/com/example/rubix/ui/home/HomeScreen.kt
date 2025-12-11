package com.example.rubix.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.ui.components.CreateFolderDialog
import com.example.rubix.ui.components.FabMenu
import com.example.rubix.ui.components.NodeItemView

@Composable
fun HomeScreen(
    onNodeClick: (NodeEntity) -> Unit,
    onCreateNote: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val nodes by viewModel.nodes.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Photo Picker
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.importImage(uri)
        }
    }
    
    // PDF Picker (Placeholder for future use with same pattern)
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

    Scaffold(
        floatingActionButton = {
            FabMenu(
                onCreateFolder = { showCreateFolderDialog = true },
                onCreateNote = onCreateNote,
                onImportImage = {
                     pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onImportPdf = {
                     pickPdf.launch("application/pdf")
                }
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
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(nodes) { node ->
                    NodeItemView(
                        node = node, 
                        onClick = { onNodeClick(node) }
                    )
                }
            }
        }
    }
}
