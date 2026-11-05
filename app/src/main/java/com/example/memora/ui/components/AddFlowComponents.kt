package com.example.memora.ui.components
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.theme.*

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImage
import com.example.memora.data.SAMPLE_PHOTO_PREFIX
import java.io.File

/**
 * photoPath holds one of two things: a "sample:<name>" marker for one of the images bundled in
 * res/drawable, or an absolute file path for a camera capture. Samples resolve to a drawable id
 * (which Coil loads directly); file paths go through Uri.fromFile, which does the escaping a
 * hand-assembled file:// string gets wrong.
 */
@Composable
fun rememberPhotoModel(path: String): Any = remember(path) {
    if (path.startsWith(SAMPLE_PHOTO_PREFIX)) {
        sampleDrawableId(path.removePrefix(SAMPLE_PHOTO_PREFIX))
    } else {
        Uri.fromFile(File(path)).toString()
    }
}

/** Resolved by name so the database never has to store a build-specific resource id. */
private fun sampleDrawableId(name: String): Int = when (name) {
    "house_keys_photo" -> R.drawable.house_keys_photo
    "passport_photo" -> R.drawable.passport_photo
    "car_keys_photo" -> R.drawable.car_keys_photo
    "birth_certificate_photo" -> R.drawable.birth_certificate_photo
    "living_room_photo" -> R.drawable.living_room_photo
    "garage_photo" -> R.drawable.garage_photo
    "bedroom_photo" -> R.drawable.bedroom_photo
    "kitchen_photo" -> R.drawable.kitchen_photo
    "balcony_photo" -> R.drawable.balcony_photo
    "office_photo" -> R.drawable.office_photo
    "top_drawer_photo" -> R.drawable.top_drawer_photo
    "toolbox_photo" -> R.drawable.toolbox_photo
    "first_cabinet_photo" -> R.drawable.first_cabinet_photo
    else -> R.drawable.logo_memora
}

/** Striped background plus a centered title and back arrow — the shell every add screen sits in. */
@Composable
fun AddFlowScaffold(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_stripes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(screenTopPadding(50.dp)))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DarkGreen,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onBackClick() }
                )
                Text(
                    text = title,
                    color = DarkGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(28.dp))
            }

            content()
        }
    }
}

/**
 * The rounded photo well at the top of every add screen. Tapping it opens the camera or gallery
 * chooser; with no photo yet it shows a prompt instead.
 */
@Composable
fun PhotoWell(
    photoPath: String?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    Box(
        modifier = modifier
            .size(if (isLandscape()) minOf(size, 150.dp) else size)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE7E1D3))
            .clickable { onClick() },
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = DarkGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ADD A PHOTO",
                    color = DarkGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Gives the dialog's window Memora's own enter/exit animation (a short fade + gentle scale, with
 * the dim layer fading along with it) instead of the abrupt platform default. Purely a window
 * animation: it changes nothing about the dialog's layout, content or behaviour.
 */
@Composable
fun MemoraDialogWindowAnimation() {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window
            ?.setWindowAnimations(R.style.Animation_Memora_Dialog)
    }
}

/** Drop-in replacement for [Dialog] that opens and closes with [MemoraDialogWindowAnimation]. */
@Composable
fun MemoraDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        MemoraDialogWindowAnimation()
        content()
    }
}

/** Camera-or-gallery chooser, shown when the photo well is tapped. */
@Composable
fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FieldCream,
        title = { Text(text = "ADD A PHOTO", color = DarkGreen, fontWeight = FontWeight.Bold) },
        text = {
            MemoraDialogWindowAnimation()
            Column {
                PhotoSourceRow(Icons.Default.PhotoCamera, "Take a photo") {
                    onDismiss()
                    onTakePhoto()
                }
                Spacer(modifier = Modifier.height(4.dp))
                PhotoSourceRow(Icons.Default.PhotoLibrary, "Choose from gallery") {
                    onDismiss()
                    onChooseFromGallery()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = AccentBrown) }
        }
    )
}

@Composable
private fun PhotoSourceRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DarkGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = if (isSystemInDarkTheme()) DarkGreen else Color.DarkGray, fontSize = 14.sp)
    }
}

@Composable
fun SuggestionsSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "SUGGESTIONS",
            color = AccentBrown,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(text = suggestion, onClick = { onSuggestionClick(suggestion) })
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(FieldCream)
            .border(1.dp, AccentBrown, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text = text, color = AccentBrown, fontSize = 13.sp)
    }
}

