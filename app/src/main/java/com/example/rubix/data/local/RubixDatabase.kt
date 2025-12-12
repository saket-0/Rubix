package com.example.rubix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NodeEntity::class], version = 3, exportSchema = false)
abstract class RubixDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao
}

