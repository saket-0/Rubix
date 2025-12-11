package com.example.rubix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NodeEntity::class], version = 2, exportSchema = false)
abstract class RubixDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao
}

