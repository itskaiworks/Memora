package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.memora.data.ItemWithLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ItemDetailScreen(
    entry: ItemWithLocation,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onFoundClick: () -> Unit = {},
    onPhotoUpdated: (String) -> Unit = {}
) {
    val item = entry.item
    var showPhotoSources by remember { mutableStateOf(false) }
    val photoPicker = rememberPhotoPicker { path -> onPhotoUpdated(path) }

    val lastRecorded = remember(item.lastUpdatedAt) {
        SimpleDateFormat("MMMM d, yyyy - h:mm a", Locale.getDefault())
            .format(Date(item.lastUpdatedAt))
    }

    if (showPhotoSources) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSources = false },
            onTakePhoto = { photoPicker.takePhoto() },
            onChooseFromGallery = { photoPicker.chooseFromGallery() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_stripes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(screenTopPadding(50.dp)))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DarkGreen,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBackClick() }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = item.name.uppercase(),
                    color = DarkGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isSystemInDarkTheme()) DarkGreen else if (item.isFavorite) Color(0xFF3A3A3A) else SubTextGray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onToggleFavorite() }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                val currentPhotoPath = item.photoPath
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape()) 220.dp else 320.dp)
                        .photoShadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlaceholderSand)
                        .clickable { showPhotoSources = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPhotoPath != null) {
                        AsyncImage(
                            model = rememberPhotoModel(currentPhotoPath),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(text = "NO PHOTO", color = SubTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    RetakePhotoButton(
                        onClick = { showPhotoSources = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSystemInDarkTheme()) CardBackground else FieldCream)
                        .border(if (isSystemInDarkTheme()) 1.dp else 2.dp, if (isSystemInDarkTheme()) CardStroke else DarkGreen, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = (item.description ?: "No note for this item yet.").uppercase(),
                        fontSize = 13.sp,
                        color = DarkGreen
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LocationTag(entry.roomName?.uppercase() ?: "UNFILED")
                    if (entry.storageName != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = DarkGreen,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        LocationTag(entry.storageName.uppercase())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "LAST RECORDED", fontSize = 11.sp, color = SubTextGray, fontWeight = FontWeight.Bold)
                Text(text = lastRecorded, fontSize = 14.sp, color = DarkGreen, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = screenBottomPadding(24.dp))
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = FieldCream,
                        contentColor = if (isSystemInDarkTheme()) DarkGreen else Color.DarkGray
                    ),
                    border = BorderStroke(1.dp, DarkGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(text = "EDIT", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onFoundClick,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryButtonContainer),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "I FOUND IT!",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FieldCream)
            .border(2.dp, DarkGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)
    }
}

@Composable
private fun RetakePhotoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(AccentBrown)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "RETAKE PHOTO",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}