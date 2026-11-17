package com.example.jarvis

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.Executors

@Composable
fun CameraScannerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var labels by remember { mutableStateOf(listOf<String>()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050811))) {
        if (hasPermission) {
            val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
            val executor = remember { Executors.newSingleThreadExecutor() }
            val detector = remember {
                ObjectDetection.getClient(
                    ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .build()
                )
            }
            DisposableEffect(lifecycleOwner) {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                var provider: ProcessCameraProvider? = null
                providerFuture.addListener({
                    try {
                        provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage == null) {
                                imageProxy.close()
                            } else {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                detector.process(image)
                                    .addOnSuccessListener { objects ->
                                        labels = objects.mapNotNull { item ->
                                            val category = item.labels.firstOrNull()?.text
                                            if (category.isNullOrBlank()) "Объект обнаружен" else category
                                        }.distinct().take(5)
                                    }
                                    .addOnFailureListener { labels = listOf("Не удалось распознать объекты") }
                                    .addOnCompleteListener { imageProxy.close() }
                            }
                        }
                        provider?.unbindAll()
                        provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    } catch (_: Exception) {
                        labels = listOf("Камера недоступна на этом устройстве")
                    }
                }, ContextCompat.getMainExecutor(context))
                onDispose {
                    try { provider?.unbindAll() } catch (_: Exception) {}
                    detector.close()
                    executor.shutdown()
                }
            }
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xDD07101D), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("JARVIS VISION / ОБНАРУЖЕНИЕ", color = Color(0xFF00E5FF), fontSize = 12.sp)
                Text(if (labels.isEmpty()) "Наведи камеру на объект" else labels.joinToString("  •  "), color = Color.White, fontSize = 16.sp)
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123C4A))) {
                    Text("ЗАКРЫТЬ КАМЕРУ")
                }
            }
        } else {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Для распознавания объектов JARVIS нужен доступ к камере.", color = Color.White, fontSize = 17.sp)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("РАЗРЕШИТЬ КАМЕРУ") }
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123C4A))) { Text("НАЗАД") }
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA102A38))) { Text("✕") }
        }
    }
}
