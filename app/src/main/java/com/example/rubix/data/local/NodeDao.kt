package com.example.rubix.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
    @Query("SELECT * FROM nodes WHERE (:parentId IS NULL AND parentId IS NULL) OR (parentId = :parentId) ORDER BY creationDate DESC")
    fun getAllInFolder(parentId: String?): Flow<List<NodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: NodeEntity)

    @Delete
    suspend fun delete(node: NodeEntity)

    @Query("SELECT * FROM nodes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY creationDate DESC")
    suspend fun search(query: String): List<NodeEntity>

    @Query("SELECT * FROM nodes WHERE id = :id")
    fun getNode(id: String): Flow<NodeEntity?>
}
