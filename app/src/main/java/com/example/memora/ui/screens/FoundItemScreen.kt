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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * "GOT IT!" — shown after tapping "I FOUND IT!" on the item detail screen, confirming the item
 * that was just marked found before sending the user on to search for something else or home.
 */
@Composable
fun FoundItemScreen(
    itemName: String,
    photoPath: String?,
    onSearchAnotherClick: () -> Unit,
    onBackToHomeClick: () -> Unit
) {
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
            Spacer(modifier = if (landscape) Modifier.height(screenTopPadding()) else Modifier.weight(1.4f))

            FoundItemPhoto(
                photoPath = photoPath,
                contentDescription = itemName,
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(110.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GOT IT!",
                color = DarkGreen,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "YOU FOUND \u201C${itemName.uppercase()}\u201D",
                color = DarkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "KEEP YOUR SPACES ORGANIZED AND YOUR ITEMS\nEASY TO FIND.",
                color = SubTextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = if (landscape) Modifier.height(16.dp) else Modifier.weight(1f))

            MemoraPrimaryButton(
                text = "SEARCH ANOTHER",
                onClick = onSearchAnotherClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            MemoraOutlinedButton(
                text = "BACK TO HOME",
                onClick = onBackToHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = screenBottomPadding(28.dp))
            )
        }
    }
}

@Composable
private fun FoundItemPhoto(
    photoPath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
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
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = "NO PHOTO", color = SubTextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}