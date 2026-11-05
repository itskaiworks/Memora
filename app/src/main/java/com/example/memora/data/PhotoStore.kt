package com.example.memora.data
import com.example.memora.utils.*
import com.example.memora.R

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Where captured photos live on disk. Everything goes in the app's own external files dir, so the
 * photos are removed with the app and need no storage permission.
 */
object PhotoStore {

    private fun photosDir(context: Context): File =
        File(context.getExternalFilesDir(null), "photos").apply { mkdirs() }

    fun newPhotoFile(context: Context): File =
        File(photosDir(context), "IMG_${System.currentTimeMillis()}.jpg")

    /** The camera app can't write to our private dir directly — it needs a FileProvider uri. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Copies a gallery pick into our own directory. The picker's uri is a temporary grant, so
     * storing it in the database would leave a broken reference after a reboot.
     */
    fun importFrom(context: Context, source: Uri): File? = runCatching {
        val target = newPhotoFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target
    }.getOrNull()

    /** Deletes an image file from local storage if it exists. */
    fun deletePhoto(path: String?) {
        if (path.isNullOrEmpty()) return
        runCatching {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}

/** Handle returned by [rememberPhotoPicker] for triggering the camera or the gallery. */
class PhotoPicker internal constructor(
    private val onCapture: () -> Unit,
    private val onPick: () -> Unit
) {
    fun takePhoto() = onCapture()
    fun chooseFromGallery() = onPick()
}

/**
 * Wires up Memora's in-app camera and the system photo picker. [onPhotoReady] receives the
 * absolute path of a file this app owns, ready to be written straight into the database.
 *
 * "Take a photo" opens [MemoraCameraDialog] (live preview, flash toggle, shutter), which asks for
 * the CAMERA permission itself. The photo picker needs no permission: it hands back a single uri
 * the user explicitly chose.
 */
@Composable
fun rememberPhotoPicker(onPhotoReady: (String) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCamera by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val copied = withContext(Dispatchers.IO) { PhotoStore.importFrom(context, uri) }
                if (copied != null) onPhotoReady(copied.absolutePath)
            }
        }
    }

    val openGallery = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    if (showCamera) {
        MemoraCameraDialog(
            onDismiss = { showCamera = false },
            onPhotoReady = { path ->
                showCamera = false
                onPhotoReady(path)
            },
            onGalleryClick = {
                showCamera = false
                openGallery()
            }
        )
    }

    return remember(context) {
        PhotoPicker(
            onCapture = { showCamera = true },
            onPick = { openGallery() }
        )
    }
}