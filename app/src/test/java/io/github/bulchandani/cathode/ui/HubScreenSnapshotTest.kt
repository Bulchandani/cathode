package io.github.bulchandani.cathode.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bulchandani.cathode.ui.hub.HubScreen
import io.github.bulchandani.cathode.ui.theme.CathodeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders [HubScreen] to a PNG so we can preview the Cathode hub
 * without a Fire TV. Output: app/build/outputs/roborazzi/hub_default.png
 *
 * Run via: ./gradlew :app:recordRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1080dp-television-xhdpi")
class HubScreenSnapshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hubScreen_default() {
        composeRule.setContent {
            CathodeTheme {
                HubScreen()
            }
        }
        composeRule
            .onRoot()
            .captureRoboImage("build/outputs/roborazzi/hub_default.png")
    }
}
