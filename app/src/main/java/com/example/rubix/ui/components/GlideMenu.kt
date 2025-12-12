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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
 * Touch-drag-release gesture menu (Glide Menu).
 * 
 * Interaction Model:
 * - Touch-down: Instantly shows menu + haptic
 * - Drag: Slide finger to highlight options with haptic on each selection change
 * - Release: Executes highlighted action (if any) and closes menu
 * - Out of bounds: Releasing outside menu area closes without action
 * 
 * Features:
 * - Hamburger icon with animated opacity (0.5 idle -> 1.0 active)
 * - Position-aware: Shows downward if near top of screen, upward otherwise
 * - Gesture blocking callback: onMenuOpenChanged
 */
@Composable
fun GlideMenuBox(
    options: List<GlideMenuOption>,
    enabled: Boolean = true,
    onMenuOpenChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    var showMenu by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    var lastHoveredIndex by remember { mutableIntStateOf(-1) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var triggerPositionY by remember { mutableFloatStateOf(0f) }
    // Calculate direction at menu open time and store it
    var menuShowsDownward by remember { mutableStateOf(false) }
    
    // Option height in pixels
    val optionHeightPx = with(density) { 48.dp.toPx() }
    // Threshold for "near top of screen" - top 25%
    val topThresholdPx = screenHeightPx * 0.25f
    
    // Animated opacity for the hamburger icon
    val iconAlpha by animateFloatAsState(
        targetValue = if (showMenu) 1f else 0.5f,
        animationSpec = spring(),
        label = "icon_opacity"
    )
    
    // Notify parent when menu state changes
    LaunchedEffect(showMenu) {
        onMenuOpenChanged?.invoke(showMenu)
    }

    Box(modifier = modifier) {
        content()
        
        // Glide trigger icon - positioned at bottom end of the card
        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .onGloballyPositioned { coordinates ->
                        triggerPositionY = coordinates.positionInRoot().y
                    }
            ) {
                // Trigger button - hamburger menu icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                // Wait for first touch
                                awaitFirstDown(requireUnconsumed = false)
                                
                                // Determine direction at moment of touch based on current position
                                menuShowsDownward = triggerPositionY < topThresholdPx
                                
                                // Show menu immediately
                                showMenu = true
                                hoveredIndex = -1
                                lastHoveredIndex = -1
                                accumulatedDragY = 0f
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                
                                // Track drag movement
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    
                                    if (!change.pressed) {
                                        // Finger released - execute highlighted action if valid
                                        if (hoveredIndex in 0 until options.size) {
                                            options[hoveredIndex].onClick()
                                        }
                                        showMenu = false
                                        hoveredIndex = -1
                                        break
                                    }
                                    
                                    // Accumulate drag delta
                                    val delta = change.positionChange()
                                    accumulatedDragY += delta.y
                                    
                                    // Calculate which option is hovered
                                    val newIndex = calculateHoveredIndex(
                                        accumulatedDragY = accumulatedDragY,
                                        optionHeightPx = optionHeightPx,
                                        optionCount = options.size,
                                        showDownward = menuShowsDownward
                                    )
                                    
                                    // Haptic feedback on selection change (only when entering a valid option)
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
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = Color.White.copy(alpha = iconAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Menu popup - position based on menuShowsDownward
                if (showMenu) {
                    Popup(
                        alignment = if (menuShowsDownward) Alignment.TopEnd else Alignment.BottomEnd,
                        offset = if (menuShowsDownward) IntOffset(0, 40) else IntOffset(0, -40),
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
                                // Display options based on direction
                                // Downward: normal order (index 0 at top, closest to finger)
                                // Upward: reversed order (index 0 at bottom, closest to finger)
                                val displayOptions = if (menuShowsDownward) {
                                    options.mapIndexed { index, option -> index to option }
                                } else {
                                    options.reversed().mapIndexed { reversedIndex, option ->
                                        (options.size - 1 - reversedIndex) to option
                                    }
                                }
                                
                                displayOptions.forEach { (actualIndex, option) ->
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

/**
 * Calculate which option index is hovered based on drag offset.
 * Returns -1 if finger is outside the menu bounds.
 */
private fun calculateHoveredIndex(
    accumulatedDragY: Float,
    optionHeightPx: Float,
    optionCount: Int,
    showDownward: Boolean
): Int {
    // Dead zone around the trigger before entering menu area (20px)
    val deadZone = 20f
    
    return if (showDownward) {
        // Menu below trigger: drag DOWN (positive Y) to select
        if (accumulatedDragY > deadZone) {
            val idx = ((accumulatedDragY - deadZone) / optionHeightPx).toInt()
            if (idx in 0 until optionCount) idx else -1
        } else {
            -1 // Finger still in dead zone or dragging up (out of bounds)
        }
    } else {
        // Menu above trigger: drag UP (negative Y) to select
        val upwardOffset = -accumulatedDragY // Convert to positive when dragging up
        if (upwardOffset > deadZone) {
            val idx = ((upwardOffset - deadZone) / optionHeightPx).toInt()
            if (idx in 0 until optionCount) idx else -1
        } else {
            -1 // Finger still in dead zone or dragging down (out of bounds)
        }
    }
}
