package com.example.rubix.domain.repository

import android.net.Uri
import com.example.rubix.data.local.NodeEntity

interface IFileRepository {
    suspend fun ingestImage(uri: Uri): NodeEntity
    suspend fun ingestPdf(uri: Uri): NodeEntity
}
