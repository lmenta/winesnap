package com.winesnap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.winesnap.app.ui.WineSnapScreen
import com.winesnap.app.ui.theme.WineSnapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WineSnapTheme {
                WineSnapScreen()
            }
        }
    }
}
