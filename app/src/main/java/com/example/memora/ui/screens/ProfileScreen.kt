package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private data class ProfileMenuEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

private val menuEntries = listOf(
    ProfileMenuEntry("MY ROOMS", "Manage your rooms", Icons.Default.MeetingRoom, PlaceholderWalnut),
    ProfileMenuEntry("MY STORAGE", "Manage storage locations", Icons.Default.Inventory2, PlaceholderTan),
    ProfileMenuEntry("FAVORITES", "Your favorite items", Icons.Default.Star, PlaceholderCamel)
)

/**
 * "PROFILE" screen.
 *
 * [username] and [photoPath] are owned by the caller (backed by the profile table, see
 * [MemoraViewModel.profile]) so an edited name or photo survives navigating away and coming back,
 * the same way a saved room or item would. An empty [username] means nothing has been saved yet,
 * which is rendered as the "Nickname" placeholder rather than stored as a real value.
 */
@Composable
fun ProfileScreen(
    username: String = "",
    photoPath: String? = null,
    onUsernameChange: (String) -> Unit = {},
    onPhotoChanged: (String) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onAllItemsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onMyRoomsClick: () -> Unit = {},
    onMyStorageClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {}
) {
    // FIX: rememberSaveable so the photo dialog / nickname editing survive rotation.
    var showPhotoSources by rememberSaveable { mutableStateOf(false) }
    var isEditingUsername by rememberSaveable { mutableStateOf(false) }
    val photoPicker = rememberPhotoPicker { path -> onPhotoChanged(path) }

    if (showPhotoSources) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSources = false },
            onTakePhoto = { photoPicker.takePhoto() },
            onChooseFromGallery = { photoPicker.chooseFromGallery() }
        )
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
                Text(
                    text = "PROFILE",
                    color = DarkGreen,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            // FIX: the photo, nickname and menu scroll (title and bottom nav stay pinned), like
            // the other screens — otherwise rotating to landscape clips the menu and the nav bar.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfilePhoto(
                        photoPath = photoPath,
                        onClick = { showPhotoSources = true },
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                    ) {
                        EditableUsernameField(
                            value = username,
                            onValueChange = onUsernameChange,
                            isEditing = isEditingUsername,
                            onStartEditing = { isEditingUsername = true },
                            onStopEditing = { isEditingUsername = false },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit nickname",
                            tint = DarkGreen.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { isEditingUsername = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    menuEntries.forEach { entry ->
                        val onClick = when (entry.title) {
                            "MY ROOMS" -> onMyRoomsClick
                            "MY STORAGE" -> onMyStorageClick
                            else -> onFavoritesClick
                        }
                        ProfileMenuRow(entry = entry, onClick = onClick)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    HorizontalDivider(color = CardStroke)
                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileMenuRow(
                        entry = ProfileMenuEntry("ARCHIVE", "View archived items", Icons.Default.Archive, DarkGreen),
                        onClick = onArchiveClick,
                        iconIsOutline = true
                    )
                }
            }

            MemoraBottomNavBar(
                selectedTab = BottomNavTab.PROFILE,
                onHomeClick = onHomeClick,
                onAllItemsClick = onAllItemsClick,
                onHistoryClick = onHistoryClick,
                onProfileClick = {},
                onAddClick = onAddClick
            )
        }
    }
}

/** Circular profile photo. Only the small camera badge over its bottom-right edge opens the
 *  take-photo/choose-from-gallery dialog used when adding a room or storage — tapping the photo
 *  itself does nothing, so it can't be mistaken for a way to view/zoom it. Falls back to a plain
 *  placeholder circle until a photo is set. */
@Composable
private fun ProfilePhoto(photoPath: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(PlaceholderTan)
        ) {
            if (photoPath != null) {
                AsyncImage(
                    model = rememberPhotoModel(photoPath),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, Color.Black, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Edit photo",
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Username shown as plain centered text by default — tapping it (or the pencil icon next to it)
 * is what turns it into an editable field, rather than it always looking/behaving like one.
 * Editing ends (and the state already persisted via [onValueChange] on every keystroke stays)
 * when the field loses focus or the user taps the keyboard's Done key. Shows "Nickname" whenever
 * nothing has been saved yet ([value] empty) — a normal placeholder, not a real stored value.
 */
@Composable
private fun EditableUsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    onStartEditing: () -> Unit,
    onStopEditing: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isEditing) {
        var localText by remember { mutableStateOf(value) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        // onFocusChanged fires once immediately on composition, while the field is still
        // unfocused (requestFocus() below hasn't landed yet). Without this guard that first,
        // "not focused yet" callback would itself call onStopEditing() and flip straight back
        // to the Text before the user can type anything.
        var hasFocused by remember { mutableStateOf(false) }

        BasicTextField(
            value = localText,
            onValueChange = { newText ->
                localText = newText
                onValueChange(newText)
            },
            singleLine = true,
            textStyle = TextStyle(
                color = DarkGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Start
            ),
            cursorBrush = SolidColor(DarkGreen),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                onStopEditing()
            }),
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        hasFocused = true
                    } else if (hasFocused) {
                        onStopEditing()
                    }
                },
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.defaultMinSize(minWidth = 80.dp)
                ) {
                    if (localText.isEmpty()) {
                        Text(
                            text = "Nickname",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkGreen.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Grabs the keyboard the moment editing starts, since tapping in isn't a normal text
        // field tap here — it's a Text swapping itself out for this field on the same frame.
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    } else {
        Text(
            text = value.ifEmpty { "Nickname" },
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (value.isEmpty()) DarkGreen.copy(alpha = 0.5f) else DarkGreen,
            modifier = modifier.clickable { onStartEditing() }
        )
    }
}

@Composable
private fun ProfileMenuRow(
    entry: ProfileMenuEntry,
    onClick: () -> Unit,
    iconIsOutline: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        if (iconIsOutline) {
            Icon(
                imageVector = entry.icon,
                contentDescription = entry.title,
                tint = entry.color,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(entry.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = entry.title,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
            Text(text = entry.subtitle, fontSize = 10.sp, color = SubTextGray)
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SubTextGray
        )
    }
}