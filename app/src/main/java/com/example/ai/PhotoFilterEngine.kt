package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.model.CropRatio
import com.example.model.EditAdjustmentState
import com.example.model.FilterPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class PhotoFilterEngine {

    /**
     * Renders a fully adjusted and filtered bitmap based on [EditAdjustmentState].
     */
    suspend fun processImage(
        originalBitmap: Bitmap,
        state: EditAdjustmentState
    ): Bitmap = withContext(Dispatchers.Default) {
        var current = originalBitmap

        // 1. Rotation and Horizontal Flip
        if (state.rotationDegrees != 0f || state.isFlippedHorizontally) {
            val matrix = Matrix().apply {
                if (state.isFlippedHorizontally) {
                    postScale(-1f, 1f)
                }
                postRotate(state.rotationDegrees)
            }
            current = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
        }

        // 2. Aspect Ratio Crop
        if (state.cropRatio != CropRatio.ORIGINAL) {
            current = applyCrop(current, state.cropRatio)
        }

        // 3. Color Transformations (Brightness, Contrast, Saturation, Warmth, Filters)
        current = applyColorAdjustments(current, state)

        // 4. Sharpness
        if (state.sharpness > 0f) {
            current = applySharpness(current, state.sharpness / 100f)
        }

        // 5. Bokeh / Depth Blur
        if (state.bokehBlur > 0f) {
            current = applyBokehBlur(current, state.bokehBlur / 100f)
        }

        // 6. Vignette
        if (state.vignette > 0f) {
            current = applyVignette(current, state.vignette / 100f)
        }

        // 7. Watermark Caption & AI Badges
        if (state.watermarkText.isNotBlank() || state.selectedBadge != null) {
            current = applyOverlays(current, state.watermarkText, state.selectedBadge)
        }

        current
    }

    private fun applyCrop(bitmap: Bitmap, ratio: CropRatio): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val targetAspect = ratio.ratioX / ratio.ratioY
        val currentAspect = w.toFloat() / h.toFloat()

        var cropW = w
        var cropH = h
        if (currentAspect > targetAspect) {
            cropW = (h * targetAspect).toInt().coerceAtMost(w)
        } else {
            cropH = (w / targetAspect).toInt().coerceAtMost(h)
        }

        val startX = (w - cropW) / 2
        val startY = (h - cropH) / 2
        return Bitmap.createBitmap(bitmap, startX, startY, cropW, cropH)
    }

    private fun applyColorAdjustments(bitmap: Bitmap, state: EditAdjustmentState): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Combined Color Matrix
        val combinedMatrix = ColorMatrix()

        // Apply Preset Filter with adjustable intensity (0% to 100%)
        val filterMatrix = getInterpolatedFilterMatrix(state.activeFilter, state.filterIntensity)
        combinedMatrix.postConcat(filterMatrix)

        // Brightness (-100..100) -> scale
        val bFactor = state.brightness * 2.55f
        val brightnessMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, bFactor,
                0f, 1f, 0f, 0f, bFactor,
                0f, 0f, 1f, 0f, bFactor,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combinedMatrix.postConcat(brightnessMatrix)

        // Contrast (-100..100)
        val cVal = (state.contrast + 100f) / 100f
        val cFactor = cVal * cVal
        val cOffset = (1f - cFactor) * 128f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                cFactor, 0f, 0f, 0f, cOffset,
                0f, cFactor, 0f, 0f, cOffset,
                0f, 0f, cFactor, 0f, cOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combinedMatrix.postConcat(contrastMatrix)

        // Saturation (-100..100)
        val satFactor = ((state.saturation + 100f) / 100f).coerceAtLeast(0f)
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(satFactor)
        combinedMatrix.postConcat(satMatrix)

        // Warmth / Temperature (-100..100)
        val wOffset = state.warmth * 0.8f
        val warmthMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, wOffset,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -wOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combinedMatrix.postConcat(warmthMatrix)

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
            isFilterBitmap = true
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun getInterpolatedFilterMatrix(preset: FilterPreset, intensity: Float): ColorMatrix {
        val targetMatrix = getRawFilterMatrix(preset)
        if (preset == FilterPreset.ORIGINAL || intensity >= 99f) {
            return targetMatrix
        }
        val alpha = (intensity / 100f).coerceIn(0f, 1f)
        val identity = ColorMatrix()
        val targetArray = targetMatrix.array
        val identityArray = identity.array
        val interpolatedArray = FloatArray(20)

        for (i in 0 until 20) {
            interpolatedArray[i] = identityArray[i] + (targetArray[i] - identityArray[i]) * alpha
        }
        return ColorMatrix(interpolatedArray)
    }

    private fun getRawFilterMatrix(preset: FilterPreset): ColorMatrix {
        return when (preset) {
            FilterPreset.ORIGINAL -> ColorMatrix()

            FilterPreset.VINTAGE -> ColorMatrix(
                floatArrayOf(
                    1.15f, 0.08f, 0.02f, 0f, 18f,
                    0.05f, 1.05f, 0.02f, 0f, 10f,
                    0.01f, 0.08f, 0.82f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { setSaturation(0.85f) }

            FilterPreset.BLACK_WHITE -> ColorMatrix().apply {
                setSaturation(0f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.35f, 0f, 0f, 0f, -28f,
                            0f, 1.35f, 0f, 0f, -28f,
                            0f, 0f, 1.35f, 0f, -28f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }

            FilterPreset.CINEMATIC -> ColorMatrix(
                floatArrayOf(
                    1.12f, 0f, 0.10f, 0f, 12f,
                    0f, 1.15f, 0.02f, 0f, 6f,
                    -0.08f, 0.08f, 1.28f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            FilterPreset.PORTRAIT -> ColorMatrix(
                floatArrayOf(
                    1.08f, 0.02f, 0f, 0f, 14f,
                    0.02f, 1.06f, 0f, 0f, 10f,
                    0f, 0.02f, 1.02f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            FilterPreset.VIVID -> ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 6f,
                    0f, 1.2f, 0f, 0f, 6f,
                    0f, 0f, 1.2f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { setSaturation(1.35f) }

            FilterPreset.CYBERPUNK -> ColorMatrix(
                floatArrayOf(
                    1.28f, 0f, 0.22f, 0f, 22f,
                    -0.10f, 0.95f, 0.18f, 0f, -6f,
                    0.10f, 0.18f, 1.45f, 0f, 32f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { setSaturation(1.4f) }

            FilterPreset.GOLDEN_HOUR -> ColorMatrix(
                floatArrayOf(
                    1.22f, 0.12f, 0f, 0f, 28f,
                    0.06f, 1.12f, 0f, 0f, 18f,
                    0f, 0f, 0.82f, 0f, -22f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            FilterPreset.NORDIC_CHILL -> ColorMatrix(
                floatArrayOf(
                    0.88f, 0f, 0f, 0f, -12f,
                    0f, 0.98f, 0.05f, 0f, 0f,
                    0.05f, 0.12f, 1.28f, 0f, 28f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { setSaturation(0.92f) }
        }
    }

    private fun applySharpness(bitmap: Bitmap, intensity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val factor = intensity * 1.5f
        val center = 1f + 4f * factor

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val c = srcPixels[idx]
                val top = srcPixels[(y - 1) * width + x]
                val bottom = srcPixels[(y + 1) * width + x]
                val left = srcPixels[y * width + (x - 1)]
                val right = srcPixels[y * width + (x + 1)]

                val cR = (c shr 16) and 0xFF
                val cG = (c shr 8) and 0xFF
                val cB = c and 0xFF

                val nR = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) +
                        ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF))
                val nG = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) +
                        ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF))
                val nB = ((top and 0xFF) + (bottom and 0xFF) + (left and 0xFF) + (right and 0xFF))

                val r = (center * cR - factor * nR).toInt().coerceIn(0, 255)
                val g = (center * cG - factor * nG).toInt().coerceIn(0, 255)
                val b = (center * cB - factor * nB).toInt().coerceIn(0, 255)

                dstPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyBokehBlur(bitmap: Bitmap, blurAmount: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw original
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Blur background border by drawing downscaled/upscaled bitmap with radial mask
        val scale = 0.2f
        val small = Bitmap.createScaledBitmap(bitmap, max(1, (width * scale).toInt()), max(1, (height * scale).toInt()), true)
        val blurred = Bitmap.createScaledBitmap(small, width, height, true)

        val maskPaint = Paint().apply {
            shader = RadialGradient(
                width / 2f, height / 2f,
                max(width, height) * 0.45f,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb((blurAmount * 240).toInt(), 255, 255, 255)),
                floatArrayOf(0f, 0.45f, 1.0f),
                Shader.TileMode.CLAMP
            )
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
        }

        val blurCanvas = Canvas(blurred)
        blurCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)

        canvas.drawBitmap(blurred, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        return output
    }

    private fun applyVignette(bitmap: Bitmap, intensity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val radius = max(width, height) * 0.75f
        val alpha = (intensity * 230).toInt().coerceIn(0, 255)

        val vignettePaint = Paint().apply {
            shader = RadialGradient(
                width / 2f, height / 2f,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
                floatArrayOf(0f, 0.5f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        return output
    }

    private fun applyOverlays(bitmap: Bitmap, text: String, badge: String?): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // 1. Watermark caption text
        if (text.isNotBlank()) {
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = max(18f, width * 0.04f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(6f, 2f, 2f, Color.BLACK)
                isAntiAlias = true
            }
            canvas.drawText(text, width * 0.05f, height - (width * 0.06f), textPaint)
        }

        // 2. AI Badge stamp
        if (badge != null) {
            val badgePaint = Paint().apply {
                color = Color.argb(180, 19, 24, 34)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val borderPaint = Paint().apply {
                color = Color.argb(220, 0, 229, 255)
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            val badgeTextPaint = Paint().apply {
                color = Color.argb(255, 0, 229, 255)
                textSize = max(14f, width * 0.032f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }

            val badgeText = "✦ $badge"
            val textW = badgeTextPaint.measureText(badgeText)
            val padX = 20f
            val padY = 12f
            val rect = RectF(
                width - textW - padX * 2 - 24f,
                24f,
                width - 24f,
                24f + badgeTextPaint.textSize + padY * 2
            )

            canvas.drawRoundRect(rect, 12f, 12f, badgePaint)
            canvas.drawRoundRect(rect, 12f, 12f, borderPaint)
            canvas.drawText(badgeText, rect.left + padX, rect.bottom - padY - 2f, badgeTextPaint)
        }

        return output
    }
}
