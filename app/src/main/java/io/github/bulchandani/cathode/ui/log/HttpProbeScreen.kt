package io.github.bulchandani.cathode.ui.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.CathodeField
import io.github.bulchandani.cathode.ui.components.Toaster
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void
import kotlinx.coroutines.launch

/**
 * Paste any URL → run a real GET with our standard headers → see the
 * response code, content-type, and first 800 bytes of body. Memory-
 * safe (capped read). Use this to diagnose 405s, find out what the
 * provider actually returns at /get.php, etc.
 */
@Composable
fun HttpProbeScreen(initialUrl: String, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    var url by remember { mutableStateOf(initialUrl) }
    var result by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(scrollState),
        ) {
            Text("HTTP PROBE", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.height(8.dp))
            Text(
                "Paste any URL. We'll GET it with the same headers Cathode uses (UA: Lavf/58.45.100). " +
                    "Response code, headers, and the first 800 bytes of the body come back below.",
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
            Spacer(Modifier.height(16.dp))
            CathodeField(
                label = "URL",
                value = url,
                onValueChange = { url = it },
                placeholder = "https://provider.example.com/get.php?username=…&password=…&type=m3u_plus",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CathodeButton(
                    text = if (running) "PROBING…" else "RUN PROBE",
                    enabled = !running && url.isNotBlank(),
                    onClick = {
                        running = true
                        result = "Probing…"
                        scope.launch {
                            result = try {
                                XtreamApi.probe(url.trim())
                            } catch (t: Throwable) {
                                "exception: ${t::class.simpleName}: ${t.message}"
                            }
                            running = false
                        }
                    },
                )
                CathodeButton(
                    text = "COPY RESULT",
                    enabled = result.isNotEmpty(),
                    onClick = {
                        clipboard.setText(AnnotatedString(result))
                        Toaster.show("Result copied")
                    },
                )
                CathodeButton(text = "CLOSE", onClick = onExit)
            }
            Spacer(Modifier.height(24.dp))
            Text("RESULT", style = CathodeText.Section, color = PhosphorGreen)
            Spacer(Modifier.height(8.dp))
            Text(
                text = result.ifEmpty { "(run a probe to see the result)" },
                style = CathodeText.Data,
                color = if (result.isEmpty()) PhosphorGreenDim else OffWhite,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
