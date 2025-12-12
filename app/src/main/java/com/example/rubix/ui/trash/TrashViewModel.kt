package com.example.rubix.ui.trash

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
class TrashViewModel @Inject constructor(
    private val nodeDao: NodeDao
) : ViewModel() {
    
    val trashedNodes: StateFlow<List<NodeEntity>> = nodeDao.getTrashed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun restore(nodeId: String) {
        viewModelScope.launch {
            nodeDao.restore(nodeId)
        }
    }
    
    fun deleteForever(nodeId: String) {
        viewModelScope.launch {
            nodeDao.deleteForever(nodeId)
        }
    }
}
