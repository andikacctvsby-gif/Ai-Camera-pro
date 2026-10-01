package com.example.model

import android.graphics.RectF

data class DetectedObject(
    val id: String,
    val label: String,
    val category: DetectionCategory,
    val confidence: Float,
    val boundingBox: RectF, // Normalized 0f..1f (left, top, right, bottom)
    val colorHex: Long = 0xFF00E5FF,
    val tip: String = ""
)

enum class DetectionCategory(
    val id: String,
    val displayName: String,
    val emoji: String,
    val colorHex: Long,
    val guidance: String
) {
    ALL("all", "Semua", "✦", 0xFF00E5FF, "Deteksi semua objek di sekitar"),
    ANIMALS("animals", "Hewan", "🐾", 0xFFFF9100, "Fokus cepat pada mata dan gerakan hewan"),
    VEHICLES("vehicles", "Kendaraan", "🚗", 0xFF18FFFF, "Deteksi mobil, motor, sepeda, & transportasi"),
    FOOD("food", "Makanan", "🍔", 0xFFFF4081, "Deteksi hidangan, buah, minuman, & kuliner"),
    LANDMARKS("landmarks", "Landmark", "🏛️", 0xFFFFD700, "Deteksi arsitektur, monumen, & gedung"),
    PEOPLE("people", "Manusia", "👤", 0xFF00E5FF, "Fokus potret wajah & pose tubuh"),
    ELECTRONICS("electronics", "Gadget", "💻", 0xFF7C4DFF, "Deteksi smartphone, laptop, & perangkat"),
    NATURE("nature", "Alam", "🌿", 0xFF00E676, "Deteksi flora, daun, bunga, & pemandangan")
}

data class DeepScanResult(
    val mainSubject: String,
    val category: String,
    val confidence: Float,
    val details: String,
    val photographyTips: String,
    val detectedItems: List<String>,
    val suggestedEditing: String
)

enum class FilterPreset(val displayName: String, val description: String) {
    ORIGINAL("Asli", "Warna natural tanpa filter"),
    VINTAGE("Vintage", "Nuansa nostalgia analog vintage tahun 90-an"),
    BLACK_WHITE("Hitam Putih", "Monokrom klasik dengan kontras dramatis"),
    CINEMATIC("Sinematik", "Gradasi teal dan oranye bergaya film layar lebar"),
    PORTRAIT("Potret", "Pencahayaan lembut dan warna kulit menawan"),
    VIVID("Vivid Pop", "Warna cerah, tajam dan kontras tinggi"),
    CYBERPUNK("Cyberpunk", "Kombinasi ungu neon dan cyan futuristik"),
    GOLDEN_HOUR("Golden Hour", "Kehangatan matahari terbenam yang memikat"),
    NORDIC_CHILL("Nordic Chill", "Suhu dingin sejuk dengan nuansa biru elegan")
}

enum class CropRatio(val label: String, val ratioX: Float, val ratioY: Float) {
    ORIGINAL("Asli", 0f, 0f),
    SQUARE("1:1 Persegi", 1f, 1f),
    PORTRAIT("4:5 Instagram", 4f, 5f),
    STORY("9:16 Story", 9f, 16f),
    LANDSCAPE("16:9 Lebar", 16f, 9f),
    STANDARD("3:4 Foto", 3f, 4f)
}

data class EditAdjustmentState(
    val brightness: Float = 0f,       // -100f to 100f
    val contrast: Float = 0f,         // -100f to 100f
    val saturation: Float = 0f,       // -100f to 100f
    val warmth: Float = 0f,           // -100f to 100f
    val sharpness: Float = 0f,        // 0f to 100f
    val vignette: Float = 0f,         // 0f to 100f
    val bokehBlur: Float = 0f,        // 0f to 100f
    val activeFilter: FilterPreset = FilterPreset.ORIGINAL,
    val filterIntensity: Float = 100f, // 0f to 100f
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontally: Boolean = false,
    val cropRatio: CropRatio = CropRatio.ORIGINAL,
    val watermarkText: String = "",
    val selectedBadge: String? = null
)

sealed class BackgroundOption {
    data object Transparent : BackgroundOption()
    data class SolidColor(val color: Long, val name: String) : BackgroundOption()
    data class Gradient(val startColor: Long, val endColor: Long, val name: String) : BackgroundOption()
    data class PresetScene(val drawableRes: Int, val name: String) : BackgroundOption()
    data class CustomImage(val uriString: String, val name: String) : BackgroundOption()
}

data class CapturedPhoto(
    val id: String,
    val filePath: String,
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val tags: List<String> = emptyList(),
    val isEdited: Boolean = false
)
