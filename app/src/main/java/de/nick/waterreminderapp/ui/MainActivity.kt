package de.nick.waterreminderapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.nick.waterreminderapp.ui.navigation.AppNavHost
import de.nick.waterreminderapp.ui.theme.WaterReminderAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaterReminderAppTheme {
                AppNavHost()
            }
        }
    }
}
