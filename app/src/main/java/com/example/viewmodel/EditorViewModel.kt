package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.PhotoFilterEngine
import com.example.data.GalleryRepository
import com.example.model.CropRatio
import com.example.model.EditAdjustmentState
import com.example.model.FilterPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EditorTab {
    FILTERS,
    ADJUST,
    CROP_TRANSFORM,
    OVERLAYS
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val filterEngine = PhotoFilterEngine()
    private val voiceParser = com.example.ai.VoiceEditParser()
    private val galleryRepository = GalleryRepository(context)

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _currentTab = MutableStateFlow(EditorTab.FILTERS)
    val currentTab: StateFlow<EditorTab> = _currentTab.asStateFlow()

    private val _editState = MutableStateFlow(EditAdjustmentState())
    val editState: StateFlow<EditAdjustmentState> = _editState.asStateFlow()

    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    private val _showCompareOriginal = MutableStateFlow(false)
    val showCompareOriginal: StateFlow<Boolean> = _showCompareOriginal.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var renderJob: Job? = null

    init {
        val sample = BitmapFactory.decodeResource(context.resources, R.drawable.sample_portrait)
        setSourceBitmap(sample)
    }

    fun setSourceBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        _originalBitmap.value = bitmap
        _previewBitmap.value = bitmap
        _editState.value = EditAdjustmentState()
        scheduleRender()
    }

    fun setTab(tab: EditorTab) {
        _currentTab.value = tab
    }

    fun setFilter(preset: FilterPreset) {
        _editState.value = _editState.value.copy(activeFilter = preset)
        scheduleRender()
    }

    fun updateFilterIntensity(value: Float) {
        _editState.value = _editState.value.copy(filterIntensity = value)
        scheduleRender()
    }

    fun updateBrightness(value: Float) {
        _editState.value = _editState.value.copy(brightness = value)
        scheduleRender()
    }

    fun updateContrast(value: Float) {
        _editState.value = _editState.value.copy(contrast = value)
        scheduleRender()
    }

    fun updateSaturation(value: Float) {
        _editState.value = _editState.value.copy(saturation = value)
        scheduleRender()
    }

    fun updateWarmth(value: Float) {
        _editState.value = _editState.value.copy(warmth = value)
        scheduleRender()
    }

    fun updateSharpness(value: Float) {
        _editState.value = _editState.value.copy(sharpness = value)
        scheduleRender()
    }

    fun updateVignette(value: Float) {
        _editState.value = _editState.value.copy(vignette = value)
        scheduleRender()
    }

    fun updateBokeh(value: Float) {
        _editState.value = _editState.value.copy(bokehBlur = value)
        scheduleRender()
    }

    fun rotate90() {
        val nextRotation = (_editState.value.rotationDegrees + 90f) % 360f
        _editState.value = _editState.value.copy(rotationDegrees = nextRotation)
        scheduleRender()
    }

    fun toggleFlipHorizontal() {
        _editState.value = _editState.value.copy(isFlippedHorizontally = !_editState.value.isFlippedHorizontally)
        scheduleRender()
    }

    fun setCropRatio(ratio: CropRatio) {
        _editState.value = _editState.value.copy(cropRatio = ratio)
        scheduleRender()
    }

    fun setWatermarkText(text: String) {
        _editState.value = _editState.value.copy(watermarkText = text)
        scheduleRender()
    }

    fun setBadge(badge: String?) {
        _editState.value = _editState.value.copy(selectedBadge = badge)
        scheduleRender()
    }

    fun applyAutoEnhance() {
        _editState.value = _editState.value.copy(
            brightness = 10f,
            contrast = 15f,
            saturation = 18f,
            warmth = 5f,
            sharpness = 25f,
            vignette = 12f
        )
        _statusMessage.value = "Magic Auto-Tuning berhasil diterapkan!"
        scheduleRender()
    }

    fun resetAdjustments() {
        _editState.value = EditAdjustmentState()
        _statusMessage.value = "Pengaturan dikembalikan ke awal"
        scheduleRender()
    }

    fun executeVoiceCommand(spokenText: String) {
        val commands = voiceParser.parse(spokenText)
        val feedbackList = mutableListOf<String>()

        for (cmd in commands) {
            when (cmd) {
                is com.example.ai.VoiceEditCommand.ApplyFilter -> {
                    _editState.value = _editState.value.copy(
                        activeFilter = cmd.preset,
                        filterIntensity = cmd.intensity
                    )
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.AdjustBrightness -> {
                    val next = (_editState.value.brightness + cmd.value).coerceIn(-100f, 100f)
                    _editState.value = _editState.value.copy(brightness = next)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.AdjustContrast -> {
                    val next = (_editState.value.contrast + cmd.value).coerceIn(-100f, 100f)
                    _editState.value = _editState.value.copy(contrast = next)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.AdjustSaturation -> {
                    val next = (_editState.value.saturation + cmd.value).coerceIn(-100f, 100f)
                    _editState.value = _editState.value.copy(saturation = next)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.AdjustWarmth -> {
                    val next = (_editState.value.warmth + cmd.value).coerceIn(-100f, 100f)
                    _editState.value = _editState.value.copy(warmth = next)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.SetBokeh -> {
                    _editState.value = _editState.value.copy(bokehBlur = cmd.value)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.SetVignette -> {
                    _editState.value = _editState.value.copy(vignette = cmd.value)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.SetSharpness -> {
                    _editState.value = _editState.value.copy(sharpness = cmd.value)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.SetCrop -> {
                    _editState.value = _editState.value.copy(cropRatio = cmd.ratio)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.Rotate -> {
                    val next = (_editState.value.rotationDegrees + 90f) % 360f
                    _editState.value = _editState.value.copy(rotationDegrees = next)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.Flip -> {
                    _editState.value = _editState.value.copy(isFlippedHorizontally = !_editState.value.isFlippedHorizontally)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.AutoEnhance -> {
                    applyAutoEnhance()
                    return
                }
                is com.example.ai.VoiceEditCommand.Reset -> {
                    resetAdjustments()
                    return
                }
                is com.example.ai.VoiceEditCommand.AddBadge -> {
                    _editState.value = _editState.value.copy(selectedBadge = cmd.badge)
                    feedbackList.add(cmd.message)
                }
                is com.example.ai.VoiceEditCommand.RemoveBackground -> {
                    feedbackList.add("Gunakan Magic Studio untuk pemotongan latar belakang.")
                }
                is com.example.ai.VoiceEditCommand.Unknown -> {
                    feedbackList.add("Perintah tidak dikenali: \"${cmd.rawText}\"")
                }
            }
        }

        if (feedbackList.isNotEmpty()) {
            _statusMessage.value = feedbackList.joinToString(" • ")
        }
        scheduleRender()
    }

    fun setShowCompareOriginal(show: Boolean) {
        _showCompareOriginal.value = show
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun scheduleRender() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            delay(50) // Debounce for super responsive slider gestures
            val original = _originalBitmap.value ?: return@launch
            _isRendering.value = true
            try {
                val processed = filterEngine.processImage(original, _editState.value)
                _previewBitmap.value = processed
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRendering.value = false
            }
        }
    }

    fun saveEditedPhoto(onSaved: (String) -> Unit) {
        val bitmap = _previewBitmap.value ?: return
        viewModelScope.launch {
            val photo = galleryRepository.savePhoto(bitmap, false, "EDIT")
            _statusMessage.value = "Foto berhasil disimpan ke galeri!"
            onSaved(photo.filePath)
        }
    }
}
