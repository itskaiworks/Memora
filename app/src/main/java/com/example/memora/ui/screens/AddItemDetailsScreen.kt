package com.example.memora.ui.screens
import com.example.memora.viewmodel.*
import com.example.memora.R
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Step 2 of the add-item flow (Figma: ITEM DETAILS) — name the thing that was just photographed.
 *
 * The name is held by the caller (the shared [ItemDraft]) rather than locally, so stepping back to
 * retake the photo and forward again does not wipe what was typed.
 */
@Composable
fun AddItemDetailsScreen(
    photoPath: String?,
    itemName: String,
    onNameChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    AddFlowScaffold(title = "ITEM DETAILS", onBackClick = onBackClick) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            PhotoWell(
                photoPath = photoPath,
                contentDescription = "Item photo",
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(28.dp))

            MemoraNameField(
                value = itemName,
                onValueChange = onNameChange,
                placeholder = "Item Name...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            SuggestionsSection(
                suggestions = Suggestions.ITEMS,
                onSuggestionClick = onNameChange
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        MemoraOutlinedButton(
            text = "NEXT",
            enabled = itemName.isNotBlank(),
            onClick = onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = screenBottomPadding(32.dp))
        )
    }
}