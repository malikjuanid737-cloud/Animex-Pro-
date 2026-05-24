package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.service.FirebaseServiceHelper
import com.example.ui.navigation.AppNavigation
import com.example.ui.navigation.AppRoutes
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support configuration
        enableEdgeToEdge()

        // Initialize singletons & SharedPreferences cache hierarchies
        ThemePreferences.init(applicationContext)
        FirebaseServiceHelper.init(applicationContext)

        // Process Deep-linking parameters
        var navStartDestination = AppRoutes.SPLASH
        intent?.data?.let { uri ->
            val host = uri.host
            val path = uri.path
            if (host == "watch" || path?.startsWith("/watch") == true) {
                val animeId = uri.getQueryParameter("animeId") ?: "a1"
                val episodeId = uri.getQueryParameter("episodeId") ?: "ep1"
                navStartDestination = "watch/$animeId/$episodeId"
            }
        }

        // Handle specific extras injected from FCM
        val deeplinkAnimeId = intent?.getStringExtra("deeplink_animeId")
        if (deeplinkAnimeId != null) {
            navStartDestination = "watch/$deeplinkAnimeId/ep1"
        }

        setContent {
            val isDarkMode = ThemePreferences.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode.value) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(startDestination = navStartDestination)
                }
            }
        }
    }
}
