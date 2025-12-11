package com.example.rubix.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rubix.data.local.NodeDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    nodeDao: NodeDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val nodeId: String = checkNotNull(savedStateHandle["nodeId"])
    
    val node = nodeDao.getNode(nodeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
