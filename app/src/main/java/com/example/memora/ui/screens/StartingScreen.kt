package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** How long the starting screen stays up before the app moves on to Home. */
private const val STARTING_SCREEN_MILLIS = 1_500L

/**
 * "STARTING SCREEN" — the app's launch splash: the Memora logo and wordmark on the same striped
 * background used elsewhere, shown for [STARTING_SCREEN_MILLIS] before [onFinished] is called.
 *
 * The wait is a coroutine `delay()`, not `Thread.sleep()`, so it pauses only this composable's
 * effect and never blocks the main thread or freezes the rest of the app while it's on screen.
 * There is nothing to tap here — Memora has no accounts to log into, so this screen always leads
 * straight to Home.
 */
@Composable
fun StartingScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(STARTING_SCREEN_MILLIS)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_stripes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_memora),
                contentDescription = "Memora Logo",
                modifier = Modifier.size(if (isLandscape()) 110.dp else 170.dp)
            )

            Text(
                text = "MEMORA",
                color = DarkGreen,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.offset(y = (-12).dp)
            )
        }
    }
}