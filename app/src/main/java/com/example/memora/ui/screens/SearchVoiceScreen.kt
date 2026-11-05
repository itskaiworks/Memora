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
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "SPEAK" — the voice half of the search step.
 *
 * This screen used to be a dead end: it animated a waveform and never called [onQueryCaptured], so
 * the only way out was Back. The mic now launches the system speech recognizer and hands the
 * transcript on to the results screen; where no recognizer exists (many emulators), it says so and
 * offers the typed search instead.
 */
@Composable
fun SearchVoiceScreen(
    onBackClick: () -> Unit = {},
    onQueryCaptured: (String) -> Unit = {},
    onTypeInsteadClick: () -> Unit = {}
) {
    var isListening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("TAP THE MIC TO SPEAK") }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        when {
            result.resultCode != Activity.RESULT_OK -> status = "DIDN'T CATCH THAT — TAP TO TRY AGAIN"
            spoken.isNullOrEmpty() -> status = "DIDN'T CATCH THAT — TAP TO TRY AGAIN"
            else -> onQueryCaptured(spoken)
        }
    }

    val startListening: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "What are you looking for?")
        }
        try {
            isListening = true
            status = "LISTENING..."
            speechLauncher.launch(intent)
        } catch (notInstalled: ActivityNotFoundException) {
            isListening = false
            status = "VOICE SEARCH ISN'T AVAILABLE ON THIS DEVICE"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_stripes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = screenTopPadding())
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
                    text = "SEARCH",
                    color = AccentBrown,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // FIX (landscape): the mic, wave and button no longer fit the short screen.
                    .then(if (isLandscape()) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(if (isListening) Color(0xFF2FA84F) else PrimaryButtonContainer)
                            .clickable { startListening() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isListening) "LISTENING..." else "TAP TO SPEAK",
                        color = DarkGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AudioWave(isListening = isListening)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = status,
                        color = SubTextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    MemoraOutlinedButton(
                        text = "TYPE INSTEAD",
                        onClick = onTypeInsteadClick,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWave(isListening: Boolean) {
    val waveColor = DarkGreen
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavePhase"
    )

    Canvas(modifier = Modifier.size(width = 160.dp, height = 32.dp)) {
        val barCount = 18
        val barWidth = 4.dp.toPx()
        val gap = (size.width - barCount * barWidth) / (barCount - 1)
        for (i in 0 until barCount) {
            val heightFactor = if (isListening) {
                val wobble = kotlin.math.sin((i * 0.9f) + phase * 6.28f)
                0.3f + 0.7f * ((wobble + 1f) / 2f)
            } else {
                0.25f
            }
            val barHeight = size.height * heightFactor
            val x = i * (barWidth + gap)
            drawLine(
                color = waveColor,
                start = Offset(x + barWidth / 2, size.height / 2 - barHeight / 2),
                end = Offset(x + barWidth / 2, size.height / 2 + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}