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
    private var pendingInvitationToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureDeepLink(intent)
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
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark =
                themeMode.resolveEffectiveMode(isSystemDark) != com.vaiinilla.app.ui.theme.VaiinillaThemeMode.Light

            androidx.compose.runtime.DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle =
                        if (isDark) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                            )
                        },
                    navigationBarStyle =
                        if (isDark) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                            )
                        },
                )
                onDispose {}
            }

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
                    pendingInvitationToken = pendingInvitationToken,
                    onDeepLinkConsumed = { pendingEstablishmentSlug = null },
                    onInvitationConsumed = { pendingInvitationToken = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureDeepLink(intent)
    }

    private fun captureDeepLink(source: Intent?) {
        pendingEstablishmentSlug = establishmentSlugFrom(source)
        pendingInvitationToken = invitationTokenFrom(source)
        // Do not retain invitation tokens in the Activity intent after capture.
        source?.data = null
    }

    companion object {
        fun establishmentSlugFrom(intent: Intent?): String? {
            val data = intent?.data ?: return null
            return establishmentSlugFrom(data)
        }

        fun establishmentSlugFrom(uri: Uri): String? {
            if (!isTrustedAppUri(uri)) return null
            val segments = uri.pathSegments
            if (segments.size != 2 || segments[0] != "e") return null
            return segments[1]
                .trim()
                .takeIf { it.length in 1..MAX_ESTABLISHMENT_SLUG_LENGTH && ESTABLISHMENT_SLUG.matches(it) }
        }

        fun invitationTokenFrom(intent: Intent?): String? {
            val data = intent?.data ?: return null
            return invitationTokenFrom(data)
        }

        fun invitationTokenFrom(uri: Uri): String? {
            if (!isTrustedAppUri(uri)) return null
            if (uri.pathSegments != listOf("invitaciones", "aceptar")) return null
            return uri
                .getQueryParameter("token")
                ?.trim()
                ?.takeIf { token ->
                    token.length in 1..MAX_INVITATION_TOKEN_LENGTH && token.none(Char::isWhitespace)
                }
        }

        private fun isTrustedAppUri(uri: Uri): Boolean {
            if (!uri.scheme.equals("https", ignoreCase = true)) return false
            val authority = uri.authority?.lowercase() ?: return false
            return authority in APP_HOSTS
        }

        private val APP_HOSTS = setOf("vaiinilla.app", "www.vaiinilla.app")
        private val ESTABLISHMENT_SLUG = Regex("[A-Za-z0-9][A-Za-z0-9_-]*")
        private const val MAX_ESTABLISHMENT_SLUG_LENGTH = 100
        private const val MAX_INVITATION_TOKEN_LENGTH = 4_096
    }
}
