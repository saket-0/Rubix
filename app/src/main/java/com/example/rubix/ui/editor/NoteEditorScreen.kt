package com.example.rubix.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.rubix.ui.components.GradientBackground

@Composable
fun NoteEditorScreen(
    navController: NavController,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val richTextState = remember { RichTextState() }
    
    // Initialize rich text state with content
    LaunchedEffect(uiState.content) {
        if (uiState.content.isNotEmpty() && richTextState.getPlainText().isEmpty()) {
            richTextState.setContent(uiState.content)
        }
    }

    // Save on exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.updateContent(uiState.title, richTextState.getContentAsHtml())
            viewModel.saveNow()
        }
    }

    // GradientBackground fills entire screen (extends behind status bar)
    GradientBackground {
        // Content respects system bar and IME padding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            // Top Bar (Google Keep style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Action icons
                Row {
                    IconButton(onClick = { /* TODO: Pin */ }) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pin",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* TODO: Reminder */ }) {
                        Icon(
                            imageVector = Icons.Filled.NotificationAdd,
                            contentDescription = "Add reminder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { /* TODO: Archive */ }) {
                        Icon(
                            imageVector = Icons.Filled.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Content area (scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Title Input (larger, lighter weight like Google Keep)
                BasicTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateContent(it, richTextState.getContentAsHtml()) },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 32.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (uiState.title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    lineHeight = 32.sp
                                )
                            )
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Content Input
                BasicTextField(
                    value = richTextState.textFieldValue,
                    onValueChange = { richTextState.onValueChange(it) },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    decorationBox = { innerTextField ->
                        if (richTextState.getPlainText().isEmpty()) {
                            Text(
                                text = "Note",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    lineHeight = 24.sp
                                )
                            )
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
            
            // Bottom Toolbar (Google Keep style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Formatting buttons (left side)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Bold
                    IconButton(
                        onClick = { richTextState.toggleBold() },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (richTextState.isBold) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatBold,
                            contentDescription = "Bold",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Italic
                    IconButton(
                        onClick = { richTextState.toggleItalic() },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (richTextState.isItalic) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatItalic,
                            contentDescription = "Italic",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Underline
                    IconButton(
                        onClick = { richTextState.toggleUnderline() },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (richTextState.isUnderline) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatUnderlined,
                            contentDescription = "Underline",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Right side actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Undo
                    IconButton(
                        onClick = { richTextState.undo() },
                        enabled = richTextState.canUndo,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(20.dp),
                            tint = if (richTextState.canUndo) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                    
                    // Redo
                    IconButton(
                        onClick = { richTextState.redo() },
                        enabled = richTextState.canRedo,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            modifier = Modifier.size(20.dp),
                            tint = if (richTextState.canRedo) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                    
                    // More options
                    IconButton(
                        onClick = { /* TODO: More options */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}



