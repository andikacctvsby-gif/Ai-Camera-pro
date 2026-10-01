package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.MagicCutoutEngine
import com.example.ai.MagicEraserEngine
import com.example.ai.ObjectDetectionEngine
import com.example.data.GalleryRepository
import com.example.model.BackgroundOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MagicMode {
    REMOVE_BG,
    OBJECT_ERASER
}

class MagicStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val cutoutEngine = MagicCutoutEngine()
    private val eraserEngine = MagicEraserEngine()
    private val detectionEngine = ObjectDetectionEngine()
    private val galleryRepository = GalleryRepository(context)

    private val _currentMode = MutableStateFlow(MagicMode.REMOVE_BG)
    val currentMode: StateFlow<MagicMode> = _currentMode.asStateFlow()

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    // --- Background Removal State ---
    private val _cutoutBitmap = MutableStateFlow<Bitmap?>(null)
    val cutoutBitmap: StateFlow<Bitmap?> = _cutoutBitmap.asStateFlow()

    private val _compositedBitmap = MutableStateFlow<Bitmap?>(null)
    val compositedBitmap: StateFlow<Bitmap?> = _compositedBitmap.asStateFlow()

    private val _selectedBackground = MutableStateFlow<BackgroundOption>(BackgroundOption.Transparent)
    val selectedBackground: StateFlow<BackgroundOption> = _selectedBackground.asStateFlow()

    private val _enableDropShadow = MutableStateFlow(true)
    val enableDropShadow: StateFlow<Boolean> = _enableDropShadow.asStateFlow()

    private val _featherRadius = MutableStateFlow(4f)
    val featherRadius: StateFlow<Float> = _featherRadius.asStateFlow()

    // --- Object Eraser State ---
    private val _erasedBitmap = MutableStateFlow<Bitmap?>(null)
    val erasedBitmap: StateFlow<Bitmap?> = _erasedBitmap.asStateFlow()

    private val _maskBitmap = MutableStateFlow<Bitmap?>(null)
    val maskBitmap: StateFlow<Bitmap?> = _maskBitmap.asStateFlow()

    private val _brushSize = MutableStateFlow(32f) // 16f to 80f
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    // --- Common States ---
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _showOriginalComparison = MutableStateFlow(false)
    val showOriginalComparison: StateFlow<Boolean> = _showOriginalComparison.asStateFlow()

    init {
        // Load default sample portrait if none set
        val defaultSample = BitmapFactory.decodeResource(context.resources, R.drawable.sample_portrait)
        setWorkingBitmap(defaultSample)
    }

    fun setWorkingBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        _originalBitmap.value = bitmap
        _maskBitmap.value = eraserEngine.createEmptyMask(bitmap.width, bitmap.height)
        _erasedBitmap.value = null
        _cutoutBitmap.value = null
        _compositedBitmap.value = null

        // Auto extract foreground on loading
        performAutoCutout(bitmap)
    }

    fun setMode(mode: MagicMode) {
        _currentMode.value = mode
    }

    fun performAutoCutout(source: Bitmap? = _originalBitmap.value) {
        val bitmap = source ?: return
        _isProcessing.value = true
        _statusMessage.value = "AI memproses pemisahan latar belakang..."

        viewModelScope.launch {
            try {
                val detected = detectionEngine.detectObjects(bitmap)
                val primaryBox = detected.find { it.id == "obj_primary" }?.boundingBox

                val cutout = cutoutEngine.removeBackground(
                    sourceBitmap = bitmap,
                    featherRadius = _featherRadius.value,
                    subjectRect = primaryBox
                )
                _cutoutBitmap.value = cutout
                updateComposition()
                _statusMessage.value = "Latar belakang berhasil dihapus!"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Gagal menghapus latar belakang"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun setBackgroundOption(option: BackgroundOption) {
        _selectedBackground.value = option
        updateComposition()
    }

    fun toggleDropShadow() {
        _enableDropShadow.value = !_enableDropShadow.value
        updateComposition()
    }

    fun setFeatherRadius(radius: Float) {
        _featherRadius.value = radius
        performAutoCutout()
    }

    private fun updateComposition() {
        val cutout = _cutoutBitmap.value ?: return
        viewModelScope.launch {
            val sceneBitmap = when (val opt = _selectedBackground.value) {
                is BackgroundOption.PresetScene -> {
                    try {
                        BitmapFactory.decodeResource(context.resources, opt.drawableRes)
                    } catch (e: Exception) {
                        null
                    }
                }
                is BackgroundOption.CustomImage -> {
                    try {
                        val uri = android.net.Uri.parse(opt.uriString)
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> null
            }

            val composited = cutoutEngine.compositeWithBackground(
                foregroundCutout = cutout,
                backgroundOption = _selectedBackground.value,
                presetSceneBitmap = sceneBitmap,
                enableDropShadow = _enableDropShadow.value
            )
            _compositedBitmap.value = composited
        }
    }

    // --- Object Eraser Actions ---

    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }

    fun addMaskStroke(points: List<PointF>) {
        val mask = _maskBitmap.value ?: return
        if (points.isEmpty()) return

        val canvas = Canvas(mask)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = _brushSize.value
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
        }
        _maskBitmap.value = mask.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun triggerAutoDetectDistractions() {
        val source = _erasedBitmap.value ?: _originalBitmap.value ?: return
        _isProcessing.value = true
        _statusMessage.value = "AI mendeteksi gangguan di latar belakang..."

        viewModelScope.launch {
            try {
                val detected = detectionEngine.detectObjects(source)
                val autoMask = eraserEngine.createAutoDistractionMask(source, detected)
                _maskBitmap.value = autoMask
                _statusMessage.value = "Objek gangguan ditandai! Tekan 'Hapus Sekarang'."
            } catch (e: Exception) {
                _statusMessage.value = "Gagal mendeteksi gangguan otomatis"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun executeObjectEraser() {
        val source = _erasedBitmap.value ?: _originalBitmap.value ?: return
        val mask = _maskBitmap.value ?: return

        _isProcessing.value = true
        _statusMessage.value = "AI sedang menghapus objek dan mengisi tekstur..."

        viewModelScope.launch {
            try {
                val erased = eraserEngine.eraseObject(source, mask)
                _erasedBitmap.value = erased
                // Reset mask after successful inpaint
                _maskBitmap.value = eraserEngine.createEmptyMask(erased.width, erased.height)
                _statusMessage.value = "Objek berhasil dihapus tanpa jejak!"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Gagal menghapus objek"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun resetEraserMask() {
        val orig = _originalBitmap.value ?: return
        _maskBitmap.value = eraserEngine.createEmptyMask(orig.width, orig.height)
        _statusMessage.value = "Kuas mask di-reset"
    }

    fun resetEraserEntirely() {
        val orig = _originalBitmap.value ?: return
        _erasedBitmap.value = null
        _maskBitmap.value = eraserEngine.createEmptyMask(orig.width, orig.height)
        _statusMessage.value = "Kembali ke foto asli"
    }

    fun setShowComparison(show: Boolean) {
        _showOriginalComparison.value = show
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun getCurrentOutputBitmap(): Bitmap? {
        return if (_currentMode.value == MagicMode.REMOVE_BG) {
            _compositedBitmap.value ?: _cutoutBitmap.value
        } else {
            _erasedBitmap.value ?: _originalBitmap.value
        }
    }

    fun saveResultToGallery(onSaved: (String) -> Unit) {
        val bitmap = getCurrentOutputBitmap() ?: return
        val isPng = _currentMode.value == MagicMode.REMOVE_BG && _selectedBackground.value is BackgroundOption.Transparent

        viewModelScope.launch {
            val photo = galleryRepository.savePhoto(bitmap, isPng, "MAGIC")
            _statusMessage.value = "Hasil disimpan ke galeri!"
            onSaved(photo.filePath)
        }
    }
}
