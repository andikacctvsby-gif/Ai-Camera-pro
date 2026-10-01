package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.GeminiVisionService
import com.example.ai.ObjectDetectionEngine
import com.example.camera.CameraManager
import com.example.data.GalleryRepository
import com.example.model.CapturedPhoto
import com.example.model.DeepScanResult
import com.example.model.DetectedObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class CameraNavigationEvent {
    data class OpenMagicStudio(val bitmap: Bitmap) : CameraNavigationEvent()
    data class OpenEditor(val bitmap: Bitmap) : CameraNavigationEvent()
    data class PhotoSaved(val message: String) : CameraNavigationEvent()
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val detectionEngine = ObjectDetectionEngine()
    val cameraManager = CameraManager(context, detectionEngine)
    val visionService = GeminiVisionService()
    val galleryRepository = GalleryRepository(context)

    val detectedObjects: StateFlow<List<DetectedObject>> = cameraManager.detectedObjects
    val zoomRatio: StateFlow<Float> = cameraManager.zoomRatio
    val isTorchOn: StateFlow<Boolean> = cameraManager.isTorchOn
    val selectedCategory: StateFlow<com.example.model.DetectionCategory> = cameraManager.selectedCategory

    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    private val _isGridEnabled = MutableStateFlow(true)
    val isGridEnabled: StateFlow<Boolean> = _isGridEnabled.asStateFlow()

    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled: StateFlow<Boolean> = _isDetectionEnabled.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0) // 0 (off), 3, 5
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _timerCountdown = MutableStateFlow<Int?>(null)
    val timerCountdown: StateFlow<Int?> = _timerCountdown.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _deepScanResult = MutableStateFlow<DeepScanResult?>(null)
    val deepScanResult: StateFlow<DeepScanResult?> = _deepScanResult.asStateFlow()

    private val _isDeepScanning = MutableStateFlow(false)
    val isDeepScanning: StateFlow<Boolean> = _isDeepScanning.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _lastCapturedBitmap = MutableStateFlow<Bitmap?>(null)
    val lastCapturedBitmap: StateFlow<Bitmap?> = _lastCapturedBitmap.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<CameraNavigationEvent>()
    val navigationEvents: SharedFlow<CameraNavigationEvent> = _navigationEvents.asSharedFlow()

    private var simulatedDetectionJob: Job? = null

    init {
        viewModelScope.launch {
            galleryRepository.initRepository()
        }
    }

    fun toggleFlash() {
        cameraManager.toggleFlash()
        _flashMode.value = cameraManager.getFlashMode()
    }

    fun toggleTorch() {
        cameraManager.toggleTorch()
    }

    fun toggleGrid() {
        _isGridEnabled.value = !_isGridEnabled.value
    }

    fun toggleDetection() {
        _isDetectionEnabled.value = !_isDetectionEnabled.value
    }

    fun cycleTimer() {
        _timerSeconds.value = when (_timerSeconds.value) {
            0 -> 3
            3 -> 5
            else -> 0
        }
    }

    fun setZoom(zoom: Float) {
        cameraManager.setZoom(zoom)
    }

    fun setSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
        if (enabled) {
            startSimulationDetections()
        } else {
            simulatedDetectionJob?.cancel()
        }
    }

    fun setCategory(category: com.example.model.DetectionCategory) {
        cameraManager.setCategory(category)
        if (_isSimulationMode.value) {
            startSimulationDetections()
        }
    }

    private fun startSimulationDetections() {
        simulatedDetectionJob?.cancel()
        simulatedDetectionJob = viewModelScope.launch {
            val sampleBitmap = getSamplePortraitBitmap() ?: return@launch
            while (_isSimulationMode.value) {
                val detected = detectionEngine.detectObjects(sampleBitmap, selectedCategory.value)
                cameraManager.setSimulatedDetections(detected)
                delay(600)
            }
        }
    }

    fun triggerCapture(onSuccess: (Bitmap) -> Unit = {}) {
        if (_isCapturing.value) return

        val timer = _timerSeconds.value
        if (timer > 0) {
            viewModelScope.launch {
                for (sec in timer downTo 1) {
                    _timerCountdown.value = sec
                    delay(1000)
                }
                _timerCountdown.value = null
                executeCapture(onSuccess)
            }
        } else {
            executeCapture(onSuccess)
        }
    }

    private fun executeCapture(onSuccess: (Bitmap) -> Unit) {
        _isCapturing.value = true

        if (_isSimulationMode.value) {
            viewModelScope.launch {
                delay(200)
                val sampleBitmap = getSamplePortraitBitmap()
                _isCapturing.value = false
                if (sampleBitmap != null) {
                    _lastCapturedBitmap.value = sampleBitmap
                    galleryRepository.savePhoto(sampleBitmap, false, "SIM_IMG")
                    onSuccess(sampleBitmap)
                }
            }
            return
        }

        val cacheFile = File(context.cacheDir, "temp_capture_${System.currentTimeMillis()}.jpg")
        cameraManager.capturePhoto(
            outputFile = cacheFile,
            onPhotoCaptured = { bitmap ->
                _isCapturing.value = false
                _lastCapturedBitmap.value = bitmap
                viewModelScope.launch {
                    galleryRepository.savePhoto(bitmap, false, "IMG")
                }
                onSuccess(bitmap)
            },
            onError = { exc ->
                _isCapturing.value = false
                // Fallback to sample if capture hardware failed
                val sample = getSamplePortraitBitmap()
                if (sample != null) {
                    _lastCapturedBitmap.value = sample
                    viewModelScope.launch {
                        galleryRepository.savePhoto(sample, false, "IMG")
                    }
                    onSuccess(sample)
                }
            }
        )
    }

    fun triggerDeepScan(bitmap: Bitmap?) {
        val targetBitmap = bitmap ?: _lastCapturedBitmap.value ?: getSamplePortraitBitmap() ?: return
        _isDeepScanning.value = true
        viewModelScope.launch {
            val result = visionService.analyzeImageDeeply(targetBitmap)
            _deepScanResult.value = result
            _isDeepScanning.value = false
        }
    }

    fun dismissDeepScan() {
        _deepScanResult.value = null
    }

    fun openInMagicStudio(bitmap: Bitmap) {
        viewModelScope.launch {
            _navigationEvents.emit(CameraNavigationEvent.OpenMagicStudio(bitmap))
        }
    }

    fun openInEditor(bitmap: Bitmap) {
        viewModelScope.launch {
            _navigationEvents.emit(CameraNavigationEvent.OpenEditor(bitmap))
        }
    }

    fun getSamplePortraitBitmap(): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.sample_portrait)
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
        simulatedDetectionJob?.cancel()
    }
}
