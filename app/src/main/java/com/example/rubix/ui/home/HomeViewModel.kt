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
}
