package io.github.bulchandani.cathode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.bulchandani.cathode.ui.theme.CathodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CathodeTheme {
                App()
            }
        }
    }
}
