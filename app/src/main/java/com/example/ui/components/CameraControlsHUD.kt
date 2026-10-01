package com.example.ui.components

import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextPrimary

@Composable
fun TopCameraBar(
    flashMode: Int,
    isTorchOn: Boolean,
    isGridEnabled: Boolean,
    isDetectionEnabled: Boolean,
    timerSeconds: Int,
    onToggleFlash: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleDetection: () -> Unit,
    onCycleTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xDD090C10), Color(0x00090C10))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Mode Button
        IconButton(
            onClick = onToggleFlash,
            modifier = Modifier.testTag("btn_flash_toggle")
        ) {
            val icon = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                else -> Icons.Default.FlashOff
            }
            val tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) CyanNeon else Color.White
            Icon(
                imageVector = icon,
                contentDescription = "Flash Mode",
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }

        // Timer Cycle Button
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (timerSeconds > 0) PurpleAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .clickable { onCycleTimer() }
                .testTag("btn_timer_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = if (timerSeconds > 0) CyanNeon else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (timerSeconds > 0) "${timerSeconds}s" else "Off",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (timerSeconds > 0) CyanNeon else Color.White
                )
            }
        }

        // HD 4K Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0x99131822),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f)),
            modifier = Modifier.testTag("badge_hd_quality")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CyanNeon, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HD 4K ULTRA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanNeon
                )
            }
        }

        // Grid Toggle
        IconButton(
            onClick = onToggleGrid,
            modifier = Modifier.testTag("btn_grid_toggle")
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = "Garis Kisi",
                tint = if (isGridEnabled) CyanNeon else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Realtime AI Detection Toggle
        IconButton(
            onClick = onToggleDetection,
            modifier = Modifier.testTag("btn_detection_toggle")
        ) {
            Icon(
                imageVector = if (isDetectionEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "AI Detection Toggle",
                tint = if (isDetectionEnabled) CyanNeon else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CameraGridLines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val gridColor = Color.White.copy(alpha = 0.25f)
        val strokeW = 1.dp.toPx()

        // Vertical lines (rule of thirds)
        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = strokeW)
        drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = strokeW)

        // Horizontal lines (rule of thirds)
        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = strokeW)
        drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = strokeW)
    }
}

@Composable
fun DetectionCategorySelectorBar(
    selectedCategory: com.example.model.DetectionCategory,
    onCategorySelected: (com.example.model.DetectionCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(Modifier.clickable(enabled = false) {}),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.model.DetectionCategory.entries.forEach { category ->
                        val isSelected = category == selectedCategory
                        val chipColor = Color(category.colorHex)
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) chipColor.copy(alpha = 0.25f) else Color(0x99131822),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) chipColor else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .clickable { onCategorySelected(category) }
                                .testTag("chip_category_${category.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.emoji,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) chipColor else Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraZoomBar(
    zoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0x99131822), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(1f, 2f, 3f, 5f).forEach { zoomTarget ->
            val isSelected = kotlin.math.abs(zoomRatio - zoomTarget) < 0.4f
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) CyanNeon else Color.Transparent)
                    .clickable { onZoomChange(zoomTarget) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${zoomTarget.toInt()}x",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) DarkBackground else Color.White
                )
            }
        }
    }
}

@Composable
fun BottomShutterBar(
    isCapturing: Boolean,
    onShutterClick: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onDeepScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x00090C10), Color(0xDD090C10), Color(0xFF090C10))
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Deep Vision Scan Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onDeepScanClick() }
                .testTag("btn_deep_scan")
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0x337C4DFF), CircleShape)
                    .border(1.5.dp, PurpleAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Deep Vision",
                    tint = PurpleAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AI Vision",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PurpleAccent
            )
        }

        // Shutter Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.testTag("shutter_button")
        ) {
            // Outer glowing ring
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .border(
                        width = 4.dp,
                        brush = Brush.sweepGradient(
                            listOf(CyanNeon, PurpleAccent, CyanNeon)
                        ),
                        shape = CircleShape
                    )
            )

            // Center capture button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (isCapturing) CyanNeon.copy(alpha = 0.5f) else Color.White)
                    .clickable(
                        enabled = !isCapturing,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onShutterClick() }
            )
        }

        // Switch Camera (Flip)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onSwitchCamera() }
                .testTag("btn_switch_camera")
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0x33131822), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Balik Kamera",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Balik",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
