package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.CapturedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GalleryRepository(private val context: Context) {

    private val _photos = MutableStateFlow<List<CapturedPhoto>>(emptyList())
    val photos: StateFlow<List<CapturedPhoto>> = _photos.asStateFlow()

    private val picturesDir: File = File(context.filesDir, "ai_cam_photos").apply {
        if (!exists()) mkdirs()
    }

    suspend fun initRepository() = withContext(Dispatchers.IO) {
        loadPhotos()
    }

    suspend fun loadPhotos() = withContext(Dispatchers.IO) {
        val files = picturesDir.listFiles { file ->
            file.extension.lowercase(Locale.ROOT) in listOf("jpg", "jpeg", "png")
        } ?: emptyArray()

        val list = files.mapNotNull { file ->
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, opts)
                CapturedPhoto(
                    id = file.nameWithoutExtension,
                    filePath = file.absolutePath,
                    timestamp = file.lastModified(),
                    width = opts.outWidth,
                    height = opts.outHeight,
                    tags = listOf("AI Cam"),
                    isEdited = file.name.contains("edited") || file.name.contains("magic")
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }

        _photos.value = list
    }

    suspend fun savePhoto(
        bitmap: Bitmap,
        isPng: Boolean = false,
        prefix: String = "IMG"
    ): CapturedPhoto = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val ext = if (isPng) "png" else "jpg"
        val fileName = "${prefix}_${timeStamp}_${UUID.randomUUID().toString().take(4)}.$ext"
        val file = File(picturesDir, fileName)

        FileOutputStream(file).use { out ->
            if (isPng) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }

        val photo = CapturedPhoto(
            id = file.nameWithoutExtension,
            filePath = file.absolutePath,
            timestamp = file.lastModified(),
            width = bitmap.width,
            height = bitmap.height,
            tags = listOf("AI Cam Pro"),
            isEdited = prefix != "IMG"
        )

        loadPhotos()
        photo
    }

    suspend fun deletePhoto(photo: CapturedPhoto) = withContext(Dispatchers.IO) {
        val file = File(photo.filePath)
        if (file.exists()) {
            file.delete()
        }
        loadPhotos()
    }

    suspend fun loadBitmap(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(filePath)
        } catch (e: Exception) {
            null
        }
    }

    fun sharePhoto(photo: CapturedPhoto) {
        try {
            val file = File(photo.filePath)
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (photo.filePath.endsWith(".png")) "image/png" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Dibuat dengan AI Cam Pro ✦")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Foto").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
