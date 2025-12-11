package com.example.rubix.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewer(pdfPath: String) {
    var pdfBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(pdfPath) {
        withContext(Dispatchers.IO) {
            val file = File(pdfPath)
            if (!file.exists()) return@withContext

            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount
            val bitmaps = mutableListOf<Bitmap>()

            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                // Render at high quality (e.g. 2x screen density or just full page width)
                // For now use hardcoded width 1080 for speed demonstration
                val width = 1080 
                val height = (width.toFloat() / page.width.toFloat() * page.height.toFloat()).toInt()
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            
            pdfRenderer.close()
            fileDescriptor.close()
            pdfBitmaps = bitmaps
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(pdfBitmaps) { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            )
        }
    }
}
