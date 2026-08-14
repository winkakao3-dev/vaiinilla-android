package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun ThemeCycleButton(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    val mode = LocalVaiinillaThemeMode.current
    val onChange = LocalVaiinillaThemeModeChanger.current ?: return

    IconButton(
        onClick = { onChange(mode.next()) },
        modifier =
            modifier
                .size(42.dp)
                .background(colors.paper2, RoundedCornerShape(16.dp)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text =
                    when (mode) {
                        VaiinillaThemeMode.System -> "⚙"
                        VaiinillaThemeMode.Light -> "☀"
                        VaiinillaThemeMode.Dark -> "◑"
                        VaiinillaThemeMode.Amoled -> "●"
                    },
                color = colors.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
