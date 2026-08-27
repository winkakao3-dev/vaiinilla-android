package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun EditorialSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(19.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(19.dp))
            Spacer(Modifier.size(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { contentDescription = placeholder },
                singleLine = true,
                textStyle = TextStyle(color = colors.ink, fontSize = 14.sp),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text(placeholder, color = colors.muted, fontSize = 14.sp)
                        }
                        input()
                    }
                },
            )
        }
    }
}

@Composable
fun EditorialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (singleLine) 48.dp else 86.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(17.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = label },
                singleLine = singleLine,
                visualTransformation =
                    if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = TextStyle(color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text(placeholder, color = colors.muted, fontSize = 14.sp)
                        }
                        input()
                    }
                },
            )
        }
    }
}

@Composable
fun EditorialNotesField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 86.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(17.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text(placeholder, color = colors.muted, fontSize = 14.sp)
                        }
                        input()
                    }
                },
            )
        }
    }
}

@Composable
fun EditorialPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color? = null,
    contentColor: Color? = null,
) {
    val colors = LocalVaiinillaColors.current
    val bg = background ?: colors.ink
    val fg = contentColor ?: colors.paper
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .physicalPress(
                    scale = PhysicalPressScale.Default,
                    enabled = enabled,
                    onClick = onClick,
                ),
        color = if (enabled) bg else colors.paper2,
        shape = RoundedCornerShape(18.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = if (enabled) fg else colors.muted,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun EditorialAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current
    EditorialPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = colors.accent,
        contentColor = colors.accentInk,
    )
}

@Composable
fun EditorialSectionHead(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, color = colors.ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
        when {
            onTrailingClick != null ->
                TextButton(onClick = onTrailingClick) {
                    Text(trailing.orEmpty(), color = colors.muted, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            trailing != null ->
                Text(trailing, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun EditorialHero(
    eyebrow: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    watermark: String? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 208.dp),
        color = colors.accent,
        shape = RoundedCornerShape(34.dp),
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            if (watermark != null) {
                Text(
                    watermark,
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = colors.accentInk.copy(alpha = 0.12f),
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Column {
                Text(
                    eyebrow.uppercase(),
                    color = colors.accentInk.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    title,
                    color = colors.accentInk,
                    fontSize = 34.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    body,
                    color = colors.accentInk.copy(alpha = 0.82f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Spacer(Modifier.height(20.dp))
                actions()
            }
        }
    }
}

@Composable
fun EditorialConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.paper,
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    message,
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Spacer(Modifier.height(18.dp))
                EditorialAccentButton(text = confirmLabel, onClick = onConfirm)
                Spacer(Modifier.height(8.dp))
                EditorialPrimaryButton(
                    text = dismissLabel,
                    onClick = onDismiss,
                    background = colors.paper2,
                    contentColor = colors.ink,
                )
            }
        }
    }
}
