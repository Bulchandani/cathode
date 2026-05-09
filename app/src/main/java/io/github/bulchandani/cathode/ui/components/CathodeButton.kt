package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

private val ButtonShape = RoundedCornerShape(6.dp)

@Composable
fun CathodeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
) {
    var focused by remember { mutableStateOf(false) }

    val container: Color = when {
        !enabled -> PhosphorGreenDim
        focused -> PhosphorGreen
        else -> Void
    }
    val border = if (enabled) PhosphorGreen else PhosphorGreenDim
    val labelColor = when {
        !enabled -> OffWhite
        focused -> Void
        else -> PhosphorGreen
    }

    Box(
        modifier = modifier
            .clip(ButtonShape)
            .background(container)
            .cathodeGlow(focused = focused && enabled, shape = ButtonShape, blurDp = 18.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CathodeText.Section,
            color = labelColor,
            textAlign = TextAlign.Center,
        )
    }
}
