package com.micro.nothingclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.micro.nothingclock.ui.screens.ClockPreviewScreen
import com.micro.nothingclock.ui.theme.MicroNothingClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MicroNothingClockTheme {
                ClockPreviewScreen()
            }
        }
    }
}
