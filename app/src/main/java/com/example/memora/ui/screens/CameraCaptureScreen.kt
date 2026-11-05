package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Step 1 of the add-item flow (Figma: CAMERA PAGE) — "CAPTURE WITH MEMORA".
 *
 * The dark box is a live camera viewfinder: the shutter takes the photo straight from it and the
 * flash toggle on the left lights the torch. The library button opens the system photo picker via
 * [rememberPhotoPicker]. Either way the file is one this app owns and can store a path to. After a
 * capture the photo is shown in the box; tapping the shutter again brings the live camera back for
 * a retake. The photo is optional: NEXT stays enabled so an item can still be filed without one.
 */
@Composable
fun CameraCaptureScreen(
    photoPath: String?,
    onBackClick: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onNextClick: () -> Unit
) {
    val photoPicker = rememberPhotoPicker { path -> onPhotoCaptured(path) }

    val permission = rememberCameraPermission()
    // Live camera shows until there is a photo; the shutter flips back to it for a retake.
    var showLive by remember { mutableStateOf(photoPath == null) }
    val camera = rememberMemoraCamera(active = permission.granted && showLive)

    LaunchedEffect(Unit) {
        if (!permission.granted) permission.request()
    }
    // A photo from the camera or the gallery replaces the live view.
    LaunchedEffect(photoPath) {
        if (photoPath != null) showLive = false
    }

    AddFlowScaffold(title = "CAPTURE WITH MEMORA", onBackClick = onBackClick) {
        val landscape = isLandscape()
        // FIX (landscape): viewfinder + shutter row + NEXT can't share a short screen, so sideways
        // they scroll together and the viewfinder gets a fixed height. Portrait is unchanged.
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
                if (photoPath != null && !showLive) {
                    AsyncImage(
                        model = rememberPhotoModel(photoPath),
                        contentDescription = "Captured item",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (permission.granted) {
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
                            when {
                                !permission.granted -> permission.request()
                                showLive -> camera.capture { path -> onPhotoCaptured(path) }
                                else -> showLive = true
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
                        .clickable { photoPicker.chooseFromGallery() },
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

            Spacer(modifier = Modifier.height(20.dp))

            MemoraOutlinedButton(
                text = "NEXT",
                onClick = onNextClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = screenBottomPadding(32.dp))
            )
        }
    }
}