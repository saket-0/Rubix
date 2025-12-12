package com.example.rubix.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rubix.data.local.NodeDao
import com.example.rubix.data.local.NodeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val nodeDao: NodeDao
) : ViewModel() {
    
    val archivedNodes: StateFlow<List<NodeEntity>> = nodeDao.getArchived()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun unarchive(nodeId: String) {
        viewModelScope.launch {
            nodeDao.restore(nodeId)
        }
    }
    
    fun moveToTrash(nodeId: String) {
        viewModelScope.launch {
            nodeDao.moveToTrash(nodeId)
        }
    }
}
