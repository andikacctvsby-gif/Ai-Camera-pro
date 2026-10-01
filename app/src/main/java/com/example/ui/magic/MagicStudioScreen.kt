package com.example.ui.magic

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.BackgroundOption
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MagicMode
import com.example.viewmodel.MagicStudioViewModel

@Composable
fun MagicStudioScreen(
    viewModel: MagicStudioViewModel,
    onNavigateToEditor: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val originalBitmap by viewModel.originalBitmap.collectAsStateWithLifecycle()
    val cutoutBitmap by viewModel.cutoutBitmap.collectAsStateWithLifecycle()
    val compositedBitmap by viewModel.compositedBitmap.collectAsStateWithLifecycle()
    val erasedBitmap by viewModel.erasedBitmap.collectAsStateWithLifecycle()
    val maskBitmap by viewModel.maskBitmap.collectAsStateWithLifecycle()
    val selectedBackground by viewModel.selectedBackground.collectAsStateWithLifecycle()
    val brushSize by viewModel.brushSize.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val showOriginalComparison by viewModel.showOriginalComparison.collectAsStateWithLifecycle()
    val enableDropShadow by viewModel.enableDropShadow.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val currentStroke = remember { mutableStateListOf<PointF>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.setBackgroundOption(BackgroundOption.CustomImage(it.toString(), "Galeri"))
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Mode Tabs (Hapus BG vs Hapus Objek)
            TabRow(
                selectedTabIndex = if (currentMode == MagicMode.REMOVE_BG) 0 else 1,
                containerColor = DarkSurface,
                contentColor = CyanNeon,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (currentMode == MagicMode.REMOVE_BG) 0 else 1]),
                        color = CyanNeon,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentMode == MagicMode.REMOVE_BG,
                    onClick = { viewModel.setMode(MagicMode.REMOVE_BG) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus Background", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_remove_bg")
                )

                Tab(
                    selected = currentMode == MagicMode.OBJECT_ERASER,
                    onClick = { viewModel.setMode(MagicMode.OBJECT_ERASER) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus Objek", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_object_eraser")
                )
            }

            // Main Interactive Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161B22))
                    .testTag("magic_canvas_container"),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = when {
                    showOriginalComparison -> originalBitmap
                    currentMode == MagicMode.REMOVE_BG -> compositedBitmap ?: cutoutBitmap ?: originalBitmap
                    else -> erasedBitmap ?: originalBitmap
                }

                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Canvas Foto Magic",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Touch Mask Drawing Layer (Only active in Object Eraser mode)
                    if (currentMode == MagicMode.OBJECT_ERASER && !showOriginalComparison) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStroke.clear()
                                            currentStroke.add(PointF(offset.x, offset.y))
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val pos = change.position
                                            currentStroke.add(PointF(pos.x, pos.y))
                                        },
                                        onDragEnd = {
                                            viewModel.addMaskStroke(currentStroke.toList())
                                            currentStroke.clear()
                                        }
                                    )
                                }
                                .testTag("eraser_touch_canvas")
                        ) {
                            // Render active drag stroke
                            if (currentStroke.size > 1) {
                                for (i in 0 until currentStroke.size - 1) {
                                    val p1 = currentStroke[i]
                                    val p2 = currentStroke[i + 1]
                                    drawLine(
                                        color = Color.Red.copy(alpha = 0.65f),
                                        start = Offset(p1.x, p1.y),
                                        end = Offset(p2.x, p2.y),
                                        strokeWidth = brushSize
                                    )
                                }
                            }
                        }
                    }
                }

                // Processing Indicator
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99090C10)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyanNeon)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "AI sedang memproses...",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // "Bandingkan Asli" Floating Pill Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (showOriginalComparison) CyanNeon else Color(0xCC131822),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable { viewModel.setShowComparison(!showOriginalComparison) }
                        .testTag("btn_compare_original")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = null,
                            tint = if (showOriginalComparison) DarkBackground else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showOriginalComparison) "Mode Asli" else "Bandingkan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showOriginalComparison) DarkBackground else Color.White
                        )
                    }
                }
            }

            // Bottom Controls Area
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (currentMode == MagicMode.REMOVE_BG) {
                        // Background Removal Controls
                        Text(
                            text = "Pilih Latar Belakang Baru:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Background Options Carousel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Transparent
                            val isTrans = selectedBackground is BackgroundOption.Transparent
                            BackgroundOptionCard(
                                title = "Transparan",
                                isSelected = isTrans,
                                onClick = { viewModel.setBackgroundOption(BackgroundOption.Transparent) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                )
                            }

                            // 2. Solid Colors
                            val colors = listOf(
                                0xFFFFFFFF to "Putih",
                                0xFF1C2230 to "Studio",
                                0xFF00E5FF to "Cyan",
                                0xFFFF4081 to "Pink",
                                0xFFFF9100 to "Oranye",
                                0xFF00E676 to "Emerald"
                            )
                            colors.forEach { (c, name) ->
                                val isSel = (selectedBackground as? BackgroundOption.SolidColor)?.color == c
                                BackgroundOptionCard(
                                    title = name,
                                    isSelected = isSel,
                                    onClick = { viewModel.setBackgroundOption(BackgroundOption.SolidColor(c, name)) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(c), CircleShape)
                                    )
                                }
                            }

                            // 3. Gradients
                            val gradients = listOf(
                                (0xFFFF9100 to 0xFFFF4081) to "Sunset",
                                (0xFF7C4DFF to 0xFF00E5FF) to "Cyber",
                                (0xFFFFD700 to 0xFFFF6D00) to "Gold"
                            )
                            gradients.forEach { (pair, name) ->
                                val (c1, c2) = pair
                                val isSel = (selectedBackground as? BackgroundOption.Gradient)?.startColor == c1
                                BackgroundOptionCard(
                                    title = name,
                                    isSelected = isSel,
                                    onClick = { viewModel.setBackgroundOption(BackgroundOption.Gradient(c1, c2, name)) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                ComposeBrush.linearGradient(listOf(Color(c1), Color(c2))),
                                                CircleShape
                                            )
                                    )
                                }
                            }

                            // 4. Scenic Presets
                            BackgroundOptionCard(
                                title = "Pantai",
                                isSelected = (selectedBackground as? BackgroundOption.PresetScene)?.drawableRes == R.drawable.bg_sunset_beach,
                                onClick = { viewModel.setBackgroundOption(BackgroundOption.PresetScene(R.drawable.bg_sunset_beach, "Pantai")) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(OrangeFlame, CircleShape)
                                )
                            }

                            BackgroundOptionCard(
                                title = "Neon Kota",
                                isSelected = (selectedBackground as? BackgroundOption.PresetScene)?.drawableRes == R.drawable.bg_cyber_neon,
                                onClick = { viewModel.setBackgroundOption(BackgroundOption.PresetScene(R.drawable.bg_cyber_neon, "Neon")) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PurpleAccent, CircleShape)
                                )
                            }

                            // 5. Custom image from gallery
                            BackgroundOptionCard(
                                title = "+ Galeri",
                                isSelected = selectedBackground is BackgroundOption.CustomImage,
                                onClick = { photoPickerLauncher.launch("image/*") }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x3300E5FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Shadow toggle & Re-cutout button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (enableDropShadow) CyanNeon.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (enableDropShadow) CyanNeon else Color.Transparent),
                                modifier = Modifier
                                    .clickable { viewModel.toggleDropShadow() }
                                    .testTag("btn_toggle_shadow")
                            ) {
                                Text(
                                    text = if (enableDropShadow) "✓ Bayangan Halus" else "+ Tambah Bayangan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (enableDropShadow) CyanNeon else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.performAutoCutout() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_re_cutout")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pisahkan Ulang", fontSize = 11.sp)
                            }
                        }
                    } else {
                        // Object Eraser Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ukuran Kuas: ${brushSize.toInt()}px",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )

                            // Reset brush
                            Text(
                                text = "Reset Pilihan",
                                fontSize = 12.sp,
                                color = Color.Red.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.resetEraserMask() }
                                    .testTag("btn_reset_eraser_mask")
                            )
                        }

                        Slider(
                            value = brushSize,
                            onValueChange = { viewModel.setBrushSize(it) },
                            valueRange = 16f..80f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Eraser Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.triggerAutoDetectDistractions() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_auto_detect_distraction")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Deteksi Gangguan", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.executeObjectEraser() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangeFlame,
                                    contentColor = DarkBackground
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_execute_eraser")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hapus Sekarang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Final Export & Workflow Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveResultToGallery { }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_magic_result")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Foto", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.getCurrentOutputBitmap()?.let { onNavigateToEditor(it) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_magic_to_editor")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lanjut Edit Foto", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun BackgroundOptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CyanNeon else Color.White.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .width(64.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
            ) {
                content()
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CyanNeon else Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
