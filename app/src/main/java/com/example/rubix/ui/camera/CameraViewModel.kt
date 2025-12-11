package com.example.rubix.ui.camera

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rubix.data.local.NodeDao
import com.example.rubix.domain.repository.IFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "CameraViewModel"

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val nodeDao: NodeDao,
    private val fileRepository: IFileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val folderId: String? = savedStateHandle["folderId"]
    
    /**
     * Ingests a batch of captured photos into the database.
     * Deletes temporary cache files after successful ingestion.
     */
    fun confirmBatch(uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEach { uri ->
                try {
                    val node = fileRepository.ingestImage(uri).copy(parentId = folderId)
                    nodeDao.insert(node)
                    Log.d(TAG, "Ingested batch photo: ${node.id}")
                    
                    // Delete temp cache file
                    uri.path?.let { path ->
                        val file = File(path)
                        if (file.exists() && file.delete()) {
                            Log.d(TAG, "Deleted temp file: $path")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error ingesting batch photo: $uri", e)
                }
            }
        }
    }
}
