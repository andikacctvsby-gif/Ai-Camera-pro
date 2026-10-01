package com.example.ui.editor

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CropRatio
import com.example.model.FilterPreset
import com.example.ui.components.FilterPreviewCard
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorTab
import com.example.viewmodel.EditorViewModel

import androidx.compose.material.icons.filled.Mic
import com.example.ui.components.VoiceCommandDialog

@Composable
fun PhotoEditorScreen(
    viewModel: EditorViewModel,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originalBitmap by viewModel.originalBitmap.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val isRendering by viewModel.isRendering.collectAsStateWithLifecycle()
    val showCompareOriginal by viewModel.showCompareOriginal.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var showVoiceDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
            // Top Toolbar: Auto-Enhance, Voice AI, Reset, Compare, Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Magic Auto-Enhance Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x337C4DFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent),
                        modifier = Modifier
                            .clickable { viewModel.applyAutoEnhance() }
                            .testTag("btn_auto_enhance")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto-Tuning",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                        }
                    }

                    // Voice AI Command Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x3300E5FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                        modifier = Modifier
                            .clickable { showVoiceDialog = true }
                            .testTag("btn_voice_command")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice AI",
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voice AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset
                    IconButton(
                        onClick = { viewModel.resetAdjustments() },
                        modifier = Modifier.testTag("btn_reset_editor")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Save
                    Button(
                        onClick = {
                            viewModel.saveEditedPhoto { onNavigateToGallery() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("btn_save_edited_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Image Preview Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161B22))
                    .testTag("editor_canvas_container"),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = if (showCompareOriginal) originalBitmap else previewBitmap

                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Preview Foto",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Compare button (press to see original)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (showCompareOriginal) CyanNeon else Color(0xCC131822),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable { viewModel.setShowCompareOriginal(!showCompareOriginal) }
                        .testTag("btn_editor_compare")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = null,
                            tint = if (showCompareOriginal) DarkBackground else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCompareOriginal) "Asli" else "Bandingkan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showCompareOriginal) DarkBackground else Color.White
                        )
                    }
                }
            }

            // Navigation Tabs for Editor Tools
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = DarkSurface,
                contentColor = CyanNeon,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                        color = CyanNeon,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTab == EditorTab.FILTERS,
                    onClick = { viewModel.setTab(EditorTab.FILTERS) },
                    text = { Text("Filter", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_filter")
                )
                Tab(
                    selected = currentTab == EditorTab.ADJUST,
                    onClick = { viewModel.setTab(EditorTab.ADJUST) },
                    text = { Text("Penyuntingan", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_adjust")
                )
                Tab(
                    selected = currentTab == EditorTab.CROP_TRANSFORM,
                    onClick = { viewModel.setTab(EditorTab.CROP_TRANSFORM) },
                    text = { Text("Rasio & Putar", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_crop")
                )
                Tab(
                    selected = currentTab == EditorTab.OVERLAYS,
                    onClick = { viewModel.setTab(EditorTab.OVERLAYS) },
                    text = { Text("Teks & AI", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_overlays")
                )
            }

            // Bottom Tools Control Panel
            Surface(
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .navigationBarsPadding()
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    when (currentTab) {
                        EditorTab.FILTERS -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Filter Intensity Slider (Only visible when a filter is applied)
                                if (editState.activeFilter != FilterPreset.ORIGINAL) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Intensitas Filter: ${editState.filterIntensity.toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanNeon
                                        )
                                    }
                                    Slider(
                                        value = editState.filterIntensity,
                                        onValueChange = { viewModel.updateFilterIntensity(it) },
                                        valueRange = 0f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = CyanNeon,
                                            activeTrackColor = CyanNeon,
                                            inactiveTrackColor = DarkSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                    )
                                }

                                // Filters Carousel
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterPreset.entries.forEach { filter ->
                                        FilterPreviewCard(
                                            filter = filter,
                                            isSelected = editState.activeFilter == filter,
                                            onSelect = { viewModel.setFilter(filter) }
                                        )
                                    }
                                }
                            }
                        }

                        EditorTab.ADJUST -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AdjustmentSlider(
                                    title = "Kecerahan",
                                    value = editState.brightness,
                                    range = -100f..100f,
                                    onValueChange = { viewModel.updateBrightness(it) }
                                )
                                AdjustmentSlider(
                                    title = "Kontras",
                                    value = editState.contrast,
                                    range = -100f..100f,
                                    onValueChange = { viewModel.updateContrast(it) }
                                )
                                AdjustmentSlider(
                                    title = "Saturasi Warna",
                                    value = editState.saturation,
                                    range = -100f..100f,
                                    onValueChange = { viewModel.updateSaturation(it) }
                                )
                                AdjustmentSlider(
                                    title = "Suhu / Kehangatan",
                                    value = editState.warmth,
                                    range = -100f..100f,
                                    onValueChange = { viewModel.updateWarmth(it) }
                                )
                                AdjustmentSlider(
                                    title = "Ketajaman",
                                    value = editState.sharpness,
                                    range = 0f..100f,
                                    onValueChange = { viewModel.updateSharpness(it) }
                                )
                                AdjustmentSlider(
                                    title = "Efek Vignette",
                                    value = editState.vignette,
                                    range = 0f..100f,
                                    onValueChange = { viewModel.updateVignette(it) }
                                )
                                AdjustmentSlider(
                                    title = "Blur Bokeh Latar",
                                    value = editState.bokehBlur,
                                    range = 0f..100f,
                                    onValueChange = { viewModel.updateBokeh(it) }
                                )
                            }
                        }

                        EditorTab.CROP_TRANSFORM -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Rotate & Flip Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.rotate90() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Putar 90°", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.toggleFlipHorizontal() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flip,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Balik Horizontal", fontSize = 12.sp)
                                    }
                                }

                                // Crop Ratio Pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CropRatio.entries.forEach { ratio ->
                                        val isSel = editState.cropRatio == ratio
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSel) CyanNeon.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSel) CyanNeon else Color.White.copy(alpha = 0.15f)
                                            ),
                                            modifier = Modifier.clickable { viewModel.setCropRatio(ratio) }
                                        ) {
                                            Text(
                                                text = ratio.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) CyanNeon else Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        EditorTab.OVERLAYS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Text Watermark Input
                                OutlinedTextField(
                                    value = editState.watermarkText,
                                    onValueChange = { viewModel.setWatermarkText(it) },
                                    label = { Text("Keterangan Foto / Watermark", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanNeon,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // AI Badges
                                Text("Pilih Badge AI:", fontSize = 11.sp, color = TextSecondary)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("AI HD 4K", "PRO CAMERA", "ENHANCED", "HDR MASTER", null).forEach { badge ->
                                        val isSel = editState.selectedBadge == badge
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) CyanNeon.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSel) CyanNeon else Color.White.copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier.clickable { viewModel.setBadge(badge) }
                                        ) {
                                            Text(
                                                text = badge?.let { "✦ $it" } ?: "Tanpa Badge",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) CyanNeon else Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        VoiceCommandDialog(
            isOpen = showVoiceDialog,
            onDismiss = { showVoiceDialog = false },
            onExecuteCommand = { spokenText ->
                viewModel.executeVoiceCommand(spokenText)
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AdjustmentSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
            Text(
                text = "${value.toInt()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CyanNeon,
                activeTrackColor = CyanNeon,
                inactiveTrackColor = DarkSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )
    }
}
