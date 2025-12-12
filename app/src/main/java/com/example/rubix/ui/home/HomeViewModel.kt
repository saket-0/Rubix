package com.example.rubix.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rubix.data.local.NodeDao
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.data.local.NodeType
import com.example.rubix.data.repository.NodeRepository
import com.example.rubix.domain.repository.IFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nodeDao: NodeDao,
    private val nodeRepository: NodeRepository,
    private val fileRepository: IFileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val folderId: String? = savedStateHandle["folderId"]
    
    // Breadcrumb path from root to current folder's parent
    private val _breadcrumbPath = MutableStateFlow<List<NodeEntity>>(emptyList())
    val breadcrumbPath: StateFlow<List<NodeEntity>> = _breadcrumbPath.asStateFlow()
    
    // Current folder info (for displaying current folder name)
    private val _currentFolder = MutableStateFlow<NodeEntity?>(null)
    val currentFolder: StateFlow<NodeEntity?> = _currentFolder.asStateFlow()

    val nodes = nodeDao.getAllInFolder(folderId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        loadBreadcrumbs()
    }
    
    private fun loadBreadcrumbs() {
        viewModelScope.launch {
            if (folderId != null) {
                _breadcrumbPath.value = nodeRepository.getAncestors(folderId)
                _currentFolder.value = nodeRepository.getNodeById(folderId)
            } else {
                _breadcrumbPath.value = emptyList()
                _currentFolder.value = null
            }
        }
    }
    
    /**
     * Moves an item to a new parent folder.
     * Uses optimistic UI - the item will disappear from the current view
     * as the folder's Flow will automatically update.
     */
    fun moveItem(itemId: String, targetFolderId: String?) {
        // Don't allow moving to the same folder
        if (targetFolderId == folderId) return
        
        // Don't allow moving a folder into itself
        if (itemId == targetFolderId) return
        
        viewModelScope.launch {
            try {
                nodeRepository.moveNode(itemId, targetFolderId)
                Log.d("HomeViewModel", "Moved item $itemId to folder: $targetFolderId")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error moving item", e)
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val node = NodeEntity(
                type = NodeType.FOLDER,
                title = name,
                parentId = folderId
            )
            nodeDao.insert(node)
        }
    }

    fun importImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val node = fileRepository.ingestImage(uri).copy(parentId = folderId)
                nodeDao.insert(node)
                Log.d("HomeViewModel", "Imported image: ${node.id} to folder: $folderId")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error importing image", e)
            }
        }
    }
    
    fun importPdf(uri: Uri) {
         viewModelScope.launch {
            try {
                val node = fileRepository.ingestPdf(uri).copy(parentId = folderId)
                nodeDao.insert(node)
                Log.d("HomeViewModel", "Imported PDF: ${node.id} to folder: $folderId")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error importing PDF", e)
            }
        }
    }
    
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val node = try {
                    fileRepository.ingestImage(uri).copy(parentId = folderId)
                } catch (e: Exception) {
                    fileRepository.ingestPdf(uri).copy(parentId = folderId)
                }
                nodeDao.insert(node)
                Log.d("HomeViewModel", "Imported file: ${node.id} to folder: $folderId")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error importing file", e)
            }
        }
    }
    
    fun reorderNodes(fromIndex: Int, toIndex: Int) {
        val currentNodes = nodes.value.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentNodes.size || 
            toIndex < 0 || toIndex >= currentNodes.size) return
            
        val movedNode = currentNodes[fromIndex]
        currentNodes.removeAt(fromIndex)
        currentNodes.add(toIndex, movedNode)
        
        val updates = mutableListOf<Pair<String, Long>>()
        val baseTime = System.currentTimeMillis()
        
        currentNodes.forEachIndexed { index, node ->
            val newSortOrder = baseTime + (index * 1000L)
            if (node.sortOrder != newSortOrder) {
                updates.add(node.id to newSortOrder)
            }
        }
        
        if (updates.isNotEmpty()) {
            viewModelScope.launch {
                nodeDao.updateSortOrders(updates)
                Log.d("HomeViewModel", "Reordered ${updates.size} nodes")
            }
        }
    }
    
    // Lifecycle actions
    fun togglePin(nodeId: String) {
        viewModelScope.launch {
            nodeDao.togglePin(nodeId)
            Log.d("HomeViewModel", "Toggled pin for: $nodeId")
        }
    }
    
    fun moveToArchive(nodeId: String) {
        viewModelScope.launch {
            nodeDao.moveToArchive(nodeId)
            Log.d("HomeViewModel", "Archived: $nodeId")
        }
    }
    
    fun moveToTrash(nodeId: String) {
        viewModelScope.launch {
            nodeDao.moveToTrash(nodeId)
            Log.d("HomeViewModel", "Trashed: $nodeId")
        }
    }
}
