package com.example.ai

import com.example.model.CropRatio
import com.example.model.EditAdjustmentState
import com.example.model.FilterPreset
import java.util.Locale

sealed class VoiceEditCommand {
    data class ApplyFilter(val preset: FilterPreset, val intensity: Float = 100f, val message: String) : VoiceEditCommand()
    data class AdjustBrightness(val value: Float, val message: String) : VoiceEditCommand()
    data class AdjustContrast(val value: Float, val message: String) : VoiceEditCommand()
    data class AdjustSaturation(val value: Float, val message: String) : VoiceEditCommand()
    data class AdjustWarmth(val value: Float, val message: String) : VoiceEditCommand()
    data class SetBokeh(val value: Float, val message: String) : VoiceEditCommand()
    data class SetVignette(val value: Float, val message: String) : VoiceEditCommand()
    data class SetSharpness(val value: Float, val message: String) : VoiceEditCommand()
    data class SetCrop(val ratio: CropRatio, val message: String) : VoiceEditCommand()
    data class Rotate(val message: String) : VoiceEditCommand()
    data class Flip(val message: String) : VoiceEditCommand()
    data class AutoEnhance(val message: String) : VoiceEditCommand()
    data class Reset(val message: String) : VoiceEditCommand()
    data class AddBadge(val badge: String, val message: String) : VoiceEditCommand()
    data class RemoveBackground(val message: String) : VoiceEditCommand()
    data class Unknown(val rawText: String) : VoiceEditCommand()
}

class VoiceEditParser {

