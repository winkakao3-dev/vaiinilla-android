package com.vaiinilla.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VaiinillaTypography =
    Typography(
        displayMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 38.sp,
                lineHeight = 39.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.2).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 24.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Black,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
    )
