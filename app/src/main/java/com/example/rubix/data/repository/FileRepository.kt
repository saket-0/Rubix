package com.example.rubix.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.rubix.data.local.NodeEntity
import com.example.rubix.data.local.NodeType
import com.example.rubix.domain.repository.IFileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : IFileRepository {

    private val originalsDir = File(context.filesDir, "originals").apply { mkdirs() }
    private val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
    private val previewsDir = File(context.filesDir, "previews").apply { mkdirs() }

    override suspend fun ingestImage(uri: Uri): NodeEntity = withContext(Dispatchers.IO) {
        val uuid = UUID.randomUUID().toString()
        val originalFile = File(originalsDir, "$uuid.jpg")
        val thumbnailFile = File(thumbnailsDir, "$uuid.jpg")
        val previewFile = File(previewsDir, "$uuid.jpg")

        try {
            // Step A (Original): Copy source Uri to filesDir/originals
            copyUriToFile(uri, originalFile)

            // Load Original Bitmap
            val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
                ?: throw IllegalStateException("Failed to decode image from ${originalFile.absolutePath}")

            val width = originalBitmap.width
            val height = originalBitmap.height
            val aspectRatio = width.toFloat() / height.toFloat()

            // Step B (Preview): Resize to max 1080px width
            val previewWidth = 1080
            val previewHeight = (previewWidth / aspectRatio).roundToInt()
            val previewBitmap = Bitmap.createScaledBitmap(originalBitmap, previewWidth, previewHeight, true)
            saveBitmap(previewBitmap, previewFile, 80)
            
            // Release preview bitmap if it's different (it likely is)
            if (previewBitmap != originalBitmap) {
                previewBitmap.recycle()
            }

            // Step C (Thumbnail): Resize to max 300px width
            val thumbnailWidth = 300
            val thumbnailHeight = (thumbnailWidth / aspectRatio).roundToInt()
            val thumbnailBitmap = Bitmap.createScaledBitmap(originalBitmap, thumbnailWidth, thumbnailHeight, true)
            saveBitmap(thumbnailBitmap, thumbnailFile, 70)
            
            if (thumbnailBitmap != originalBitmap) {
                thumbnailBitmap.recycle()
            }

            // Step D (Metadata): Dominant Color (Center Pixel)
            val centerPixel = originalBitmap.getPixel(width / 2, height / 2)
            
            // We can recycle the original bitmap now
            originalBitmap.recycle()

            return@withContext NodeEntity(
                id = uuid,
                type = NodeType.IMAGE,
                title = "Image_$uuid", // Default title
                aspectRatio = aspectRatio,
                dominantColor = centerPixel,
                originalPath = originalFile.absolutePath,
                previewPath = previewFile.absolutePath,
                thumbnailPath = thumbnailFile.absolutePath
            )

        } catch (e: Exception) {
            // Cleanup partial files
            originalFile.delete()
            thumbnailFile.delete()
            previewFile.delete()
            throw e
        }
    }

    override suspend fun ingestPdf(uri: Uri): NodeEntity = withContext(Dispatchers.IO) {
        val uuid = UUID.randomUUID().toString()
        val originalFile = File(originalsDir, "$uuid.pdf")
        val thumbnailFile = File(thumbnailsDir, "$uuid.jpg")

        try {
            // Step A: Copy original PDF
            copyUriToFile(uri, originalFile)

            // Step B: Render Page 0
            val parcelFileDescriptor = ParcelFileDescriptor.open(originalFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val width = page.width
                val height = page.height
                val aspectRatio = width.toFloat() / height.toFloat()
                
                // create bitmap for thumbnail (scale straight to thumbnail size if possible, 
                // but PdfRenderer renders to bitmap size)
                // Let's render nicely then scale down or render at target size?
                // PdfRenderer renders to the bitmap size. So let's create a bitmap of width 300
                val targetWidth = 300
                val targetHeight = (targetWidth / aspectRatio).roundToInt()
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pdfRenderer.close()
                parcelFileDescriptor.close()

                // Save thumbnail
                saveBitmap(bitmap, thumbnailFile, 70)
                bitmap.recycle()

                return@withContext NodeEntity(
                    id = uuid,
                    type = NodeType.PDF,
                    title = "PDF_$uuid",
                    aspectRatio = aspectRatio,
                    originalPath = originalFile.absolutePath,
                    thumbnailPath = thumbnailFile.absolutePath
                    // No preview for PDF yet (just thumbnail as per requirements)
                )
            } else {
                pdfRenderer.close()
                parcelFileDescriptor.close()
                throw IllegalStateException("PDF has 0 pages")
            }

        } catch (e: Exception) {
             originalFile.delete()
             thumbnailFile.delete()
             throw e
        }
    }

    private fun copyUriToFile(uri: Uri, destFile: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Could not open input stream for Uri: $uri")
    }

    private fun saveBitmap(bitmap: Bitmap, file: File, quality: Int) {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }
    }
}
