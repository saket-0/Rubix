package com.example.rubix.di

import android.content.Context
import androidx.room.Room
import com.example.rubix.data.local.NodeDao
import com.example.rubix.data.local.RubixDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RubixDatabase {
        return Room.databaseBuilder(
            context,
            RubixDatabase::class.java,
            "rubix_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideNodeDao(database: RubixDatabase): NodeDao {
        return database.nodeDao()
    }
}
