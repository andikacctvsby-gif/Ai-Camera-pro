package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.camera.CameraScreen
import com.example.ui.editor.PhotoEditorScreen
import com.example.ui.gallery.GalleryScreen
import com.example.ui.magic.MagicStudioScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CameraViewModel
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.GalleryViewModel
import com.example.viewmodel.MagicStudioViewModel

enum class MainDestination(
    val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
) {
    CAMERA(R.string.nav_camera, Icons.Default.CameraAlt, "nav_item_camera"),
    MAGIC(R.string.nav_magic, Icons.Default.AutoFixHigh, "nav_item_magic"),
    EDITOR(R.string.nav_editor, Icons.Default.Tune, "nav_item_editor"),
    GALLERY(R.string.nav_gallery, Icons.Default.PhotoLibrary, "nav_item_gallery")
}

@Composable
fun MainScreen(
    cameraViewModel: CameraViewModel = viewModel(),
    magicViewModel: MagicStudioViewModel = viewModel(),
    editorViewModel: EditorViewModel = viewModel(),
    galleryViewModel: GalleryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(MainDestination.CAMERA) }

    // Custom back handler to return to camera screen
    BackHandler(enabled = currentDestination != MainDestination.CAMERA) {
        currentDestination = MainDestination.CAMERA
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = CyanNeon,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .background(DarkSurface)
                    .testTag("main_bottom_nav")
            ) {
                MainDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(id = destination.titleRes),
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(id = destination.titleRes),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBackground,
                            selectedTextColor = CyanNeon,
                            indicatorColor = CyanNeon,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(destination.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                MainDestination.CAMERA -> {
                    CameraScreen(
                        viewModel = cameraViewModel,
                        onNavigateToMagicStudio = { bitmap ->
                            magicViewModel.setWorkingBitmap(bitmap)
                            currentDestination = MainDestination.MAGIC
                        },
                        onNavigateToEditor = { bitmap ->
                            editorViewModel.setSourceBitmap(bitmap)
                            currentDestination = MainDestination.EDITOR
                        },
                        onNavigateToGallery = {
                            galleryViewModel.repository.apply { }
                            currentDestination = MainDestination.GALLERY
                        }
                    )
                }

                MainDestination.MAGIC -> {
                    MagicStudioScreen(
                        viewModel = magicViewModel,
                        onNavigateToEditor = { bitmap ->
                            editorViewModel.setSourceBitmap(bitmap)
                            currentDestination = MainDestination.EDITOR
                        }
                    )
                }

                MainDestination.EDITOR -> {
                    PhotoEditorScreen(
                        viewModel = editorViewModel,
                        onNavigateToGallery = {
                            currentDestination = MainDestination.GALLERY
                        }
                    )
                }

                MainDestination.GALLERY -> {
                    GalleryScreen(
                        viewModel = galleryViewModel,
                        onOpenInMagicStudio = { bitmap ->
                            magicViewModel.setWorkingBitmap(bitmap)
                            currentDestination = MainDestination.MAGIC
                        },
                        onOpenInEditor = { bitmap ->
                            editorViewModel.setSourceBitmap(bitmap)
                            currentDestination = MainDestination.EDITOR
                        },
                        onCaptureNew = {
                            currentDestination = MainDestination.CAMERA
                        }
                    )
                }
            }
        }
    }
}
