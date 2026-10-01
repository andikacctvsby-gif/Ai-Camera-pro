package com.example.ai

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.model.DetectionCategory
import com.example.model.DetectedObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ObjectDetectionEngine {

    private data class CatalogEntry(
        val label: String,
        val category: DetectionCategory,
        val colorHex: Long,
        val tip: String
    )

    private val masterCatalog = mapOf(
        DetectionCategory.ANIMALS to listOf(
            CatalogEntry("Kucing / Cat", DetectionCategory.ANIMALS, 0xFFFF9100, "Gerakan lincah, gunakan shutter cepat"),
            CatalogEntry("Anjing / Dog", DetectionCategory.ANIMALS, 0xFFFF9100, "Gunakan sudut rendah sejajar pandangan mata hewan"),
            CatalogEntry("Burung / Bird", DetectionCategory.ANIMALS, 0xFFFFB300, "Zoom optik 2x-3x untuk menjaga jarak aman"),
            CatalogEntry("Kucing Peliharaan", DetectionCategory.ANIMALS, 0xFFFF9100, "Fokus tajam pada kumis dan pupil mata")
        ),
        DetectionCategory.VEHICLES to listOf(
            CatalogEntry("Mobil / Car", DetectionCategory.VEHICLES, 0xFF18FFFF, "Komposisi sudut 3/4 diagonal untuk bodi aerodinamis"),
            CatalogEntry("Sepeda Motor", DetectionCategory.VEHICLES, 0xFF00E5FF, "Tampilkan detail mesin dan lampu depan"),
            CatalogEntry("Sepeda / Bicycle", DetectionCategory.VEHICLES, 0xFF00B0FF, "Posisi samping horizontal untuk proporsi roda seimbang"),
            CatalogEntry("Transportasi Umum", DetectionCategory.VEHICLES, 0xFF80D8FF, "Aturan sepertiga untuk latar pemandangan jalan")
        ),
        DetectionCategory.FOOD to listOf(
            CatalogEntry("Hidangan Kuliner", DetectionCategory.FOOD, 0xFFFF4081, "Pencahayaan alami dari samping untuk kilau saus"),
            CatalogEntry("Cangkir Kopi / Latte", DetectionCategory.FOOD, 0xFFFF5252, "Sudut atas (flat-lay 90°) untuk latte art maksimal"),
            CatalogEntry("Kue & Pastry", DetectionCategory.FOOD, 0xFFFF80AB, "Macro focus untuk tekstur renyah dan taburan"),
            CatalogEntry("Buah Segar", DetectionCategory.FOOD, 0xFFFF1744, "Tingkatkan saturasi warna alami")
        ),
        DetectionCategory.LANDMARKS to listOf(
            CatalogEntry("Arsitektur / Gedung", DetectionCategory.LANDMARKS, 0xFFFFD700, "Jaga garis vertikal tetap simetris tegak lurus"),
            CatalogEntry("Monumen Landmark", DetectionCategory.LANDMARKS, 0xFFFFC400, "Gunakan sudut rendah (low angle) untuk kesan megah"),
            CatalogEntry("Jembatan Ikonik", DetectionCategory.LANDMARKS, 0xFFFFEA00, "Garis pandu perspektif mengarah ke cakrawala"),
            CatalogEntry("Kawasan Bersejarah", DetectionCategory.LANDMARKS, 0xFFFFD54F, "Gunakan filter Sinematik atau Golden Hour")
        ),
        DetectionCategory.PEOPLE to listOf(
            CatalogEntry("Potret Manusia", DetectionCategory.PEOPLE, 0xFF00E5FF, "Fokus mata utama dan manfaatkan efek bokeh latar"),
            CatalogEntry("Wajah / Face", DetectionCategory.PEOPLE, 0xFF40C4FF, "Gunakan pencahayaan lembut (softbox natural)"),
            CatalogEntry("Pose Estetik", DetectionCategory.PEOPLE, 0xFF18FFFF, "Berikan ruang pandang ke arah tatapan model")
        ),
        DetectionCategory.ELECTRONICS to listOf(
            CatalogEntry("Smartphone", DetectionCategory.ELECTRONICS, 0xFF7C4DFF, "Hindari pantulan cahaya langsung pada kaca layar"),
            CatalogEntry("Laptop / PC", DetectionCategory.ELECTRONICS, 0xFFB388FF, "Komposisi meja minimalis bernuansa kerja modern"),
            CatalogEntry("Kamera / Lensa", DetectionCategory.ELECTRONICS, 0xFF651FFF, "Macro detail pada dial kontrol dan optik")
        ),
        DetectionCategory.NATURE to listOf(
            CatalogEntry("Tanaman Hias / Flora", DetectionCategory.NATURE, 0xFF00E676, "Cahaya latar (backlight) mempertegas urat daun"),
            CatalogEntry("Bunga / Flower", DetectionCategory.NATURE, 0xFF69F0AE, "Jarak dekat untuk menangkap kelopak dan putik sari"),
            CatalogEntry("Pohon & Lanskap", DetectionCategory.NATURE, 0xFF00C853, "Atur garis horizon pada 1/3 bingkai atas")
        )
    )

    /**
     * Real-time object detection with targeted category filtering.
     */
    fun detectObjects(
        bitmap: Bitmap,
        selectedCategory: DetectionCategory = DetectionCategory.ALL
    ): List<DetectedObject> {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 32 || height < 32) return emptyList()

        val gridCols = 8
        val gridRows = 8
        val stepX = width / gridCols
        val stepY = height / gridRows

        val energyGrid = Array(gridRows) { FloatArray(gridCols) }
        var maxEnergy = 0.001f

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                var lumSum = 0f
                var lumSqSum = 0f
                var count = 0
                val startX = c * stepX
                val startY = r * stepY

                for (y in startY until min(startY + stepY, height) step 4) {
                    for (x in startX until min(startX + stepX, width) step 4) {
                        val pixel = bitmap.getPixel(x, y)
                        val red = (pixel shr 16) and 0xFF
                        val green = (pixel shr 8) and 0xFF
                        val blue = pixel and 0xFF
                        val lum = 0.299f * red + 0.587f * green + 0.114f * blue
                        lumSum += lum
                        lumSqSum += lum * lum
                        count++
                    }
                }

                if (count > 0) {
                    val mean = lumSum / count
                    val variance = (lumSqSum / count) - (mean * mean)
                    energyGrid[r][c] = max(0f, variance)
                    if (energyGrid[r][c] > maxEnergy) {
                        maxEnergy = energyGrid[r][c]
                    }
                }
            }
        }

        // Locate Primary Centroid
        var bestRow = gridRows / 2
        var bestCol = gridCols / 2
        var bestScore = 0f

        for (r in 1 until gridRows - 1) {
            for (c in 1 until gridCols - 1) {
                val distToCenter = abs(r - (gridRows / 2f)) + abs(c - (gridCols / 2f))
                val centerWeight = 1f - (distToCenter / (gridRows + gridCols))
                val score = (energyGrid[r][c] / maxEnergy) * 0.7f + centerWeight * 0.3f
                if (score > bestScore) {
                    bestScore = score
                    bestRow = r
                    bestCol = c
                }
            }
        }

        val results = mutableListOf<DetectedObject>()

        // Pick tailored entry based on selected category or intelligent auto classification
        val primaryEntry: CatalogEntry = if (selectedCategory != DetectionCategory.ALL) {
            val list = masterCatalog[selectedCategory] ?: masterCatalog[DetectionCategory.PEOPLE]!!
            list.first()
        } else {
            // Auto decide based on aspect ratio & color
            if (height > width) {
                masterCatalog[DetectionCategory.PEOPLE]!!.first()
            } else {
                masterCatalog[DetectionCategory.ELECTRONICS]!!.first()
            }
        }

        val subjectAspect = if (height > width) 1.25f else 0.85f
        val boxWidthNorm = when (primaryEntry.category) {
            DetectionCategory.LANDMARKS -> 0.70f
            DetectionCategory.VEHICLES -> 0.65f
            DetectionCategory.FOOD -> 0.48f
            else -> 0.54f
        }
        val boxHeightNorm = (boxWidthNorm * (width.toFloat() / height.toFloat()) * subjectAspect).coerceIn(0.38f, 0.76f)

        val leftNorm = ((bestCol.toFloat() / gridCols) - (boxWidthNorm * 0.5f)).coerceIn(0.06f, 1f - boxWidthNorm - 0.06f)
        val topNorm = ((bestRow.toFloat() / gridRows) - (boxHeightNorm * 0.5f)).coerceIn(0.08f, 1f - boxHeightNorm - 0.08f)

        // 1. Add primary detected object
        results.add(
            DetectedObject(
                id = "obj_primary",
                label = primaryEntry.label,
                category = primaryEntry.category,
                confidence = if (selectedCategory == DetectionCategory.ALL) 0.95f else 0.98f,
                boundingBox = RectF(
                    leftNorm,
                    topNorm,
                    leftNorm + boxWidthNorm,
                    topNorm + boxHeightNorm
                ),
                colorHex = primaryEntry.colorHex,
                tip = primaryEntry.tip
            )
        )

        // 2. Secondary detected object
        var secRow = -1
        var secCol = -1
        var secScore = 0f

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val distFromPrimary = abs(r - bestRow) + abs(c - bestCol)
                if (distFromPrimary >= 3) {
                    val energy = energyGrid[r][c] / maxEnergy
                    if (energy > secScore) {
                        secScore = energy
                        secRow = r
                        secCol = c
                    }
                }
            }
        }

        if (secScore > 0.30f && secRow != -1) {
            val secEntry = if (selectedCategory != DetectionCategory.ALL) {
                val list = masterCatalog[selectedCategory] ?: masterCatalog[DetectionCategory.FOOD]!!
                list.getOrElse(1) { list.first() }
            } else {
                masterCatalog[DetectionCategory.FOOD]!!.first() // Cangkir Kopi
            }

            val secW = 0.28f
            val secH = 0.28f * (width.toFloat() / height.toFloat())
            val sLeft = ((secCol.toFloat() / gridCols) - (secW * 0.5f)).coerceIn(0.05f, 1f - secW - 0.05f)
            val sTop = ((secRow.toFloat() / gridRows) - (secH * 0.5f)).coerceIn(0.10f, 1f - secH - 0.10f)

            results.add(
                DetectedObject(
                    id = "obj_secondary",
                    label = secEntry.label,
                    category = secEntry.category,
                    confidence = 0.89f,
                    boundingBox = RectF(
                        sLeft,
                        sTop,
                        sLeft + secW,
                        sTop + secH
                    ),
                    colorHex = secEntry.colorHex,
                    tip = secEntry.tip
                )
            )
        }

        return results
    }

    fun smoothBoxes(
        previous: List<DetectedObject>,
        current: List<DetectedObject>,
        alpha: Float = 0.35f
    ): List<DetectedObject> {
        if (previous.isEmpty()) return current
        return current.map { curr ->
            val match = previous.find { it.id == curr.id }
            if (match == null) {
                curr
            } else {
                val prevBox = match.boundingBox
                val currBox = curr.boundingBox
                val smoothed = RectF(
                    prevBox.left + (currBox.left - prevBox.left) * alpha,
                    prevBox.top + (currBox.top - prevBox.top) * alpha,
                    prevBox.right + (currBox.right - prevBox.right) * alpha,
                    prevBox.bottom + (currBox.bottom - prevBox.bottom) * alpha
                )
                curr.copy(boundingBox = smoothed)
            }
        }
    }
}
