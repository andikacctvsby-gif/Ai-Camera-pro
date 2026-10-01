package com.example.ui.components

import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DetectedObject

@Composable
fun ObjectBoundingBoxOverlay(
    detectedObjects: List<DetectedObject>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("detection_overlay_canvas")
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (obj in detectedObjects) {
            val left = obj.boundingBox.left * canvasWidth
            val top = obj.boundingBox.top * canvasHeight
            val right = obj.boundingBox.right * canvasWidth
            val bottom = obj.boundingBox.bottom * canvasHeight
            val boxWidth = right - left
            val boxHeight = bottom - top

            val strokeColor = Color(obj.colorHex).copy(alpha = pulseAlpha)
            val cornerLen = (boxWidth.coerceAtMost(boxHeight) * 0.22f).coerceIn(16f, 40f)

            // Draw Corner Brackets (High-Tech Reticle)
            val cornerStroke = Stroke(width = 4.dp.toPx())

            // Top-Left Corner
            drawPath(
                path = Path().apply {
                    moveTo(left, top + cornerLen)
                    lineTo(left, top)
                    lineTo(left + cornerLen, top)
                },
                color = strokeColor,
                style = cornerStroke
            )

            // Top-Right Corner
            drawPath(
                path = Path().apply {
                    moveTo(right - cornerLen, top)
                    lineTo(right, top)
                    lineTo(right, top + cornerLen)
                },
                color = strokeColor,
                style = cornerStroke
            )

            // Bottom-Left Corner
            drawPath(
                path = Path().apply {
                    moveTo(left, bottom - cornerLen)
                    lineTo(left, bottom)
                    lineTo(left + cornerLen, bottom)
                },
                color = strokeColor,
                style = cornerStroke
            )

            // Bottom-Right Corner
            drawPath(
                path = Path().apply {
                    moveTo(right - cornerLen, bottom)
                    lineTo(right, bottom)
                    lineTo(right, bottom - cornerLen)
                },
                color = strokeColor,
                style = cornerStroke
            )

            // Dashed outline box
            drawRoundRect(
                color = strokeColor.copy(alpha = 0.35f),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            // Category & Confidence Badge Chip
            val confPercent = (obj.confidence * 100).toInt()
            val labelText = "✦ ${obj.label} · $confPercent%"
            val textLayout = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            )

            val badgePadX = 8.dp.toPx()
            val badgePadY = 4.dp.toPx()
            val badgeW = textLayout.size.width + badgePadX * 2
            val badgeH = textLayout.size.height + badgePadY * 2
            val badgeTop = (top - badgeH - 4.dp.toPx()).coerceAtLeast(16.dp.toPx())

            // Badge Background
            drawRoundRect(
                color = Color(0xDD090C10),
                topLeft = Offset(left, badgeTop),
                size = Size(badgeW, badgeH),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(left, badgeTop),
                size = Size(badgeW, badgeH),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Badge Text
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                topLeft = Offset(left + badgePadX, badgeTop + badgePadY),
                style = TextStyle(
                    color = Color(obj.colorHex),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // Tip Chip at Bottom of Box if available
            if (obj.tip.isNotBlank()) {
                val tipText = "ℹ ${obj.tip}"
                val tipLayout = textMeasurer.measure(
                    text = tipText,
                    style = TextStyle(
                        color = Color(0xFFE0E0E0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                val tipW = tipLayout.size.width + badgePadX * 2
                val tipH = tipLayout.size.height + badgePadY * 2
                val tipTop = (bottom + 4.dp.toPx()).coerceAtMost(canvasHeight - tipH - 8.dp.toPx())

                drawRoundRect(
                    color = Color(0xB3131822),
                    topLeft = Offset(left, tipTop),
                    size = Size(tipW, tipH),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = tipText,
                    topLeft = Offset(left + badgePadX, tipTop + badgePadY),
                    style = TextStyle(
                        color = Color(0xFFE0E0E0),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
