package com.example.rubix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A subtle vertical gradient background that adds depth to the UI.
 * Works in both light and dark modes.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Create a subtle gradient: surface at top, surface + slight primary tint at bottom
    val gradientColors = listOf(
        surfaceColor,
        surfaceColor.blend(primaryColor, 0.05f)
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        content()
    }
}

/**
 * Blend two colors together by a given ratio.
 * @param other The color to blend with
 * @param ratio 0.0 = this color, 1.0 = other color
 */
private fun Color.blend(other: Color, ratio: Float): Color {
    val inverseRatio = 1f - ratio
    return Color(
        red = this.red * inverseRatio + other.red * ratio,
        green = this.green * inverseRatio + other.green * ratio,
        blue = this.blue * inverseRatio + other.blue * ratio,
        alpha = this.alpha
    )
}
