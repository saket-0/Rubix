package com.example.rubix.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * FAB with tap-to-create-note and drag-to-reveal radial menu.
 * - Single tap: Creates a new note immediately
 * - Drag: Reveals radial menu with import options
 */
@Composable
fun FabMenu(
    onCreateFolder: () -> Unit,
    onCreateNote: () -> Unit,
    onImportImage: () -> Unit,
    onImportFile: () -> Unit,
    onTakePhoto: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf(-1) }
    var isDragging by remember { mutableStateOf(false) }
    
    val density = LocalDensity.current
    val radiusDp = 120.dp  // Increased for better visibility
    val radiusPx = with(density) { radiusDp.toPx() }
    val buttonSizeDp = 52.dp
    val buttonSizePx = with(density) { buttonSizeDp.toPx() }
    
    // Menu items (shown on drag)
    data class RadialItem(
        val icon: ImageVector,
        val label: String,
        val angleDegrees: Float,
        val containerColor: Color,
        val onClick: () -> Unit
    )
    
    val menuItems = listOf(
        RadialItem(
            icon = Icons.Filled.CreateNewFolder,
            label = "New Folder",
            angleDegrees = 180f,  // Left
            containerColor = Color(0xFF2196F3),  // Blue
            onClick = onCreateFolder
        ),
        RadialItem(
            icon = Icons.Filled.CameraAlt,
            label = "Take Photo",
            angleDegrees = 210f,
            containerColor = Color(0xFF9C27B0),  // Purple
            onClick = onTakePhoto
        ),
        RadialItem(
            icon = Icons.Filled.Image,
            label = "Import Image",
            angleDegrees = 240f,
            containerColor = Color(0xFF4CAF50),  // Green
            onClick = onImportImage
        ),
        RadialItem(
            icon = Icons.Filled.AttachFile,
            label = "Import File",
            angleDegrees = 270f,  // Top
            containerColor = Color(0xFFFF9800),  // Orange
            onClick = onImportFile
        )
    )
    
    // Animated expansion
    val expansionProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expansion"
    )
    
    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        // Radial sub-buttons (only when expanded via drag)
        if (expansionProgress > 0.01f) {
            menuItems.forEachIndexed { index, item ->
                val angleRad = Math.toRadians(item.angleDegrees.toDouble())
                val currentRadius = radiusPx * expansionProgress
                
                val offsetX = (currentRadius * cos(angleRad)).toFloat()
                val offsetY = (currentRadius * sin(angleRad)).toFloat()
                
                // Scale for hover feedback
                val itemScale by animateFloatAsState(
                    targetValue = if (hoveredIndex == index) 1.25f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "item_scale_$index"
                )
                
                SmallFloatingActionButton(
                    onClick = {
                        expanded = false
                        hoveredIndex = -1
                        item.onClick()
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = offsetX.toInt(),
                                y = offsetY.toInt()
                            )
                        }
                        .size(buttonSizeDp * itemScale),
                    containerColor = item.containerColor,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp * expansionProgress
                    )
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                }
            }
        }
        
        // Main FAB - Tap to create note, drag to show menu
        FloatingActionButton(
            onClick = { }, // Handled by pointerInput
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Single tap = create note
                            if (!isDragging) {
                                onCreateNote()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            expanded = true
                        },
                        onDrag = { change, _ ->
                            val dragPos = change.position
                            val mainButtonCenter = Offset(buttonSizePx / 2, buttonSizePx / 2)
                            
                            var closestIndex = -1
                            var closestDistance = Float.MAX_VALUE
                            
                            menuItems.forEachIndexed { index, item ->
                                val angleRad = Math.toRadians(item.angleDegrees.toDouble())
                                val itemX = mainButtonCenter.x + (radiusPx * cos(angleRad)).toFloat()
                                val itemY = mainButtonCenter.y + (radiusPx * sin(angleRad)).toFloat()
                                
                                val distance = sqrt(
                                    (dragPos.x - itemX).pow(2) + 
                                    (dragPos.y - itemY).pow(2)
                                )
                                
                                if (distance < closestDistance && distance < radiusPx * 0.6f) {
                                    closestDistance = distance
                                    closestIndex = index
                                }
                            }
                            
                            hoveredIndex = closestIndex
                        },
                        onDragEnd = {
                            if (hoveredIndex >= 0 && hoveredIndex < menuItems.size) {
                                menuItems[hoveredIndex].onClick()
                            }
                            expanded = false
                            hoveredIndex = -1
                            isDragging = false
                        },
                        onDragCancel = {
                            expanded = false
                            hoveredIndex = -1
                            isDragging = false
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create Note"
            )
        }
    }
}

