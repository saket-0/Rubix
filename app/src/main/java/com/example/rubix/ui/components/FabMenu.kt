package com.example.rubix.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * FAB with radial menu supporting two interaction flows:
 * 
 * Flow A: Tap-to-Open
 * - Tap the button → menu expands and stays open
 * - Tap desired option to execute action
 * 
 * Flow B: Drag-to-Select (Gesture)
 * - Touch and drag immediately (no long press)
 * - Hovering over option scales it up + haptic feedback
 * - Release finger to execute action
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
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    var lastHoveredIndex by remember { mutableIntStateOf(-1) }
    var isDragging by remember { mutableStateOf(false) }
    
    val view = LocalView.current
    val density = LocalDensity.current
    val radiusDp = 120.dp
    val radiusPx = with(density) { radiusDp.toPx() }
    val buttonSizeDp = 52.dp
    val buttonSizePx = with(density) { buttonSizeDp.toPx() }
    val dragThreshold = with(density) { 10.dp.toPx() }  // Movement threshold to detect drag
    
    // Menu items
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
    
    // Helper to find closest menu item to a position
    fun findHoveredIndex(position: Offset, buttonCenter: Offset): Int {
        var closestIndex = -1
        var closestDistance = Float.MAX_VALUE
        
        menuItems.forEachIndexed { index, item ->
            val angleRad = Math.toRadians(item.angleDegrees.toDouble())
            val itemX = buttonCenter.x + (radiusPx * cos(angleRad)).toFloat()
            val itemY = buttonCenter.y + (radiusPx * sin(angleRad)).toFloat()
            
            val distance = sqrt(
                (position.x - itemX).pow(2) + 
                (position.y - itemY).pow(2)
            )
            
            if (distance < closestDistance && distance < radiusPx * 0.6f) {
                closestDistance = distance
                closestIndex = index
            }
        }
        return closestIndex
    }
    
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
        // Radial sub-buttons
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
                        isDragging = false
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
        
        // Main FAB with unified gesture handling
        FloatingActionButton(
            onClick = { }, // Handled by pointerInput
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPosition = down.position
                        val buttonCenter = Offset(buttonSizePx / 2, buttonSizePx / 2)
                        
                        var totalMovement = Offset.Zero
                        var dragStarted = false
                        
                        // If menu is already open from a previous tap, track for selection
                        if (expanded) {
                            dragStarted = true
                            isDragging = true
                        }
                        
                        // Track drag
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            
                            if (!change.pressed) {
                                // Finger lifted
                                if (dragStarted && hoveredIndex >= 0 && hoveredIndex < menuItems.size) {
                                    // Execute hovered action
                                    menuItems[hoveredIndex].onClick()
                                    expanded = false
                                    hoveredIndex = -1
                                } else if (!dragStarted) {
                                    // Simple tap - create note directly
                                    onCreateNote()
                                } else if (dragStarted && hoveredIndex < 0) {
                                    // Drag ended but not on any item - close menu
                                    expanded = false
                                }
                                isDragging = false
                                lastHoveredIndex = -1
                                break
                            }
                            
                            val positionDelta = change.positionChange()
                            totalMovement += positionDelta
                            
                            // Detect if this is a drag (moved beyond threshold)
                            if (!dragStarted && (abs(totalMovement.x) > dragThreshold || abs(totalMovement.y) > dragThreshold)) {
                                dragStarted = true
                                isDragging = true
                                expanded = true
                            }
                            
                            // Update hover state during drag
                            if (dragStarted) {
                                val currentPosition = downPosition + totalMovement
                                val newHoveredIndex = findHoveredIndex(currentPosition, buttonCenter)
                                
                                // Haptic feedback when hovering over a new item
                                if (newHoveredIndex != lastHoveredIndex && newHoveredIndex >= 0) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    lastHoveredIndex = newHoveredIndex
                                }
                                
                                hoveredIndex = newHoveredIndex
                            }
                        }
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create Note"
            )
        }
    }
}
