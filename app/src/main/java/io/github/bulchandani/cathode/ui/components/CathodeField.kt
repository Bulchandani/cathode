@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.bulchandani.cathode.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
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
 * Click-to-edit text field. D-pad focus puts a highlight on the field row;
 * SELECT raises a full-screen editor overlay (see [FieldEditorOverlay])
 * that actually takes input. This split is essential on TV — bringing up
 * the IME inline mid-screen creates layout chaos.
 *
 * The field-row is a tv-material3 Surface so D-pad navigation works
 * correctly (no hand-rolled focusable/clickable stacking).
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
            color = PhosphorGreenDim,
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = {
                editor.value = FieldEditorRequest(
                    label = label,
                    initial = value,
                    placeholder = placeholder,
                    password = password,
                    onConfirm = onValueChange,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = ClickableSurfaceDefaults.shape(shape = FieldShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = DimGrey,
                contentColor = if (value.isEmpty()) PhosphorGreenDim else OffWhite,
                focusedContainerColor = DimGrey,
                focusedContentColor = OffWhite,
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, PhosphorGreenDim), shape = FieldShape),
                focusedBorder = Border(BorderStroke(2.dp, PhosphorGreen), shape = FieldShape),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(text = display, style = CathodeText.Body, maxLines = 1)
            }
        }
    }
}

/**
 * Full-screen editor overlay. Rendered once at the top of the App
 * composition; reads its request from [LocalFieldEditor]. Covers the
 * parent screen entirely with a near-opaque scrim, focuses the
 * BasicTextField on first frame so the IME pops once, places OK/Cancel
 * above the input on TV and below on touch.
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(req.label.uppercase(), style = CathodeText.Section, color = PhosphorGreen)
            if (req.placeholder.isNotEmpty()) {
                Text(req.placeholder, style = CathodeText.Caption, color = PhosphorGreenDim)
            }

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
