package com.example.rubix.data.repository

import com.example.rubix.data.local.NodeDao
import com.example.rubix.data.local.NodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for node hierarchy operations.
 * Handles ancestry resolution for breadcrumbs and node movement.
 */
@Singleton
class NodeRepository @Inject constructor(
    private val nodeDao: NodeDao
) {
    /**
     * Gets all ancestors of a node, ordered from root to immediate parent.
     * Does NOT include the node itself.
     */
    suspend fun getAncestors(startNodeId: String): List<NodeEntity> = withContext(Dispatchers.IO) {
        val ancestors = mutableListOf<NodeEntity>()
        
        // First, get the starting node to find its parentId
        val startNode = nodeDao.getNodeById(startNodeId)
        var currentParentId: String? = startNode?.parentId
        
        // Walk up the tree until we hit root (null parent)
        while (currentParentId != null) {
            val parentNode = nodeDao.getNodeById(currentParentId) ?: break
            ancestors.add(0, parentNode)  // Prepend for root-first order
            currentParentId = parentNode.parentId
        }
        
        ancestors
    }

    /**
     * Moves a node to a new parent folder.
     * @param nodeId The ID of the node to move
     * @param newParentId The ID of the target folder, or null for root
     */
    suspend fun moveNode(nodeId: String, newParentId: String?) = withContext(Dispatchers.IO) {
        nodeDao.updateParentId(nodeId, newParentId)
    }
    
    /**
     * Gets a single node by ID.
     */
    suspend fun getNodeById(id: String): NodeEntity? = withContext(Dispatchers.IO) {
        nodeDao.getNodeById(id)
    }
}
