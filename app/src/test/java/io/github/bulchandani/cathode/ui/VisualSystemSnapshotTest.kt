package io.github.bulchandani.cathode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.CathodeField
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.components.cathodeGlow
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.CathodeTheme
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the full Cathode design library on one page so we can review
 * the visual system before applying it to real screens. Output:
 * `app/build/outputs/roborazzi/visual_system.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1080dp-television-xhdpi")
class VisualSystemSnapshotTest {

    @Test
    fun visualSystem() {
        captureRoboImage(filePath = "build/outputs/roborazzi/visual_system.png") {
            CathodeTheme {
                Box(modifier = Modifier.fillMaxSize().background(Void)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(48.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            text = "CATHODE TYPE SPEC",
                            style = CathodeText.Display,
                            color = PhosphorGreen,
                        )

                        // ---------- TYPOGRAPHY ----------
                        SectionHeader("TYPOGRAPHY")
                        Text("Display VT323 56sp", style = CathodeText.Display, color = PhosphorGreen)
                        Text("Headline VT323 32sp", style = CathodeText.Headline, color = PhosphorGreen)
                        Text("Section Plex Mono Bold 20sp", style = CathodeText.Section, color = OffWhite)
                        Text("Body Plex Mono Regular 16sp — quick brown fox", style = CathodeText.Body, color = OffWhite)
                        Text("Data Plex Mono 14sp — bitrate 5,432 kbps · h264 1080p · 59.94fps", style = CathodeText.Data, color = OffWhite)
                        Text("Caption Plex Mono 12sp — supplementary metadata", style = CathodeText.Caption, color = PhosphorGreenDim)

                        Spacer(Modifier.height(8.dp))

                        // ---------- COMPONENTS ----------
                        SectionHeader("COMPONENTS")

                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            CathodeBox(modifier = Modifier.size(width = 240.dp, height = 96.dp)) {
                                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("CathodeBox idle", style = CathodeText.Body, color = PhosphorGreen)
                                }
                            }

                            // Synthetic "focused" box — uses the cathodeGlow modifier
                            // directly so the glow shows up in the static snapshot.
                            Box(
                                modifier = Modifier
                                    .size(width = 240.dp, height = 96.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DimGrey)
                                    .cathodeGlow(focused = true, shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("CathodeBox focused", style = CathodeText.Body, color = PhosphorGreen)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            CathodeButton(text = "BUTTON", onClick = {})
                            CathodeButton(text = "DISABLED", onClick = {}, enabled = false)
                            // Synthetic focused button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PhosphorGreen)
                                    .cathodeGlow(focused = true, shape = RoundedCornerShape(6.dp), blurDp = 18f)
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("FOCUSED", style = CathodeText.Section, color = Void)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            CathodeField(
                                label = "Empty field",
                                value = "",
                                onValueChange = {},
                                placeholder = "host.com:8080",
                                modifier = Modifier.width(360.dp),
                            )
                            CathodeField(
                                label = "Filled field",
                                value = "provider.example.com:8080",
                                onValueChange = {},
                                modifier = Modifier.width(360.dp),
                            )
                        }
                    }

                    CathodeScanlines()
                    CathodeVignette()
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(text = label, style = CathodeText.Section, color = PhosphorGreen)
}
