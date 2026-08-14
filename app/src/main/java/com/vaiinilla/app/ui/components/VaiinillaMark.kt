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

@Composable
fun VaiinillaMark(
    modifier: Modifier = Modifier,
    cream: Color = Cream,
    leafA: Color = Lime,
    leafB: Color = Color(0xFF8FB84E),
    coral: Color = Coral,
) {
    Image(
        painter = painterResource(id = R.drawable.v_mark_clean),
        contentDescription = "Vaiinilla",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
