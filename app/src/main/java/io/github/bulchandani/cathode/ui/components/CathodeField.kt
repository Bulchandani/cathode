package io.github.bulchandani.cathode.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
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

data class FieldEditorRequest(
    val label: String,
    val initial: String,
    val placeholder: String,
    val password: Boolean,
    val onConfirm: (String) -> Unit,
)

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
                .border(
                    width = if (rowFocused) 2.dp else 0.dp,
                    color = if (rowFocused) PhosphorGreen else Color.Transparent,
                    shape = FieldShape,
                )
                .cathodeGlow(focused = rowFocused, shape = FieldShape, blurDp = 18.dp)
                .onFocusChanged { rowFocused = it.isFocused }
                // No explicit .focusable() — clickable adds one. Two focus
                // stops produces a 2-press SELECT on Fire TV.
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
 * Full-screen editor overlay. Layout adapts to device class:
 *
 * - **Touch device (phone/tablet)**: OK/Cancel below input (touch-natural).
 *   `imePadding()` shifts the whole card up when the IME shows so the
 *   OK button is never under the keyboard.
 * - **TV / leanback**: OK/Cancel ABOVE input. From the input's D-pad
 *   position, UP reaches the buttons; the IME's Done action also
 *   confirms via [KeyboardActions]. Either way the OK button is
 *   reachable without arbitrary downward navigation.
 *
 * Focus is requested onto the input on first frame so typing starts
 * immediately. The black scrim is `fillMaxSize` so the parent screen
 * is completely covered — fixes the "underlay box" complaint.
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
        imeAction = ImeAction.Done,
    )
    val confirm = {
        req.onConfirm(draft)
        editor.value = null
    }
    val cancel = { editor.value = null }
    val keyboardActions = KeyboardActions(onDone = { confirm() })

    androidx.activity.compose.BackHandler { cancel() }

    val config = LocalConfiguration.current
    val isTv = (config.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.97f))
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(top = 24.dp, bottom = 24.dp)
                .widthIn(min = 320.dp, max = 900.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DimGrey)
                .padding(24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(req.label.uppercase(), style = CathodeText.Section, color = PhosphorGreen)
            if (req.placeholder.isNotEmpty()) {
                Text(req.placeholder, style = CathodeText.Caption, color = PhosphorGreenDim)
            }

            // On TV, place action buttons ABOVE the input so D-pad UP
            // from the focused input reaches them. On touch, below
            // (touch-natural; keyboard pushes the card via imePadding).
            if (isTv) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CathodeButton(text = "OK", onClick = confirm)
                    CathodeButton(text = "CANCEL", onClick = cancel)
                }
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
                    keyboardActions = keyboardActions,
                    cursorBrush = SolidColor(PhosphorGreen),
                    textStyle = CathodeText.Body.copy(color = OffWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }

            if (!isTv) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CathodeButton(text = "CANCEL", onClick = cancel)
                    CathodeButton(text = "OK", onClick = confirm)
                }
            }
        }
    }
}
