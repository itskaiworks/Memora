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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CheckGreen = Color(0xFF1B8A3C)

/**
 * The single confirmation screen behind "ITEM SAVED!", "ROOM SAVED" and "ROOM & STORAGE SAVED!".
 * A [SavedSummary] carrying two photos renders the paired room/storage layout.
 */
@Composable
fun SavedScreen(
    summary: SavedSummary,
    onDoneClick: () -> Unit
) {
    val dateText = remember(summary.savedAt) {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            .format(Date(summary.savedAt))
            .uppercase()
    }
    val timeText = remember(summary.savedAt) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(summary.savedAt))
    }

    // FIX (landscape): the content scrolls and the big weighted gaps become fixed small ones.
    val landscape = isLandscape()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_stripes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .then(if (landscape) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = if (landscape) Modifier.height(screenTopPadding()) else Modifier.weight(0.18f))

            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = CheckGreen,
                modifier = Modifier.size(if (landscape) 90.dp else 180.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = summary.heading,
                color = DarkGreen,
                // "ROOM & STORAGE SAVED!" needs to come down a size to stay on one line.
                fontSize = if (summary.heading.length > 14) 24.sp else 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (summary.paired) {
                PairedPhotos(
                    primary = summary.primaryPhoto,
                    secondary = summary.secondaryPhoto
                )
            } else {
                SavedPhoto(
                    photoPath = summary.primaryPhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (landscape) 150.dp else 210.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = summary.categoryLabel,
                    color = DarkGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(text = dateText, color = DarkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = breadcrumb(summary),
                    color = AccentBrown,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(text = timeText, color = DarkGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = if (landscape) Modifier.height(16.dp) else Modifier.weight(1f))

            MemoraOutlinedButton(
                text = "DONE",
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = screenBottomPadding(32.dp))
            )
        }
    }
}

/** "GARAGE | 1ST CABINET" — the room keeps its trailing bar even with no storage, as in the mockups. */
private fun breadcrumb(summary: SavedSummary): String = when {
    summary.roomName == null -> "UNFILED"
    summary.storageName == null -> "${summary.roomName}  |"
    else -> "${summary.roomName}  |  ${summary.storageName}"
}

@Composable
private fun PairedPhotos(primary: String?, secondary: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        SavedPhoto(photoPath = primary, modifier = Modifier.size(130.dp))
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "in",
            tint = Color.Black,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .size(36.dp)
        )
        SavedPhoto(photoPath = secondary, modifier = Modifier.size(130.dp))
    }
}

@Composable
private fun SavedPhoto(photoPath: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(elevation = 5.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE7E1D3)),
        contentAlignment = Alignment.Center
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = rememberPhotoModel(photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = "NO PHOTO", color = SubTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}