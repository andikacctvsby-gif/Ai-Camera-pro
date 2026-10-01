package com.example.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.DetectionCategory
import com.example.ui.components.BottomShutterBar
import com.example.ui.components.CameraGridLines
import com.example.ui.components.CameraZoomBar
import com.example.ui.components.DeepScanDialog
import com.example.ui.components.DetectionCategorySelectorBar
import com.example.ui.components.ObjectBoundingBoxOverlay
import com.example.ui.components.TopCameraBar
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToMagicStudio: (Bitmap) -> Unit,
    onNavigateToEditor: (Bitmap) -> Unit,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            viewModel.setSimulationMode(true)
        }
    }

    val detectedObjects by viewModel.detectedObjects.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val isGridEnabled by viewModel.isGridEnabled.collectAsStateWithLifecycle()
    val isDetectionEnabled by viewModel.isDetectionEnabled.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.timerSeconds.collectAsStateWithLifecycle()
    val timerCountdown by viewModel.timerCountdown.collectAsStateWithLifecycle()
    val isCapturing by viewModel.isCapturing.collectAsStateWithLifecycle()
    val deepScanResult by viewModel.deepScanResult.collectAsStateWithLifecycle()
    val isDeepScanning by viewModel.isDeepScanning.collectAsStateWithLifecycle()
    val isSimulationMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var shutterFlashActive by remember { mutableStateOf(false) }

    // Navigation events listener
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is com.example.viewmodel.CameraNavigationEvent.OpenMagicStudio -> {
                    onNavigateToMagicStudio(event.bitmap)
                }
                is com.example.viewmodel.CameraNavigationEvent.OpenEditor -> {
                    onNavigateToEditor(event.bitmap)
                }
                else -> {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (hasCameraPermission && !isSimulationMode) {
            // Live CameraX Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                        viewModel.cameraManager.startCamera(lifecycleOwner, this)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview_view")
            )
        } else {
            // Fallback / Simulation Scene
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.sample_portrait),
                    contentDescription = "Simulasi Kamera AI",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Simulation indicator banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xDD090C10),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(CyanNeon, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode Simulasi Kamera Aktif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )
                    }
                }
            }
        }

        // 3x3 Composition Grid Overlay
        if (isGridEnabled) {
            CameraGridLines(modifier = Modifier.fillMaxSize())
        }

        // Real-Time Object Bounding Box Overlay
        if (isDetectionEnabled && detectedObjects.isNotEmpty()) {
            ObjectBoundingBoxOverlay(
                detectedObjects = detectedObjects,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Camera HUD
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            TopCameraBar(
                flashMode = flashMode,
                isTorchOn = isTorchOn,
                isGridEnabled = isGridEnabled,
                isDetectionEnabled = isDetectionEnabled,
                timerSeconds = timerSeconds,
                onToggleFlash = { viewModel.toggleFlash() },
                onToggleTorch = { viewModel.toggleTorch() },
                onToggleGrid = { viewModel.toggleGrid() },
                onToggleDetection = { viewModel.toggleDetection() },
                onCycleTimer = { viewModel.cycleTimer() }
            )

            // Category Targeted Detection Selector
            DetectionCategorySelectorBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { cat -> viewModel.setCategory(cat) },
                modifier = Modifier.fillMaxWidth()
            )

            // Category Guidance Tip Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xB3131822),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(selectedCategory.colorHex),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedCategory.guidance,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Zoom Bar
        CameraZoomBar(
            zoomRatio = zoomRatio,
            onZoomChange = { zoom -> viewModel.setZoom(zoom) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )

        // Bottom Shutter & Action Bar
        BottomShutterBar(
            isCapturing = isCapturing,
            onShutterClick = {
                shutterFlashActive = true
                viewModel.triggerCapture { capturedBitmap ->
                    shutterFlashActive = false
                    // Quick modal or transition
                }
            },
            onSwitchCamera = {
                previewViewRef?.let {
                    viewModel.cameraManager.switchCamera(lifecycleOwner, it)
                }
            },
            onOpenGallery = onNavigateToGallery,
            onDeepScanClick = {
                viewModel.triggerDeepScan(null)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        )

        // Shutter White Flash Effect
        AnimatedVisibility(
            visible = shutterFlashActive,
            enter = fadeIn(animationSpec = tween(50)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // Timer Countdown Overlay
        timerCountdown?.let { count ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    fontSize = 84.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyanNeon
                )
            }
        }

        // Deep Vision Scan Dialog
        DeepScanDialog(
            result = deepScanResult,
            isLoading = isDeepScanning,
            onDismiss = { viewModel.dismissDeepScan() },
            onOpenInMagicStudio = {
                viewModel.lastCapturedBitmap.value?.let { onNavigateToMagicStudio(it) }
                    ?: viewModel.getSamplePortraitBitmap()?.let { onNavigateToMagicStudio(it) }
            },
            onOpenInEditor = {
                viewModel.lastCapturedBitmap.value?.let { onNavigateToEditor(it) }
                    ?: viewModel.getSamplePortraitBitmap()?.let { onNavigateToEditor(it) }
            }
        )

        // Permission Request Dialog if not granted and not yet simulated
        if (!hasCameraPermission && !isSimulationMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE090C10))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0x2200E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Akses Kamera Diperlukan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "AI Cam Pro memerlukan izin kamera untuk mendeteksi objek secara real-time dan mengambil gambar beresolusi tinggi.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_grant_camera_permission")
                        ) {
                            Text("Izinkan Akses Kamera", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.setSimulationMode(true) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PurpleAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_try_simulation_mode")
                        ) {
                            Text("Coba Mode Simulasi Cerdas")
                        }
                    }
                }
            }
        }
    }
}
