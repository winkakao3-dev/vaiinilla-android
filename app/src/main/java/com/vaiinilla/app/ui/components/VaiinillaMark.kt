package com.vaiinilla.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.vaiinilla.app.R
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun VaiinillaMark(
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalVaiinillaColors.current.isDark,
    cream: Color = Cream,
    leafA: Color = Lime,
    leafB: Color = Color(0xFF8FB84E),
    coral: Color = Coral,
) {
    val iconRes = if (isDark) R.drawable.v_mark_dark else R.drawable.v_mark_light
    Image(
        painter = painterResource(id = iconRes),
        contentDescription = "Vaiinilla",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
