package com.example.rubix.ui.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

private const val TAG = "CameraScreen"

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember { mutableStateOf(false) }
    val capturedUris = remember { mutableStateListOf<Uri>() }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // Request permission on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraContent(
                context = context,
                lifecycleOwner = lifecycleOwner,
                capturedUris = capturedUris,
                onPhotoTaken = { uri ->
                    capturedUris.add(uri)
                },
                onDone = {
                    viewModel.confirmBatch(capturedUris.toList())
                    navController.popBackStack()
                },
                onClose = {
                    // Clean up temp files on cancel
                    capturedUris.forEach { uri ->
                        try {
                            File(uri.path ?: "").delete()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to delete temp file", e)
                        }
                    }
                    navController.popBackStack()
                }
            )
        } else {
            // Permission denied state
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera permission required",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun CameraContent(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    capturedUris: MutableList<Uri>,
    onPhotoTaken: (Uri) -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit
) {
    val executor: Executor = ContextCompat.getMainExecutor(context)
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    
    // Shutter animation
    val shutterScale by animateFloatAsState(
        targetValue = if (isCapturing) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "shutter_scale"
    )
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar with photo count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (capturedUris.isNotEmpty()) {
                Text(
                    text = "${capturedUris.size} photo${if (capturedUris.size > 1) "s" else ""}",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        
        // Camera Preview (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                        
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Camera binding failed", e)
                        }
                    }, executor)
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Preview Strip (LazyRow of captured photos)
        if (capturedUris.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = capturedUris.size) { index ->
                    val uri = capturedUris[index]
                    Box(
                        modifier = Modifier.size(60.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Captured photo $index",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        )
                        
                        // Delete button on each thumbnail
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .clickable {
                                    // Delete the file and remove from list
                                    try {
                                        File(uri.path ?: "").delete()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to delete", e)
                                    }
                                    capturedUris.removeAt(index)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom bar with controls: Exit (left) - Shutter (center) - Done (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exit button (left)
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Shutter button (center)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX = shutterScale
                        scaleY = shutterScale
                    }
                    .background(Color.Transparent, CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable(enabled = !isCapturing) {
                        isCapturing = true
                        val photoFile = createTempImageFile(context)
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        
                        imageCapture?.takePicture(
                            outputOptions,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val savedUri = Uri.fromFile(photoFile)
                                    onPhotoTaken(savedUri)
                                    isCapturing = false
                                    Log.d(TAG, "Photo saved: $savedUri")
                                }
                                
                                override fun onError(exception: ImageCaptureException) {
                                    Log.e(TAG, "Photo capture failed", exception)
                                    isCapturing = false
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color.White, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Done button (right)
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (capturedUris.isNotEmpty()) {
                    IconButton(
                        onClick = onDone,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Done",
                            tint = Color.White
                        )
                    }
                } else {
                    // Placeholder to maintain spacing
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
    
    // Cleanup camera on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbind camera", e)
            }
        }
    }
}

private fun createTempImageFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
    val fileName = "RUBIX_$timestamp.jpg"
    return File(context.cacheDir, fileName)
}
