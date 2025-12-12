package com.example.rubix.ui.camera

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.rubix.data.local.NodeDao
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.data.local.NodeType
import com.example.rubix.domain.repository.IFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
     * This is a suspend function that completes AFTER all images are saved.
     * If batch size > 1, creates a folder to group them.
     * Deletes temporary cache files after successful ingestion.
     */
    suspend fun confirmBatch(uris: List<Uri>) {
        if (uris.isEmpty()) return
        
        // Create a folder if batch has more than 1 image
        val targetParentId = if (uris.size > 1) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val folderName = "Batch $timestamp"
            val folderNode = NodeEntity(
                type = NodeType.FOLDER,
                title = folderName,
                parentId = folderId
            )
            nodeDao.insert(folderNode)
            Log.d(TAG, "Created batch folder: ${folderNode.id} - $folderName")
            folderNode.id
        } else {
            folderId
        }
        
        // Process all images - this runs synchronously in the calling coroutine
        uris.forEach { uri ->
            try {
                val node = fileRepository.ingestImage(uri).copy(parentId = targetParentId)
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
        
        Log.d(TAG, "Batch ingestion complete: ${uris.size} photos")
    }
}
