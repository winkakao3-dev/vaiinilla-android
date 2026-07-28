package com.vaiinilla.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.vaiinilla.app.ui.navigation.AppNavHost
import com.vaiinilla.app.ui.theme.ThemePreferences
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var pendingEstablishmentSlug by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingEstablishmentSlug = establishmentSlugFrom(intent)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )

        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemePreferences.load(context)) }

            VaiinillaTheme(
                themeMode = themeMode,
                onThemeModeChange = { mode ->
                    themeMode = mode
                    ThemePreferences.save(context, mode)
                },
            ) {
                AppNavHost(
                    navController = rememberNavController(),
                    pendingEstablishmentSlug = pendingEstablishmentSlug,
                    onDeepLinkConsumed = { pendingEstablishmentSlug = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingEstablishmentSlug = establishmentSlugFrom(intent)
    }

    companion object {
        fun establishmentSlugFrom(intent: Intent?): String? {
            val data = intent?.data ?: return null
            return establishmentSlugFrom(data)
        }

        fun establishmentSlugFrom(uri: Uri): String? {
            if (uri.host != "vaiinilla.app") return null
            val segments = uri.pathSegments
            if (segments.size >= 2 && segments[0] == "e") {
                return segments[1].takeIf { it.isNotBlank() }
            }
            return null
        }
    }
}
