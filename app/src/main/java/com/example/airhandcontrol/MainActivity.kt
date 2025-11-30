package com.example.airhandcontrol

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraSelector
import android.util.Log
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Log.w(TAG, "Camera permission not granted")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val handProcessor = remember { HandProcessor(ctx) }

                    var landmarks by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
                    // smoothing state for pointer
                    var smoothX by remember { mutableStateOf(0f) }
                    var smoothY by remember { mutableStateOf(0f) }
                    var prevPinch by remember { mutableStateOf(false) }
                    var pinchStartTime by remember { mutableStateOf(0L) }

                    // Analyzer: receives Bitmaps from camera preview
                    CameraPreviewView(onAnalyzeBitmap = { bmp ->
                        // Run inference off the UI thread
                        CoroutineScope(Dispatchers.Default).launch {
                            try {
                                val res = handProcessor.estimateLandmarks(bmp)
                                if (res != null && res.landmarks.size >= 9) {
                                    // Convert to normalized pairs (x,y)
                                    val pts = res.landmarks.map { Pair(it.x, it.y) }
                                    // update overlay on UI
                                    kotlinx.coroutines.Dispatchers.Main.let {
                                        CoroutineScope(it).launch {
                                            landmarks = pts
                                        }
                                    }

                                    // Use index finger tip (landmark 8) for pointer
                                    val idx = res.landmarks[8]
                                    val nx = idx.x.coerceIn(0f, 1f)
                                    val ny = idx.y.coerceIn(0f, 1f)

                                    // smoothing
                                    val alpha = 0.25f
                                    smoothX = smoothX * (1 - alpha) + nx * alpha
                                    smoothY = smoothY * (1 - alpha) + ny * alpha

                                    // dispatch move to accessibility service
                                    AirHandServiceHolder.instance?.movePointerNormalized(smoothX, smoothY)

                                    // Pinch detection: thumb tip (4) vs index tip (8)
                                    val thumb = res.landmarks[4]
                                    val idxTip = idx
                                    val dx = thumb.x - idxTip.x
                                    val dy = thumb.y - idxTip.y
                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                    val pinchThreshold = 0.06f // tune this on-device

                                    if (dist < pinchThreshold) {
                                        if (!prevPinch) {
                                            prevPinch = true
                                            pinchStartTime = System.currentTimeMillis()
                                        } else {
                                            // still pinching
                                        }
                                        // during pinch, emulate drag by continuing to move pointer
                                    } else {
                                        // pinch released
                                        if (prevPinch) {
                                            val duration = System.currentTimeMillis() - pinchStartTime
                                            if (duration < 300) {
                                                // treat as click
                                                AirHandServiceHolder.instance?.clickAtNormalized(nx, ny)
                                            } else {
                                                // end of drag emulation - send a final click to release
                                                AirHandServiceHolder.instance?.clickAtNormalized(nx, ny)
                                            }
                                        }
                                        prevPinch = false
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("HandInfer", "Inference failed", e)
                            }
                        }
                    })

                    // Overlay and controls
                    HandOverlay(landmarks = landmarks)
                    var showTrainer by remember { mutableStateOf(false) }
                    Controls(onOpenTrainer = { showTrainer = true })

                    if (showTrainer) {
                        val ctx2 = LocalContext.current
                        val store = remember { HandSampleStore(ctx2) }
                        var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        var label by remember { mutableStateOf("") }

                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                            if (uri != null) {
                                // load bitmap
                                val stream = ctx2.contentResolver.openInputStream(uri)
                                val bmp = BitmapFactory.decodeStream(stream)
                                stream?.close()
                                pickedBitmap = bmp
                            }
                        }

                        Surface(modifier = Modifier.padding(12.dp)) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Button(onClick = { launcher.launch("image/*") }) { Text("Pick Image") }
                                if (pickedBitmap != null) {
                                    Image(bitmap = pickedBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.size(160.dp))
                                    BasicTextField(value = label, onValueChange = { label = it }, modifier = Modifier.padding(4.dp))
                                    Button(onClick = {
                                        // run inference and save sample
                                        CoroutineScope(Dispatchers.Default).launch {
                                            val hp = HandProcessor(ctx2)
                                            val out = pickedBitmap?.let { hp.estimateLandmarks(it) }
                                            if (out != null) {
                                                store.saveSample(label.ifBlank { "sample" }, out.landmarks)
                                            }
                                            hp.close()
                                        }
                                    }) { Text("Save Sample") }
                                }
                                Button(onClick = { showTrainer = false }) { Text("Close") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Controls(onOpenTrainer: () -> Unit) {
    var enabled by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.padding(12.dp)) {
        Button(onClick = {
            // open Accessibility Settings to enable service
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            enabled = true
        }) {
            Text("Open Accessibility Settings")
        }
        Button(onClick = onOpenTrainer) {
            Text("Open Trainer")
        }
    }
}

@Composable
fun CameraPreviewView(onAnalyzeBitmap: (Bitmap) -> Unit) {
    AndroidView(factory = { context ->
        val previewView = PreviewView(context)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(context as ComponentActivity, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera bind failed", e)
            }

        }, ContextCompat.getMainExecutor(context))

        // Start a coroutine that polls the previewView bitmap and sends it to analyzer
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            while (true) {
                try {
                    val bmp = previewView.bitmap
                    if (bmp != null) {
                        onAnalyzeBitmap(bmp)
                    }
                } catch (_: Exception) {
                }
                delay(100) // sample ~10 FPS
            }
        }

        previewView
    })
}