/** Filled dark-green / emerald button — the SAVE in a two-button row. */
@Composable
fun MemoraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = PrimaryButtonContainer,
        disabledContainerColor = PrimaryButtonContainer.copy(alpha = 0.35f),
        disabledContentColor = Color.White
    )
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        modifier = modifier.height(if (isLandscape()) 48.dp else 64.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

/** Cream button with a green outline — the standalone SAVE, DONE, and the chaining actions. */
@Composable
fun MemoraOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = FieldCream,
            contentColor = DarkGreen,
            disabledContainerColor = FieldCream,
            disabledContentColor = DarkGreen.copy(alpha = 0.38f)
        ),
        border = BorderStroke(1.dp, if (enabled) DarkGreen else DarkGreen.copy(alpha = 0.38f)),
        modifier = modifier.height(if (isLandscape()) 48.dp else 64.dp)
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

/** The big white name field on the add screens (white fill + green outline, per the mockups). */
@Composable
fun MemoraNameField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = placeholder, color = DarkGreen.copy(alpha = 0.6f), fontSize = 17.sp)
        },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = DarkGreen, fontSize = 17.sp),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground,
            focusedBorderColor = CardStroke,
            unfocusedBorderColor = CardStroke,
            focusedTextColor = DarkGreen,
            unfocusedTextColor = DarkGreen
        ),
        modifier = modifier.height(72.dp)
    )
}

/**
 * Cream pill with a chevron, used to pick the room (and storage) an item belongs to. Built from a
 * Box plus DropdownMenu rather than ExposedDropdownMenuBox so it can carry the mockup styling.
 */
@Composable
fun <T> MemoraDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    emptyHint: String = "Nothing to choose yet"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(30.dp), clip = false)
                .clip(RoundedCornerShape(30.dp))
                .background(FieldCream)
                .border(1.dp, DarkGreen, RoundedCornerShape(30.dp))
                .clickable { expanded = true }
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = selected?.let(optionLabel)?.uppercase() ?: label,
                color = DarkGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = DarkGreen,
                modifier = Modifier.size(28.dp)
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FieldCream)
        ) {
            if (options.isEmpty()) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(emptyHint, color = SubTextGray, fontSize = 14.sp) },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel(option).uppercase(),
                                color = DarkGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/** Starting points offered on each add screen, so an empty database is still usable. */
object Suggestions {
    val ROOMS = listOf(
        "Living Room", "Bedroom", "Kitchen", "Garage",
        "Office", "Bathroom", "Balcony", "Storage Room"
    )
    val STORAGES = listOf(
        "1st Cabinet", "Top Drawer", "Toolbox", "Closet",
        "Shelf", "Nightstand", "Filing Cabinet", "Safe"
    )
    val ITEMS = listOf(
        "House Keys", "Motorcycle Keys", "Padlock Keys", "Passport",
        "Birth Certificate", "Car Keys", "Ballpens", "Certificate of Land Title"
    )
}


private val WarningRed = Color(0xFFE53935)
private val ConfirmGreen = Color(0xFF1B8A3C)

@Composable
fun MemoraConfirmCard(message: String, onNo: () -> Unit, onYes: () -> Unit) {
    MemoraDialog(onDismissRequest = onNo) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningRed,
                modifier = Modifier.size(78.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = DarkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onNo,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldCream, contentColor = DarkGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(text = "NO", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onYes,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(text = "YES", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MemoraResultCard(message: String, onDone: () -> Unit) {
    // Not dismissible by tapping outside: DONE is what moves the flow on, so a stray tap should
    // not leave the user on a stale form for an item that no longer exists.
    MemoraDialog(
        onDismissRequest = onDone,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = ConfirmGreen,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                color = DarkGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            MemoraOutlinedButton(
                text = "DONE",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MemoraInfoCard(message: String, buttonText: String = "OKAY", onOkay: () -> Unit) {
    MemoraDialog(
        onDismissRequest = onOkay,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningRed,
                modifier = Modifier.size(78.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = DarkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onOkay,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StorageArchiveDialog(
    storageName: String,
    onDismiss: () -> Unit,
    onArchiveWithItems: () -> Unit,
    onArchiveStorageOnly: () -> Unit
) {
    MemoraDialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningRed,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "ARCHIVE STORAGE \"${storageName.uppercase()}\"?",
                color = DarkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This storage contains items. Would you also like to archive the items inside it?",
                color = SubTextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onArchiveWithItems,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(text = "YES, ARCHIVE ITEMS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = onArchiveStorageOnly,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldCream, contentColor = DarkGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(text = "NO, STORAGE ONLY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StorageRestoreDialog(
    storageName: String,
    onDismiss: () -> Unit,
    onRestoreWithItems: () -> Unit,
    onRestoreStorageOnly: () -> Unit
) {
    MemoraDialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .border(1.dp, CardStroke, RoundedCornerShape(18.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = null,
                tint = DarkGreen,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "RESTORE STORAGE \"${storageName.uppercase()}\"?",
                color = DarkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Would you like to restore all items that were previously stored here as well?",
                color = SubTextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onRestoreWithItems,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(text = "YES, RESTORE ITEMS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = onRestoreStorageOnly,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldCream, contentColor = DarkGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(text = "NO, STORAGE ONLY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}