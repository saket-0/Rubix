package com.example.rubix.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rubix.data.local.NodeDao
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.data.local.NodeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NoteUiState(
    val title: String = "",
    val content: String = "",
    val lastModified: Long = System.currentTimeMillis()
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val nodeDao: NodeDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val nodeId: String? = savedStateHandle["nodeId"]
    private val parentId: String? = savedStateHandle["parentId"]

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private var currentNode: NodeEntity? = null
    private var saveJob: Job? = null
    private var isInitialized = false

    init {
        initializeNote()
    }

    private fun initializeNote() {
        viewModelScope.launch {
            if (nodeId != null) {
                // Editing existing note
                val existingNode = nodeDao.getNode(nodeId).first()
                if (existingNode != null) {
                    currentNode = existingNode
                    _uiState.update {
                        it.copy(
                            title = existingNode.title,
                            content = existingNode.content ?: "",
                            lastModified = existingNode.creationDate
                        )
                    }
                }
            } else {
                // Creating new note
                val newId = UUID.randomUUID().toString()
                currentNode = NodeEntity(
                    id = newId,
                    parentId = parentId,
                    type = NodeType.NOTE,
                    title = "",
                    content = ""
                )
                // We don't save immediately, wait for user input
            }
            isInitialized = true
        }
    }

    fun updateContent(newTitle: String, newContent: String) {
        if (!isInitialized) return

        _uiState.update {
            it.copy(title = newTitle, content = newContent, lastModified = System.currentTimeMillis())
        }

        // Debounce Logic
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(750) // Debounce delay
            saveToDb()
        }
    }

    fun saveNow() {
        saveJob?.cancel() // Cancel pending debounce
        viewModelScope.launch {
            saveToDb()
        }
    }

    private suspend fun saveToDb() {
        val current = currentNode ?: return
        val state = _uiState.value

        // Don't save empty new notes if they haven't been touched?
        // Actually, user said: "Use a Job (Coroutine). Cancel the previous job, delay for 750ms, then launch dao.insert()."
        // Also "Back Press: Ensure the final state is saved immediately".
        // Use case: Open new note -> Type "F" -> Back. Should save.
        // Use case: Open new note -> Don't type -> Back. Should probably not save if empty? 
        // Logic: if title and content are empty, maybe skip?
        // But for now, let's just save whatever is in state to be safe and avoiding data loss.
        
        // Update the entity
        val updatedNode = current.copy(
            title = state.title,
            content = state.content,
            creationDate = state.lastModified // or update a modification timestamp if we had one
        )
        
        currentNode = updatedNode
        nodeDao.insert(updatedNode)
    }
}
