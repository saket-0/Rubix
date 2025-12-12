package com.example.rubix.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.rubix.ui.components.GlideMenuBox
import com.example.rubix.ui.components.GlideMenuOption
import com.example.rubix.ui.components.GradientBackground
import com.example.rubix.ui.components.NodeItemView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    navController: NavController,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val trashedNodes by viewModel.trashedNodes.collectAsState()
    
    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Trash") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (trashedNodes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Trash is empty",
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
                        items(trashedNodes, key = { it.id }) { node ->
                            GlideMenuBox(
                                options = listOf(
                                    GlideMenuOption(
                                        icon = Icons.Filled.RestoreFromTrash,
                                        label = "Restore",
                                        color = Color(0xFF00897B), // Teal
                                        onClick = { viewModel.restore(node.id) }
                                    ),
                                    GlideMenuOption(
                                        icon = Icons.Filled.DeleteForever,
                                        label = "Delete Forever",
                                        color = Color(0xFFE53935), // Red
                                        onClick = { viewModel.deleteForever(node.id) }
                                    )
                                )
                            ) {
                                NodeItemView(
                                    node = node,
                                    onClick = { /* View only in trash */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
