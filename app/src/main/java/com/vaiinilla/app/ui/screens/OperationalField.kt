package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.animateSelectionColor

private val FieldError = Color(0xFFE5645A)

/**
 * Campo de texto de las pantallas del personal: relleno plano con la etiqueta adentro, el mismo
 * de Nuevo producto. El borde se oscurece con el foco y se vuelve coral con un error.
 */
@Composable
internal fun OperationalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supporting: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val border =
        animateSelectionColor(
            when {
                isError -> FieldError
                focused -> colors.textPrimary
                else -> colors.cardBorder
            },
            "operational-field-border",
        )
    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(colors.textPrimary),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label }
                    .clip(shape)
                    .background(colors.cardInner)
                    .border(1.dp, border, shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            decorationBox = { input ->
                Column {
                    Text(label, color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.padding(top = 2.dp)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, color = colors.textMuted, fontSize = 16.sp)
                        }
                        input()
                    }
                }
            },
        )
        if (supporting != null) {
            Text(
                supporting,
                color = if (isError) FieldError else colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}
