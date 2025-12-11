package com.example.rubix.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
 * Radial FAB Menu - Buttons fan out in an arc when expanded.
 * Supports "slide-to-select" gesture: press FAB, drag to option, release to trigger.
 */
@Composable
fun FabMenu(
    onCreateFolder: () -> Unit,
    onCreateNote: () -> Unit,
    onImportImage: () -> Unit,
    onImportPdf: () -> Unit,
    onTakePhoto: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf(-1) }
    
    val density = LocalDensity.current
    val radiusDp = 100.dp
    val radiusPx = with(density) { radiusDp.toPx() }
    val buttonSizeDp = 48.dp
    val buttonSizePx = with(density) { buttonSizeDp.toPx() }
    
    // Define menu items with angles (in degrees, 0° = right, counter-clockwise)
    data class RadialItem(
        val icon: ImageVector,
        val label: String,
        val angleDegrees: Float,
        val containerColor: Color,
        val onContentColor: Color,
        val onClick: () -> Unit
    )
    
    val menuItems = listOf(
        RadialItem(
            icon = Icons.Filled.CreateNewFolder,
            label = "New Folder",
            angleDegrees = 180f,  // Left
            containerColor = Color(0xFF2196F3),  // Blue
            onContentColor = Color.White,
            onClick = onCreateFolder
        ),
        RadialItem(
            icon = Icons.Filled.CameraAlt,
            label = "Take Photo",
            angleDegrees = 202f,  // Between folder and image import
            containerColor = Color(0xFF9C27B0),  // Purple
            onContentColor = Color.White,
            onClick = onTakePhoto
        ),
        RadialItem(
            icon = Icons.Filled.Image,
            label = "Import Image",
            angleDegrees = 225f,  // Top-Left
            containerColor = Color(0xFF4CAF50),  // Green
            onContentColor = Color.White,
            onClick = onImportImage
        ),
        RadialItem(
            icon = Icons.Filled.Add,
            label = "New Note",
            angleDegrees = 270f,  // Top
            containerColor = Color(0xFFFFC107),  // Yellow/Amber
            onContentColor = Color.Black,
            onClick = onCreateNote
        ),
        RadialItem(
            icon = Icons.Filled.PictureAsPdf,
            label = "Import PDF",
            angleDegrees = 315f,  // Top-Right
            containerColor = Color(0xFFFF9800),  // Orange
            onContentColor = Color.White,
            onClick = onImportPdf
        )
    )
    
    // Animated expansion progress
    val expansionProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "expansion"
    )
    
    // Main FAB rotation
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_rotation"
    )
    
    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        // Radial sub-buttons
        menuItems.forEachIndexed { index, item ->
            val angleRad = Math.toRadians(item.angleDegrees.toDouble())
            val currentRadius = radiusPx * expansionProgress
            
            // Calculate offset from center (negative Y because screen coords are inverted)
            val offsetX = (currentRadius * cos(angleRad)).toFloat()
            val offsetY = (currentRadius * sin(angleRad)).toFloat()
            
            // Scale animation for hover feedback
            val itemScale by animateFloatAsState(
                targetValue = if (hoveredIndex == index) 1.2f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "item_scale_$index"
            )
            
            if (expansionProgress > 0.01f) {
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
                    contentColor = item.onContentColor,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp * expansionProgress
                    )
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                }
            }
        }
        
        // Main FAB with drag detection
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            expanded = true
                        },
                        onDrag = { change, _ ->
                            // Determine which button the finger is over
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
                                
                                if (distance < closestDistance && distance < radiusPx * 0.5f) {
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
                        },
                        onDragCancel = {
                            expanded = false
                            hoveredIndex = -1
                        }
                    )
                }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Add else Icons.Filled.Edit,
                contentDescription = if (expanded) "Close Menu" else "Open Menu",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}
