package com.example.rubix.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun FabMenu(
    onCreateFolder: () -> Unit,
    onImportImage: () -> Unit,
    onImportPdf: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f, label = "fab_rotation")

    Column(
        horizontalAlignment = Alignment.End
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FabMenuItem(
                    icon = Icons.Default.CreateNewFolder,
                    text = "New Folder",
                    onClick = {
                        expanded = false
                        onCreateFolder()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                FabMenuItem(
                    icon = Icons.Default.Image,
                    text = "Import Image",
                    onClick = {
                        expanded = false
                        onImportImage()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                FabMenuItem(
                    icon = Icons.Default.PictureAsPdf,
                    text = "Import PDF",
                    onClick = {
                        expanded = false
                        onImportPdf()
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Expand Menu",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        SmallFloatingActionButton(
            onClick = onClick
        ) {
            Icon(imageVector = icon, contentDescription = text)
        }
    }
}
