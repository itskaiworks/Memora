package com.example.memora.utils
import com.example.memora.data.*
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.R

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Memora's own in-app camera: a live preview, a shutter that saves straight into [PhotoStore], and
 * a flash (torch) toggle. Used both inline on the "CAPTURE WITH MEMORA" screen and, through
 * [MemoraCameraDialog], everywhere else the user chooses "Take a photo".
 */

// ---------------------------------------------------------------------------------- permission

/** Whether the CAMERA permission is held, and a way to ask for it. */
class CameraPermission internal constructor(
    val granted: Boolean,
    private val onRequest: () -> Unit
) {
    fun request() = onRequest()
}

@Composable
fun rememberCameraPermission(): CameraPermission {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    return CameraPermission(granted) { launcher.launch(Manifest.permission.CAMERA) }
}

// ------------------------------------------------------------------------------------- camera

/** Handle for the live camera: [capture] saves a photo, [toggleTorch] switches the flash light. */
class MemoraCamera internal constructor(
    internal val controller: LifecycleCameraController,
    private val context: Context
) {
    var torchOn by mutableStateOf(false)
        private set

    private var capturing = false

    fun toggleTorch() {
        torchOn = !torchOn
    }

    /** Takes a photo into a file this app owns and hands its absolute path to [onSaved]. */
    fun capture(onSaved: (String) -> Unit) {
        if (capturing) return
        capturing = true

        val file = PhotoStore.newPhotoFile(context)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        try {
            controller.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        capturing = false
                        if (file.length() > 0L) onSaved(file.absolutePath) else file.delete()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        capturing = false
                        file.delete()
                    }
                }
            )
        } catch (e: IllegalStateException) {
            // The camera has not finished starting yet; ignore the tap.
            capturing = false
            file.delete()
        }
    }
}

/**
 * Creates the camera and keeps it running only while [active] — so it is released as soon as the
 * screen shows a captured photo or is left, and the torch goes out with it.
 */
@Composable
fun rememberMemoraCamera(active: Boolean): MemoraCamera {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val camera = remember {
        MemoraCamera(
            controller = LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            },
            context = context
        )
    }

    DisposableEffect(camera, lifecycleOwner, active) {
        if (active) camera.controller.bindToLifecycle(lifecycleOwner)
        onDispose { camera.controller.unbind() }
    }

    // Apply the torch whenever it is toggled...
    val torchOn = camera.torchOn
    LaunchedEffect(torchOn, active) {
        if (active) camera.controller.enableTorch(torchOn)
    }

    // ...and again each time the camera (re)starts, since a fresh start always begins with it off.
    DisposableEffect(camera, lifecycleOwner, active) {
        if (!active) return@DisposableEffect onDispose { }

        val observer = Observer<Int> { state ->
            if ((state == TorchState.ON) != camera.torchOn) {
                camera.controller.enableTorch(camera.torchOn)
            }
        }
        camera.controller.torchState.observe(lifecycleOwner, observer)
        onDispose { camera.controller.torchState.removeObserver(observer) }
    }

    return camera
}

/** The live viewfinder. TextureView-backed, so rounded clipping and screen fades still work. */
@Composable
fun CameraPreview(camera: MemoraCamera, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = camera.controller
            }
        }
    )
}

// ------------------------------------------------------------------------------------ controls

/** Flash toggle, sized and laid out like the GALLERY button so the two line up. */
@Composable
fun CameraFlashToggle(on: Boolean, onClick: () -> Unit) {
    val tint = if (on) AccentBrown else DarkGreen
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(56.dp)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = if (on) Icons.Default.FlashOn else Icons.Default.FlashOff,
            contentDescription = if (on) "Turn flash off" else "Turn flash on",
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "FLASH", color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------------------------------- dialog

/**
 * Full-screen camera shown whenever the user picks "Take a photo" outside the capture screen: the
 * same dark viewfinder, flash toggle, shutter and gallery button. Tapping the shutter takes the
 * photo and closes; the gallery button closes this and opens the system photo picker.
 */
@Composable
fun MemoraCameraDialog(
    onDismiss: () -> Unit,
    onPhotoReady: (String) -> Unit,
    onGalleryClick: () -> Unit
) {
    MemoraDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val permission = rememberCameraPermission()
        val camera = rememberMemoraCamera(active = permission.granted)

        LaunchedEffect(Unit) {
            if (!permission.granted) permission.request()
        }

        AddFlowScaffold(title = "CAPTURE WITH MEMORA", onBackClick = onDismiss) {
            val landscape = isLandscape()
            // FIX (landscape): scroll together with a fixed-height viewfinder; portrait unchanged.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (landscape) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (landscape) Modifier.height(220.dp) else Modifier.weight(1f))
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp), clip = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF2A2A28)),
                    contentAlignment = Alignment.Center
                ) {
                    if (permission.granted) {
                        CameraPreview(camera = camera, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = FieldCream,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TAKE A PHOTO OF THE ITEM",
                                color = FieldCream,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CameraFlashToggle(on = camera.torchOn, onClick = { camera.toggleTorch() })

                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(DarkGreen)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(FieldCream)
                            .clickable {
                                if (permission.granted) {
                                    camera.capture { path -> onPhotoReady(path) }
                                } else {
                                    permission.request()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take photo",
                            tint = DarkGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { onGalleryClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Choose from gallery",
                            tint = DarkGreen,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "GALLERY", color = DarkGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(if (landscape) 16.dp else 48.dp))
            }
        }
    }
}