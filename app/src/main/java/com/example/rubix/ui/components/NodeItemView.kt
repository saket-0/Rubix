package com.example.rubix.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.data.local.NodeType
import java.io.File

/**
 * Node item card with drag-and-drop support.
 * - All items can be dragged (drag source)
 * - Folders can receive drops (drop target)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeItemView(
    node: NodeEntity,
    onClick: () -> Unit,
    isDragging: Boolean = false,
    onDrop: ((draggedNodeId: String) -> Unit)? = null
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    var isDropHovered by remember { mutableStateOf(false) }
    
    // Scale animation for press and drop hover
    val scale by animateFloatAsState(
        targetValue = when {
            isDropHovered -> 1.08f
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    // Alpha for when being dragged
    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 0.4f else 1f,
        label = "drag_alpha"
    )
    
    // Border color for drop target feedback
    val borderColor by animateColorAsState(
        targetValue = if (isDropHovered) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        label = "border_color"
    )
    
    // Drop target for folders
    val dropTarget = remember(node.id, onDrop) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val dragData = event.toAndroidDragEvent().clipData
                if (dragData != null && dragData.itemCount > 0) {
                    val draggedNodeId = dragData.getItemAt(0).text.toString()
                    // Don't drop onto self
                    if (draggedNodeId != node.id) {
                        onDrop?.invoke(draggedNodeId)
                        return true
                    }
                }
                return false
            }
            
            override fun onEntered(event: DragAndDropEvent) {
                isDropHovered = true
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            
            override fun onExited(event: DragAndDropEvent) {
                isDropHovered = false
            }
            
            override fun onEnded(event: DragAndDropEvent) {
                isDropHovered = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Drag source - all items can be dragged
            .dragAndDropSource {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { offset ->
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        startTransfer(
                            DragAndDropTransferData(
                                clipData = ClipData.newPlainText("node_id", node.id),
                                flags = android.view.View.DRAG_FLAG_GLOBAL
                            )
                        )
                    }
                )
            }
            // Drop target - only for folders
            .then(
                if (node.type == NodeType.FOLDER && onDrop != null) {
                    Modifier.dragAndDropTarget(
                        shouldStartDragAndDrop = { true },
                        target = dropTarget
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDropHovered) 8.dp else 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isDropHovered) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (node.type) {
                NodeType.FOLDER -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Folder",
                            modifier = Modifier.size(64.dp),
                            tint = if (isDropHovered) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = node.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                NodeType.NOTE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = node.title.ifEmpty { "Untitled" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (!node.content.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = node.content,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    AsyncImage(
                        model = File(node.previewPath ?: (node.thumbnailPath ?: "")),
                        contentDescription = node.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(node.aspectRatio)
                    )
                }
            }
        }
    }
}
