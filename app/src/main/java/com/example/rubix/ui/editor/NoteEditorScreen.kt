package com.example.rubix.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.Modifier
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

    GradientBackground(
        modifier = Modifier
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable content area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Title Input
                BasicTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateContent(it, richTextState.getContentAsHtml()) },
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (uiState.title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content Input with Rich Text
                BasicTextField(
                    value = richTextState.textFieldValue,
                    onValueChange = { richTextState.onValueChange(it) },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    decorationBox = { innerTextField ->
                        if (richTextState.getPlainText().isEmpty()) {
                            Text(
                                text = "Start typing...",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
            
            // Formatting Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Bold
                IconButton(
                    onClick = { richTextState.toggleBold() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (richTextState.isBold) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatBold,
                        contentDescription = "Bold",
                        tint = if (richTextState.isBold) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Italic
                IconButton(
                    onClick = { richTextState.toggleItalic() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (richTextState.isItalic) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatItalic,
                        contentDescription = "Italic",
                        tint = if (richTextState.isItalic) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Underline
                IconButton(
                    onClick = { richTextState.toggleUnderline() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (richTextState.isUnderline) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatUnderlined,
                        contentDescription = "Underline",
                        tint = if (richTextState.isUnderline) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Undo
                IconButton(
                    onClick = { richTextState.undo() },
                    enabled = richTextState.canUndo
                ) {
                    Icon(
                        imageVector = Icons.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (richTextState.canUndo) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                
                // Redo
                IconButton(
                    onClick = { richTextState.redo() },
                    enabled = richTextState.canRedo
                ) {
                    Icon(
                        imageVector = Icons.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (richTextState.canRedo) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}


