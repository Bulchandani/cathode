package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim

private val FieldShape = RoundedCornerShape(6.dp)

/**
 * D-pad-friendly text field. On D-pad focus the field highlights but
 * does NOT pop up the IME. The keyboard only appears when the user
 * explicitly clicks (SELECT on a remote, tap on touch) — that opens
 * a small modal editor with a real BasicTextField that grabs focus
 * and shows the IME exactly once. Apply (OK) commits the draft;
 * Cancel discards.
 */
@Composable
fun CathodeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    password: Boolean = false,
) {
    var rowFocused by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }

    val display = when {
        value.isEmpty() -> placeholder
        password -> "•".repeat(value.length.coerceAtMost(40))
        else -> value
    }

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = CathodeText.Caption,
            color = if (rowFocused) PhosphorGreen else PhosphorGreenDim,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(FieldShape)
                .background(DimGrey)
                .cathodeGlow(focused = rowFocused, shape = FieldShape, blurDp = 18.dp)
                .onFocusChanged { rowFocused = it.isFocused }
                .focusable()
                .clickable { dialogOpen = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = display,
                style = CathodeText.Body,
                color = if (value.isEmpty()) PhosphorGreenDim else OffWhite,
                maxLines = 1,
            )
        }
    }

    if (dialogOpen) {
        FieldEditorDialog(
            label = label,
            initial = value,
            password = password,
            placeholder = placeholder,
            onConfirm = { onValueChange(it); dialogOpen = false },
            onDismiss = { dialogOpen = false },
        )
    }
}

@Composable
private fun FieldEditorDialog(
    label: String,
    initial: String,
    password: Boolean,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val visual: VisualTransformation =
        if (password) PasswordVisualTransformation() else VisualTransformation.None
    val keyboardOpts = KeyboardOptions(
        keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        // Outer scrim: fills the dialog window and paints near-opaque black
        // so the parent screen (and the field box that was just clicked)
        // are no longer visible behind the editor. Fixes the "two boxes
        // underneath each other" report.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { /* swallow taps on the scrim */ },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 600.dp, max = 900.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DimGrey)
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(label.uppercase(), style = CathodeText.Section, color = PhosphorGreen)
                if (placeholder.isNotEmpty()) {
                    Text(placeholder, style = CathodeText.Caption, color = PhosphorGreenDim)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(FieldShape)
                        .background(Color.Black)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        visualTransformation = visual,
                        keyboardOptions = keyboardOpts,
                        cursorBrush = SolidColor(PhosphorGreen),
                        textStyle = CathodeText.Body.copy(color = OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CathodeButton(text = "CANCEL", onClick = onDismiss)
                    CathodeButton(text = "OK", onClick = { onConfirm(draft) })
                }
            }
        }
    }
}
