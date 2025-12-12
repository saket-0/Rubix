package com.example.rubix.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
    var isDragging by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    var lastHoveredIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var triggerPosition by remember { mutableStateOf(0f) }
    var showAbove by remember { mutableStateOf(true) }
    
    val optionHeight = with(density) { 48.dp.toPx() }
    
    Box(modifier = modifier) {
        content()
        
        // Glide trigger icon
        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .onGloballyPositioned { coordinates ->
                        triggerPosition = coordinates.positionInRoot().y
                        showAbove = triggerPosition > screenHeight * 0.6f
                    }
                    .pointerInput(options) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            
                            // Show menu immediately on touch
                            showMenu = true
                            isDragging = true
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
                                    isDragging = false
                                    hoveredIndex = -1
                                    break
                                }
                                
                                val delta = change.positionChange()
                                dragOffsetY += delta.y
                                
                                // Calculate which option is hovered
                                val menuDirection = if (showAbove) -1 else 1
                                val newIndex = if (showAbove) {
                                    // Menu above: negative Y means going up into menu
                                    val offset = -dragOffsetY
                                    if (offset > 20) {
                                        ((offset - 20) / optionHeight).toInt().coerceIn(0, options.size - 1)
                                    } else -1
                                } else {
                                    // Menu below: positive Y means going down into menu
                                    val offset = dragOffsetY
                                    if (offset > 20) {
                                        ((offset - 20) / optionHeight).toInt().coerceIn(0, options.size - 1)
                                    } else -1
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
        }
        
        // Menu popup
        if (showMenu) {
            Popup(
                alignment = if (showAbove) Alignment.BottomEnd else Alignment.TopEnd,
                offset = IntOffset(0, if (showAbove) -40 else 40),
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
                        options.forEachIndexed { index, option ->
                            val isHovered = hoveredIndex == index
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
