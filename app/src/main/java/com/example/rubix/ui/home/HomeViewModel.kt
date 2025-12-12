package com.example.rubix.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rubix.data.local.NodeDao
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.data.local.NodeType
import com.example.rubix.domain.repository.IFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nodeDao: NodeDao,
    private val fileRepository: IFileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val folderId: String? = savedStateHandle["folderId"]

    val nodes = nodeDao.getAllInFolder(folderId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
                // Repository uses Dispatchers.IO
                val node = fileRepository.ingestImage(uri).copy(parentId = folderId)
                nodeDao.insert(node)
                Log.d("HomeViewModel", "Imported image: ${node.id} to folder: $folderId")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error importing image", e)
            }
        }
    }
    
    // Future proofing for PDF support if added to UI later
    fun importPdf(uri: Uri) {
         viewModelScope.launch {
            try {
                // Repository uses Dispatchers.IO
                val node = fileRepository.ingestPdf(uri).copy(parentId = folderId)
                nodeDao.insert(node)
                Log.d("HomeViewModel", "Imported PDF: ${node.id} to folder: $folderId")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error importing PDF", e)
            }
        }
    }
    
    // Generic file import (handles any file type)
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            try {
                // Try to import as image first, fall back to PDF handler
                val node = try {
                    fileRepository.ingestImage(uri).copy(parentId = folderId)
                } catch (e: Exception) {
                    // If not an image, try as PDF
                    fileRepository.ingestPdf(uri).copy(parentId = folderId)
                }
                nodeDao.insert(node)
                Log.d("HomeViewModel", "Imported file: ${node.id} to folder: $folderId")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error importing file", e)
            }
        }
    }
    
    /**
     * Reorders nodes after drag-and-drop.
     * Calculates new sortOrder for the moved item based on its neighbors.
     */
    fun reorderNodes(fromIndex: Int, toIndex: Int) {
        val currentNodes = nodes.value.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentNodes.size || 
            toIndex < 0 || toIndex >= currentNodes.size) return
            
        val movedNode = currentNodes[fromIndex]
        currentNodes.removeAt(fromIndex)
        currentNodes.add(toIndex, movedNode)
        
        // Calculate new sortOrder values
        // Use timestamp-based ordering with gaps to allow future insertions
        val updates = mutableListOf<Pair<String, Long>>()
        val baseTime = System.currentTimeMillis()
        
        currentNodes.forEachIndexed { index, node ->
            // Space items 1000ms apart for easy future insertions
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
}

