package com.example.rubix.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * State holder for rich text editing with Bold, Italic, Underline support.
 * Includes undo/redo functionality.
 */
class RichTextState {
    
    var textFieldValue by mutableStateOf(TextFieldValue())
        private set
    
    private val undoStack = mutableStateListOf<TextFieldValue>()
    private val redoStack = mutableStateListOf<TextFieldValue>()
    private var lastSavedValue: TextFieldValue? = null
    
    // Current styles at cursor/selection
    var isBold by mutableStateOf(false)
        private set
    var isItalic by mutableStateOf(false)
        private set
    var isUnderline by mutableStateOf(false)
        private set
    
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    
    /**
     * Update text from user input
     */
    fun onValueChange(newValue: TextFieldValue) {
        // Save to undo stack if significant change
        val previousValue = textFieldValue
        if (previousValue.text != newValue.text) {
            if (lastSavedValue == null || 
                previousValue.text.length - lastSavedValue!!.text.length >= 5 ||
                newValue.text.length - lastSavedValue!!.text.length >= 5) {
                undoStack.add(previousValue)
                if (undoStack.size > 50) undoStack.removeAt(0)
                redoStack.clear()
                lastSavedValue = previousValue
            }
        }
        
        textFieldValue = newValue
        updateCurrentStyles()
    }
    
    /**
     * Toggle bold on selection
     */
    fun toggleBold() {
        applyStyle(SpanStyle(fontWeight = FontWeight.Bold))
        isBold = !isBold
    }
    
    /**
     * Toggle italic on selection
     */
    fun toggleItalic() {
        applyStyle(SpanStyle(fontStyle = FontStyle.Italic))
        isItalic = !isItalic
    }
    
    /**
     * Toggle underline on selection
     */
    fun toggleUnderline() {
        applyStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        isUnderline = !isUnderline
    }
    
    /**
     * Undo last action
     */
    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(textFieldValue)
            textFieldValue = undoStack.removeLast()
            updateCurrentStyles()
        }
    }
    
    /**
     * Redo last undone action
     */
    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(textFieldValue)
            textFieldValue = redoStack.removeLast()
            updateCurrentStyles()
        }
    }
    
    /**
     * Set initial content (for loading existing notes)
     */
    fun setContent(text: String) {
        // Parse HTML-like content if present
        textFieldValue = TextFieldValue(
            annotatedString = parseHtmlToAnnotated(text),
            selection = TextRange(0)
        )
        undoStack.clear()
        redoStack.clear()
    }
    
    /**
     * Get content as HTML-like string for storage
     */
    fun getContentAsHtml(): String {
        return annotatedToHtml(textFieldValue.annotatedString)
    }
    
    /**
     * Get plain text content
     */
    fun getPlainText(): String {
        return textFieldValue.text
    }
    
    private fun applyStyle(style: SpanStyle) {
        val selection = textFieldValue.selection
        if (selection.collapsed) return // No selection
        
        val text = textFieldValue.annotatedString
        val newAnnotatedString = buildAnnotatedString {
            append(text.subSequence(0, selection.min))
            withStyle(style) {
                append(text.subSequence(selection.min, selection.max))
            }
            append(text.subSequence(selection.max, text.length))
        }
        
        textFieldValue = textFieldValue.copy(annotatedString = newAnnotatedString)
    }
    
    private fun updateCurrentStyles() {
        val selection = textFieldValue.selection
        if (selection.collapsed && selection.start < textFieldValue.annotatedString.length) {
            val styles = textFieldValue.annotatedString.getSpanStyles(selection.start)
            isBold = styles.any { it.item.fontWeight == FontWeight.Bold }
            isItalic = styles.any { it.item.fontStyle == FontStyle.Italic }
            isUnderline = styles.any { it.item.textDecoration == TextDecoration.Underline }
        }
    }
    
    private fun AnnotatedString.getSpanStyles(index: Int): List<AnnotatedString.Range<SpanStyle>> {
        return spanStyles.filter { index in it.start until it.end }
    }
    
    /**
     * Parse simple HTML tags to AnnotatedString
     */
    private fun parseHtmlToAnnotated(html: String): AnnotatedString {
        // Simple HTML parser for <b>, <i>, <u> tags
        return buildAnnotatedString {
            var text = html
            var currentIndex = 0
            
            // Regex to find tags
            val tagRegex = Regex("<(/?)(b|i|u)>", RegexOption.IGNORE_CASE)
            val boldStack = mutableListOf<Int>()
            val italicStack = mutableListOf<Int>()
            val underlineStack = mutableListOf<Int>()
            
            var cleanText = ""
            var lastEnd = 0
            
            tagRegex.findAll(html).forEach { match ->
                cleanText += html.substring(lastEnd, match.range.first)
                val isClosing = match.groupValues[1] == "/"
                val tagName = match.groupValues[2].lowercase()
                
                when (tagName) {
                    "b" -> if (isClosing && boldStack.isNotEmpty()) {
                        val start = boldStack.removeLast()
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, cleanText.length)
                    } else if (!isClosing) {
                        boldStack.add(cleanText.length)
                    }
                    "i" -> if (isClosing && italicStack.isNotEmpty()) {
                        val start = italicStack.removeLast()
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, cleanText.length)
                    } else if (!isClosing) {
                        italicStack.add(cleanText.length)
                    }
                    "u" -> if (isClosing && underlineStack.isNotEmpty()) {
                        val start = underlineStack.removeLast()
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, cleanText.length)
                    } else if (!isClosing) {
                        underlineStack.add(cleanText.length)
                    }
                }
                lastEnd = match.range.last + 1
            }
            cleanText += html.substring(lastEnd)
            
            append(cleanText)
        }
    }
    
    /**
     * Convert AnnotatedString to HTML
     */
    private fun annotatedToHtml(annotated: AnnotatedString): String {
        if (annotated.spanStyles.isEmpty()) {
            return annotated.text
        }
        
        // Build HTML with tags
        val result = StringBuilder()
        var lastEnd = 0
        
        // Sort spans by start position
        val sortedSpans = annotated.spanStyles.sortedBy { it.start }
        
        // Simple approach: wrap each span individually
        // For production, would need proper nesting handling
        val text = annotated.text
        
        data class TagEvent(val index: Int, val isOpen: Boolean, val tag: String)
        val events = mutableListOf<TagEvent>()
        
        annotated.spanStyles.forEach { span ->
            val tag = when {
                span.item.fontWeight == FontWeight.Bold -> "b"
                span.item.fontStyle == FontStyle.Italic -> "i"
                span.item.textDecoration == TextDecoration.Underline -> "u"
                else -> null
            }
            if (tag != null) {
                events.add(TagEvent(span.start, true, tag))
                events.add(TagEvent(span.end, false, tag))
            }
        }
        
        events.sortWith(compareBy({ it.index }, { !it.isOpen }))
        
        var currentIndex = 0
        events.forEach { event ->
            if (event.index > currentIndex) {
                result.append(text.substring(currentIndex, event.index))
                currentIndex = event.index
            }
            result.append(if (event.isOpen) "<${event.tag}>" else "</${event.tag}>")
        }
        if (currentIndex < text.length) {
            result.append(text.substring(currentIndex))
        }
        
        return result.toString()
    }
}
