package com.wbnbwclll.voiceledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wbnbwclll.voiceledger.ui.navigation.LedgerNavHost
import com.wbnbwclll.voiceledger.ui.theme.VoiceLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as VoiceLedgerApp).repository
        setContent {
            VoiceLedgerTheme {
                LedgerNavHost(repository = repository)
            }
        }
    }
}
