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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
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
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim

private val FieldShape = RoundedCornerShape(6.dp)

/**
 * Request payload describing which field is asking the global editor
 * overlay to come up. The overlay (rendered at the App.kt level) reads
 * a state of this type — when non-null, it draws full-screen on top of
 * everything else.
 */
data class FieldEditorRequest(
    val label: String,
    val initial: String,
    val placeholder: String,
    val password: Boolean,
    val onConfirm: (String) -> Unit,
)

/**
 * Top-level mutable state for the active field editor request. Provided
 * via [CompositionLocalProvider] in App.kt so any CathodeField nested
 * anywhere in the composition tree can drive the overlay.
 *
 * We hold the state outside any individual screen so the editor can
 * truly cover everything — no Dialog window-sizing or focus-stealing
 * shenanigans.
 */
val LocalFieldEditor = compositionLocalOf<MutableState<FieldEditorRequest?>> {
    error("FieldEditor controller not provided — wrap App content in CompositionLocalProvider(LocalFieldEditor provides ...)")
}

/**
 * D-pad-friendly text field. On D-pad focus the field highlights but
 * does NOT pop up the IME. Click (SELECT on a remote, tap on touch)
 * raises a top-level overlay editor that actually takes input.
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
    val editor = LocalFieldEditor.current

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
                .clickable {
                    editor.value = FieldEditorRequest(
                        label = label,
                        initial = value,
                        placeholder = placeholder,
                        password = password,
                        onConfirm = onValueChange,
                    )
                }
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
}

/**
 * Full-screen editor overlay. Rendered once at the top of the App
 * composition; reads its request from [LocalFieldEditor]. Covers
 * absolutely everything beneath with a near-opaque black scrim,
 * centers the editor card, focuses the [BasicTextField] on first
 * frame so the IME comes up exactly once.
 */
@Composable
fun FieldEditorOverlay() {
    val editor = LocalFieldEditor.current
    val req = editor.value ?: return

    var draft by remember(req) { mutableStateOf(req.initial) }
    val focusRequester = remember(req) { FocusRequester() }
    LaunchedEffect(req) { focusRequester.requestFocus() }

    val visual: VisualTransformation =
        if (req.password) PasswordVisualTransformation() else VisualTransformation.None
    val keyboardOpts = KeyboardOptions(
        keyboardType = if (req.password) KeyboardType.Password else KeyboardType.Text,
    )

    androidx.activity.compose.BackHandler { editor.value = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.97f)),
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
            Text(req.label.uppercase(), style = CathodeText.Section, color = PhosphorGreen)
            if (req.placeholder.isNotEmpty()) {
                Text(req.placeholder, style = CathodeText.Caption, color = PhosphorGreenDim)
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
                CathodeButton(text = "CANCEL", onClick = { editor.value = null })
                CathodeButton(text = "OK", onClick = {
                    req.onConfirm(draft)
                    editor.value = null
                })
            }
        }
    }
}