    /**
     * Parses spoken text into a list of structured editing commands.
     */
    fun parse(spokenText: String): List<VoiceEditCommand> {
        val text = spokenText.lowercase(Locale.ROOT).trim()
        val commands = mutableListOf<VoiceEditCommand>()

        // 1. Auto Enhance / Sempurnakan
        if (text.contains("auto") || text.contains("sempurnakan") || text.contains("otomatis") || text.contains("magic enhance")) {
            commands.add(VoiceEditCommand.AutoEnhance("✦ Magic Auto-Tuning diterapkan secara cerdas!"))
            return commands
        }

        // 2. Reset
        if (text.contains("reset") || text.contains("kembalikan") || text.contains("ulang") || text.contains("semula")) {
            commands.add(VoiceEditCommand.Reset("Pengaturan dikembalikan ke awal."))
            return commands
        }

        // 3. Filters
        when {
            text.contains("vintage") || text.contains("retro") || text.contains("klasik") -> {
                commands.add(VoiceEditCommand.ApplyFilter(FilterPreset.VINTAGE, 95f, "Filter Vintage diaktifkan."))
            }
            text.contains("hitam putih") || text.contains("monokrom") || text.contains("black and white") || text.contains("b&w") || text.contains("noir") -> {
                commands.add(VoiceEditCommand.ApplyFilter(FilterPreset.BLACK_WHITE, 100f, "Filter Hitam Putih dramatis diaktifkan."))
            }
            text.contains("sinematik") || text.contains("cinematic") || text.contains("film") -> {
                commands.add(VoiceEditCommand.ApplyFilter(FilterPreset.CINEMATIC, 95f, "Filter Sinematik teal & orange diterapkan."))
            }
            text.contains("potret") || text.contains("portrait") || text.contains("wajah") || text.contains("halus") || text.contains("kulit") -> {
                commands.add(VoiceEditCommand.ApplyFilter(FilterPreset.PORTRAIT, 90f, "Filter Potret lembut diaktifkan."))
            }
            text.contains("vivid") || text.contains("cerah") || text.contains("tajamkan warna") || text.contains("pop") -> {
                commands.add(VoiceEditCommand.ApplyFilter(FilterPreset.VIVID, 100f, "Filter Vivid Pop cerah diterapkan."))
            }
            text.contains("cyberpunk") || text.contains("neon") || text.contains("cyber") -> {
                commands.add(VoiceEditCommand.ApplyFilter(FilterPreset.CYBERPUNK, 90f, "Filter Cyberpunk neon diterapkan."))
            }
            text.contains("golden") || text.contains("senja") || text.contains("matahari terbenam") -> {
                commands.add(VoiceEditCommand.ApplyFilter(FilterPreset.GOLDEN_HOUR, 95f, "Filter Golden Hour hangat diterapkan."))
            }
            text.contains("dingin") || text.contains("nordic") || text.contains("biru") || text.contains("cool") -> {
                commands.add(VoiceEditCommand.ApplyFilter(FilterPreset.NORDIC_CHILL, 90f, "Filter Nordic Chill sejuk diterapkan."))
            }
        }

        // 4. Brightness / Kecerahan
        if (text.contains("kecerahan") || text.contains("terang") || text.contains("brightness") || text.contains("gelap")) {
            val delta = when {
                text.contains("turun") || text.contains("gelap") || text.contains("kurang") -> -20f
                text.contains("sangat terang") || text.contains("maksimal") -> 40f
                else -> 20f
            }
            commands.add(VoiceEditCommand.AdjustBrightness(delta, "Kecerahan disesuaikan (${if (delta > 0) "+$delta" else "$delta"})."))
        }

        // 5. Contrast / Kontras
        if (text.contains("kontras") || text.contains("contrast")) {
            val delta = if (text.contains("turun") || text.contains("kurang")) -20f else 25f
            commands.add(VoiceEditCommand.AdjustContrast(delta, "Kontras ditingkatkan."))
        }

        // 6. Saturation / Kejenuhan
        if (text.contains("saturasi") || text.contains("warna warni") || text.contains("saturation")) {
            val delta = if (text.contains("turun") || text.contains("pucat")) -25f else 30f
            commands.add(VoiceEditCommand.AdjustSaturation(delta, "Saturasi warna diperkaya."))
        }

        // 7. Warmth / Kehangatan
        if (text.contains("hangat") || text.contains("warmth") || text.contains("suhu")) {
            val delta = if (text.contains("dingin")) -25f else 25f
            commands.add(VoiceEditCommand.AdjustWarmth(delta, "Suhu warna disesuaikan."))
        }

        // 8. Bokeh / Blur
        if (text.contains("bokeh") || text.contains("blur") || text.contains("buramkan latar") || text.contains("depth")) {
            commands.add(VoiceEditCommand.SetBokeh(50f, "Efek Bokeh latar belakang diterapkan."))
        }

        // 9. Vignette
        if (text.contains("vignette") || text.contains("vignet") || text.contains("sudut gelap")) {
            commands.add(VoiceEditCommand.SetVignette(40f, "Efek Vignette sudut diterapkan."))
        }

        // 10. Sharpness / Ketajaman
        if (text.contains("tajam") || text.contains("detail") || text.contains("sharp")) {
            commands.add(VoiceEditCommand.SetSharpness(35f, "Ketajaman detail ditingkatkan."))
        }

        // 11. Rotate & Flip
        if (text.contains("putar") || text.contains("rotate")) {
            commands.add(VoiceEditCommand.Rotate("Foto diputar 90 derajat."))
        }
        if (text.contains("balik") || text.contains("flip") || text.contains("cermin")) {
            commands.add(VoiceEditCommand.Flip("Foto dibalik secara horizontal."))
        }

        // 12. Crop Ratios
        if (text.contains("persegi") || text.contains("kotak") || text.contains("1:1") || text.contains("instagram")) {
            commands.add(VoiceEditCommand.SetCrop(CropRatio.SQUARE, "Rasio dipotong ke 1:1 Persegi."))
        } else if (text.contains("story") || text.contains("9:16")) {
            commands.add(VoiceEditCommand.SetCrop(CropRatio.STORY, "Rasio dipotong ke 9:16 Story."))
        } else if (text.contains("sinema") || text.contains("16:9") || text.contains("lebar")) {
            commands.add(VoiceEditCommand.SetCrop(CropRatio.LANDSCAPE, "Rasio dipotong ke 16:9 Lebar."))
        }

        // 13. AI Badges
        if (text.contains("badge") || text.contains("stempel") || text.contains("4k")) {
            commands.add(VoiceEditCommand.AddBadge("AI HD 4K", "Badge AI HD 4K disematkan."))
        }

        // 14. Remove background keyword
        if (text.contains("hapus background") || text.contains("hapus latar") || text.contains("potong objek") || text.contains("transparan")) {
            commands.add(VoiceEditCommand.RemoveBackground("Perintah hapus latar belakang diterima."))
        }

        if (commands.isEmpty()) {
            commands.add(VoiceEditCommand.Unknown(spokenText))
        }

        return commands
    }
}
