package io.github.bulchandani.cathode

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.bulchandani.cathode.ui.theme.CathodeTheme

class MainActivity : ComponentActivity() {

    /** Process-wide voice-search query inbox; consumed by SearchScreen. */
    companion object {
        var pendingVoiceQuery by mutableStateOf<String?>(null)
            private set
        fun consumeVoiceQuery(): String? {
            val q = pendingVoiceQuery
            pendingVoiceQuery = null
            return q
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleVoiceIntent(intent)
        setContent {
            CathodeTheme {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleVoiceIntent(intent)
    }

    private fun handleVoiceIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEARCH) {
            pendingVoiceQuery = intent.getStringExtra(SearchManager.QUERY)
        }
    }
}
