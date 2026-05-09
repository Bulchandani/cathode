package io.github.bulchandani.cathode.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import kotlinx.coroutines.delay

/**
 * Tiny app-wide status banner — call [show] from anywhere; the
 * shell renders [Host] once and a 1500ms fade in/out plays.
 */
object Toaster {
    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> = _message

    fun show(text: String) { _message.value = text }
    internal fun clear() { _message.value = null }
}

@Composable
fun ToasterHost(durationMs: Long = 1500L) {
    val msg by Toaster.message
    LaunchedEffect(msg) {
        if (msg != null) {
            delay(durationMs)
            Toaster.clear()
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = msg != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = msg.orEmpty(),
                    style = CathodeText.Body,
                    color = PhosphorGreen,
                )
            }
        }
    }
}
