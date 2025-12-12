package com.example.rubix.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rubix.data.local.NodeEntity

/**
 * Breadcrumb navigation bar with drag-and-drop support.
 * Shows the path from root to current folder, each segment being a drop target.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BreadcrumbBar(
    breadcrumbs: List<NodeEntity>,
    currentFolderName: String?,
    onNavigate: (folderId: String?) -> Unit,
    onDropItem: (itemId: String, targetFolderId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Home (Root) item
        item(key = "home") {
            BreadcrumbItem(
                text = null,
                isHome = true,
                isActive = breadcrumbs.isEmpty() && currentFolderName == null,
                onClick = { onNavigate(null) },
                onDrop = { itemId -> onDropItem(itemId, null) }
            )
        }
        
        // Ancestor folders
        items(breadcrumbs, key = { it.id }) { folder ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                BreadcrumbItem(
                    text = folder.title,
                    isHome = false,
                    isActive = false,
                    onClick = { onNavigate(folder.id) },
                    onDrop = { itemId -> onDropItem(itemId, folder.id) }
                )
            }
        }
        
        // Current folder (if we're in a folder)
        if (currentFolderName != null) {
            item(key = "current") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = currentFolderName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BreadcrumbItem(
    text: String?,
    isHome: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    onDrop: (itemId: String) -> Unit
) {
    val view = LocalView.current
    var isHovered by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        label = "breadcrumb_scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHovered -> MaterialTheme.colorScheme.primaryContainer
            isActive -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> Color.Transparent
        },
        label = "breadcrumb_bg"
    )
    
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val dragData = event.toAndroidDragEvent().clipData
                if (dragData != null && dragData.itemCount > 0) {
                    val itemId = dragData.getItemAt(0).text.toString()
                    onDrop(itemId)
                    return true
                }
                return false
            }
            
            override fun onEntered(event: DragAndDropEvent) {
                isHovered = true
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            
            override fun onExited(event: DragAndDropEvent) {
                isHovered = false
            }
            
            override fun onEnded(event: DragAndDropEvent) {
                isHovered = false
            }
        }
    }
    
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isHome) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Home",
                tint = if (isActive) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = text ?: "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) 
                    MaterialTheme.colorScheme.onSurface 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
