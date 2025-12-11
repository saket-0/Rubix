package com.example.rubix.di

import com.example.rubix.data.repository.FileRepository
import com.example.rubix.domain.repository.IFileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        fileRepository: FileRepository
    ): IFileRepository
}
