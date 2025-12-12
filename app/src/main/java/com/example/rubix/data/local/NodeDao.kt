package com.example.rubix.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
    
    // Main feed - only active items (not archived or trashed)
    @Query("""
        SELECT * FROM nodes 
        WHERE ((:parentId IS NULL AND parentId IS NULL) OR (parentId = :parentId))
        AND isArchived = 0 AND isTrashed = 0
        ORDER BY isPinned DESC, sortOrder ASC
    """)
    fun getAllInFolder(parentId: String?): Flow<List<NodeEntity>>
    
    // Archive feed - archived but not trashed
    @Query("""
        SELECT * FROM nodes 
        WHERE isArchived = 1 AND isTrashed = 0
        ORDER BY sortOrder DESC
    """)
    fun getArchived(): Flow<List<NodeEntity>>
    
    // Trash feed - all trashed items
    @Query("""
        SELECT * FROM nodes 
        WHERE isTrashed = 1
        ORDER BY sortOrder DESC
    """)
    fun getTrashed(): Flow<List<NodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: NodeEntity)

    @Delete
    suspend fun delete(node: NodeEntity)

    @Query("""
        SELECT * FROM nodes 
        WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        AND isArchived = 0 AND isTrashed = 0
        ORDER BY creationDate DESC
    """)
    suspend fun search(query: String): List<NodeEntity>

    @Query("SELECT * FROM nodes WHERE id = :id")
    fun getNode(id: String): Flow<NodeEntity?>
    
    @Query("UPDATE nodes SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Long)
    
    @Transaction
    suspend fun updateSortOrders(updates: List<Pair<String, Long>>) {
        updates.forEach { (id, sortOrder) ->
            updateSortOrder(id, sortOrder)
        }
    }
    
    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getNodeById(id: String): NodeEntity?
    
    @Query("UPDATE nodes SET parentId = :newParentId WHERE id = :nodeId")
    suspend fun updateParentId(nodeId: String, newParentId: String?)
    
    // Lifecycle actions
    @Query("UPDATE nodes SET isArchived = 1 WHERE id = :id")
    suspend fun moveToArchive(id: String)
    
    @Query("UPDATE nodes SET isTrashed = 1 WHERE id = :id")
    suspend fun moveToTrash(id: String)
    
    @Query("UPDATE nodes SET isArchived = 0, isTrashed = 0 WHERE id = :id")
    suspend fun restore(id: String)
    
    @Query("DELETE FROM nodes WHERE id = :id")
    suspend fun deleteForever(id: String)
    
    @Query("UPDATE nodes SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: String)
    
    @Query("UPDATE nodes SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean)
}
