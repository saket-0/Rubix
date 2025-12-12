package com.example.rubix.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Glide Menu option configuration
 */
data class GlideMenuOption(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onClick: () -> Unit
)

/**
 * Touch-drag-release gesture menu.
 * - Touch triggers menu display immediately
 * - Drag highlights options
 * - Release executes highlighted option
 */
@Composable
fun GlideMenuBox(
    options: List<GlideMenuOption>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    var showMenu by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    var lastHoveredIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var triggerPositionY by remember { mutableStateOf(0f) }
    
    
    val optionHeight = with(density) { 48.dp.toPx() }
    
    Box(modifier = modifier) {
        content()
        
        // Glide trigger icon - positioned at bottom end
        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .onGloballyPositioned { coordinates ->
                        triggerPositionY = coordinates.positionInRoot().y
                    }
            ) {
                // Trigger button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .pointerInput(options) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                
                                // Show menu immediately on touch
                                showMenu = true
                                hoveredIndex = -1
                                lastHoveredIndex = -1
                                dragOffsetY = 0f
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                
                                // Track drag
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    
                                    if (!change.pressed) {
                                        // Finger released
                                        if (hoveredIndex >= 0 && hoveredIndex < options.size) {
                                            options[hoveredIndex].onClick()
                                        }
                                        showMenu = false
                                        hoveredIndex = -1
                                        break
                                    }
                                    
                                    val delta = change.positionChange()
                                    dragOffsetY += delta.y
                                    
                                    // Menu is above: drag UP (negative Y) to select options
                                    // First option at top of menu = last in reversed order
                                    val offset = -dragOffsetY // Positive when dragging up
                                    val newIndex = if (offset > 20) {
                                        // Calculate from bottom of menu upward
                                        val idx = ((offset - 20) / optionHeight).toInt()
                                        idx.coerceIn(0, options.size - 1)
                                    } else {
                                        -1
                                    }
                                    
                                    if (newIndex != lastHoveredIndex && newIndex >= 0) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        lastHoveredIndex = newIndex
                                    }
                                    hoveredIndex = newIndex
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Menu popup - anchored to this Box (next to trigger)
                if (showMenu) {
                    Popup(
                        alignment = Alignment.BottomEnd,
                        offset = IntOffset(0, -36), // Position above the trigger
                        onDismissRequest = { showMenu = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(160.dp)
                                    .padding(vertical = 4.dp)
                            ) {
                                // Options in reverse order (first option closest to finger)
                                options.reversed().forEachIndexed { reversedIndex, option ->
                                    val actualIndex = options.size - 1 - reversedIndex
                                    val isHovered = hoveredIndex == actualIndex
                                    val scale by animateFloatAsState(
                                        targetValue = if (isHovered) 1.05f else 1f,
                                        animationSpec = spring(),
                                        label = "option_scale"
                                    )
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scale(scale)
                                            .background(
                                                if (isHovered) option.color.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = option.label,
                                            tint = option.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isHovered) option.color else MaterialTheme.colorScheme.onSurface
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
}
