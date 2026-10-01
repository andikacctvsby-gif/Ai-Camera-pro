package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GalleryRepository
import com.example.model.CapturedPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val repository = GalleryRepository(context)

    val photos: StateFlow<List<CapturedPhoto>> = repository.photos

    private val _selectedPhoto = MutableStateFlow<CapturedPhoto?>(null)
    val selectedPhoto: StateFlow<CapturedPhoto?> = _selectedPhoto.asStateFlow()

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadPhotos()
        }
    }

    fun selectPhoto(photo: CapturedPhoto) {
        _selectedPhoto.value = photo
        viewModelScope.launch {
            _selectedBitmap.value = repository.loadBitmap(photo.filePath)
        }
    }

    fun dismissDetail() {
        _selectedPhoto.value = null
        _selectedBitmap.value = null
    }

    fun deleteSelectedPhoto() {
        val photo = _selectedPhoto.value ?: return
        viewModelScope.launch {
            repository.deletePhoto(photo)
            _selectedPhoto.value = null
            _selectedBitmap.value = null
            _statusMessage.value = "Foto dihapus dari koleksi."
        }
    }

    fun shareSelectedPhoto() {
        val photo = _selectedPhoto.value ?: return
        repository.sharePhoto(photo)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
