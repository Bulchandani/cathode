package io.github.bulchandani.cathode.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.data.store.SettingsStore
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.Toaster
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.Void

/** 4-digit PIN entry. Used for setting a new PIN and for gating Settings. */
@Composable
fun PinDialog(
    title: String,
    subtitle: String = "",
    onConfirm: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .widthIn(min = 320.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = CathodeText.Section, color = PhosphorGreen)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = CathodeText.Caption, color = OffWhite)
            }
            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i < pin.length) PhosphorGreen else DimGrey),
                    )
                }
            }
            error?.let { Text(it, style = CathodeText.Caption, color = AlarmRed) }
            // Number pad
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("DEL", "0", "OK"),
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { label ->
                            PinKey(label) {
                                when (label) {
                                    "DEL" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    "OK" -> {
                                        if (pin.length != 4) {
                                            error = "Enter 4 digits"
                                        } else if (!onConfirm(pin)) {
                                            error = "Incorrect PIN"
                                            pin = ""
                                        }
                                    }
                                    else -> if (pin.length < 4) pin += label
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            CathodeButton(text = "CANCEL", onClick = onDismiss)
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DimGrey)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = CathodeText.Section,
            color = when (label) {
                "DEL" -> Amber
                "OK" -> PhosphorGreen
                else -> OffWhite
            },
        )
    }
}

/** Convenience wrappers for the two flows. */
@Composable
fun SetPinDialog(onDone: () -> Unit, onDismiss: () -> Unit) {
    PinDialog(
        title = "SET PIN",
        subtitle = "4 digits. Required when entering Settings.",
        onConfirm = { entered ->
            SettingsStore.setPin(entered)
            Toaster.show("PIN set")
            onDone()
            true
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun VerifyPinDialog(onPass: () -> Unit, onDismiss: () -> Unit) {
    PinDialog(
        title = "ENTER PIN",
        subtitle = "Settings is locked.",
        onConfirm = { entered ->
            if (SettingsStore.checkPin(entered)) {
                onPass()
                true
            } else false
        },
        onDismiss = onDismiss,
    )
}
