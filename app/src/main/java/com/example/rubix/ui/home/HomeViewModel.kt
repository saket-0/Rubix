package com.example.rubix.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rubix.data.local.NodeDao
import com.example.rubix.domain.repository.IFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nodeDao: NodeDao,
    private val fileRepository: IFileRepository
) : ViewModel() {

    val nodes = nodeDao.getAllInFolder(null)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun importImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val node = fileRepository.ingestImage(uri)
                nodeDao.insert(node)
                Log.d("HomeViewModel", "Imported image: ${node.id}")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error importing image", e)
            }
        }
    }
    
    // Future proofing for PDF support if added to UI later
    fun importPdf(uri: Uri) {
         viewModelScope.launch {
            try {
                val node = fileRepository.ingestPdf(uri)
                nodeDao.insert(node)
                Log.d("HomeViewModel", "Imported PDF: ${node.id}")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error importing PDF", e)
            }
        }
    }
}
