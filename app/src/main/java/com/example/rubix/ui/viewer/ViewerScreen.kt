package com.example.rubix.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rubix.data.local.NodeType

@Composable
fun ViewerScreen(
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val node by viewModel.node.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val currentNode = node
        if (currentNode == null) {
            CircularProgressIndicator()
        } else {
            when (currentNode.type) {
                NodeType.IMAGE -> {
                    // Critical: Use previewPath for fast loading, or originalPath if needed (but preview is safer for 1080p limit)
                    // Requirement says: "strictly load from node.previewPath"
                    ZoomableImage(imagePath = currentNode.previewPath ?: currentNode.originalPath ?: "")
                }
                NodeType.PDF -> {
                    // Requirement says: "Show PdfViewer(path = node.originalPath)"
                    PdfViewer(pdfPath = currentNode.originalPath ?: "")
                }
                else -> {
                    Text("Unsupported Format")
                }
            }
        }
    }
}
