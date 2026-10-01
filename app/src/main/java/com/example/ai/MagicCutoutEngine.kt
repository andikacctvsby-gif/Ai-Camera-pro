package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.example.model.BackgroundOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MagicCutoutEngine {

    /**
     * Automatically extracts the foreground subject and removes background.
     * Produces a clean ARGB_8888 Bitmap with alpha transparency.
     */
    suspend fun removeBackground(
        sourceBitmap: Bitmap,
        featherRadius: Float = 4f,
        subjectRect: RectF? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample background color statistics from image corners and border
        val cornerSampleSize = min(20, min(width, height) / 8)
        var bgR = 0L
        var bgG = 0L
        var bgB = 0L
        var sampleCount = 0

        val samplePoints = listOf(
            0 to 0,
            width - cornerSampleSize to 0,
            0 to height - cornerSampleSize,
            width - cornerSampleSize to height - cornerSampleSize,
            width / 2 to 0,
            0 to height / 2,
            width - cornerSampleSize to height / 2
        )

        for ((startX, startY) in samplePoints) {
            for (y in startY until min(startY + cornerSampleSize, height)) {
                for (x in startX until min(startX + cornerSampleSize, width)) {
                    val p = pixels[y * width + x]
                    bgR += (p shr 16) and 0xFF
                    bgG += (p shr 8) and 0xFF
                    bgB += p and 0xFF
                    sampleCount++
                }
            }
        }

        val avgBgR = (bgR / sampleCount).toFloat()
        val avgBgG = (bgG / sampleCount).toFloat()
        val avgBgB = (bgB / sampleCount).toFloat()

        // Center of subject
        val centerX = subjectRect?.let { ((it.left + it.right) / 2f * width).toInt() } ?: (width / 2)
        val centerY = subjectRect?.let { ((it.top + it.bottom) / 2f * height).toInt() } ?: (height / 2)
        val maxDistFromCenter = sqrt((width * width + height * height).toDouble() / 4.0).toFloat()

        val alphaMask = ByteArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val p = pixels[idx]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                // Color difference from background
                val dR = r - avgBgR
                val dG = g - avgBgG
                val dB = b - avgBgB
                val colorDist = sqrt((dR * dR + dG * dG + dB * dB).toDouble()).toFloat()

                // Distance from center of interest
                val dx = x - centerX
                val dy = y - centerY
                val distFromCenter = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val centerFactor = (1f - (distFromCenter / maxDistFromCenter)).coerceIn(0f, 1f)

                // Foreground confidence score
                val foregroundScore = (colorDist / 140f) * 0.7f + (centerFactor * 0.6f)

                // Subject bounding box prior if provided
                val inSubjectBox = if (subjectRect != null) {
                    val pxNorm = x.toFloat() / width
                    val pyNorm = y.toFloat() / height
                    pxNorm in (subjectRect.left - 0.05f)..(subjectRect.right + 0.05f) &&
                            pyNorm in (subjectRect.top - 0.05f)..(subjectRect.bottom + 0.05f)
                } else {
                    // Elliptical portrait zone
                    val normX = (x - centerX).toFloat() / (width * 0.42f)
                    val normY = (y - centerY).toFloat() / (height * 0.48f)
                    (normX * normX + normY * normY) <= 1.0f
                }

                val finalAlpha = when {
                    inSubjectBox && foregroundScore > 0.45f -> 255
                    inSubjectBox -> ((foregroundScore / 0.45f) * 255f).toInt().coerceIn(0, 255)
                    foregroundScore > 0.85f -> ((foregroundScore - 0.85f) / 0.15f * 200f).toInt().coerceIn(0, 255)
                    else -> 0
                }

                alphaMask[idx] = finalAlpha.toByte()
            }
        }

        // Apply edge smoothing / anti-aliasing
        val outputPixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val p = pixels[idx]
                var a = (alphaMask[idx].toInt() and 0xFF)

                // 3x3 box blur on alpha edges
                if (a in 1..254 && featherRadius > 0) {
                    var sumA = 0
                    var count = 0
                    for (ky in -1..1) {
                        val ny = y + ky
                        if (ny in 0 until height) {
                            for (kx in -1..1) {
                                val nx = x + kx
                                if (nx in 0 until width) {
                                    sumA += (alphaMask[ny * width + nx].toInt() and 0xFF)
                                    count++
                                }
                            }
                        }
                    }
                    if (count > 0) {
                        a = (sumA / count).coerceIn(0, 255)
                    }
                }

                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                outputPixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * Composites extracted foreground subject onto a chosen background
     * (Transparent, Solid Color, Gradient, Scenic Photo, or Custom Image).
     */
    suspend fun compositeWithBackground(
        foregroundCutout: Bitmap,
        backgroundOption: BackgroundOption,
        presetSceneBitmap: Bitmap? = null,
        enableDropShadow: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = foregroundCutout.width
        val height = foregroundCutout.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Draw Background
        when (backgroundOption) {
            is BackgroundOption.Transparent -> {
                // Keep transparent
            }
            is BackgroundOption.SolidColor -> {
                canvas.drawColor(backgroundOption.color.toInt())
            }
            is BackgroundOption.Gradient -> {
                val paint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        backgroundOption.startColor.toInt(),
                        backgroundOption.endColor.toInt(),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            is BackgroundOption.PresetScene, is BackgroundOption.CustomImage -> {
                if (presetSceneBitmap != null) {
                    val srcRect = Rect(0, 0, presetSceneBitmap.width, presetSceneBitmap.height)
                    val dstRect = Rect(0, 0, width, height)
                    canvas.drawBitmap(presetSceneBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                } else {
                    canvas.drawColor(0xFF131822.toInt())
                }
            }
        }

        // 2. Draw Soft Drop Shadow if enabled
        if (enableDropShadow && backgroundOption !is BackgroundOption.Transparent) {
            val shadowPaint = Paint().apply {
                color = Color.argb(90, 0, 0, 0)
                maskFilter = android.graphics.BlurMaskFilter(24f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            val shadowBitmap = foregroundCutout.extractAlpha(shadowPaint, null)
            canvas.drawBitmap(shadowBitmap, 8f, 16f, shadowPaint)
        }

        // 3. Draw Foreground Cutout
        val fgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(foregroundCutout, 0f, 0f, fgPaint)

        result
    }
}
