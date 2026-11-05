package com.vaiinilla.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

private val AuthPlaceholder = Color(0xFF9B9D94)
private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun AuthAccessField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    kind: AuthAccessFieldKind,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    onTrailingLabel: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    error: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    var focused by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val isPassword = kind == AuthAccessFieldKind.Password
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = colors.ink, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            if (trailingLabel != null && onTrailingLabel != null) {
                Text(
                    trailingLabel,
                    // Se siente al tocarlo y toca en 48 dp, aunque la letra mida 16.
                    modifier =
                        Modifier
                            .minimumInteractiveComponentSize()
                            .physicalPress(scale = PhysicalPressScale.Small, onClick = onTrailingLabel)
                            .semantics { role = Role.Button },
                    color = colors.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(FieldShape)
                    .background(colors.paper)
                    .border(
                        width = if (focused) 2.dp else 1.dp,
                        color =
                            when {
                                error != null -> colors.coral
                                focused -> colors.accent.copy(alpha = 0.68f)
                                else -> colors.line
                            },
                        shape = FieldShape,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                kind.icon,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.padding(start = 16.dp).size(20.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = if (isPassword) 4.dp else 16.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = label },
                singleLine = true,
                cursorBrush = SolidColor(colors.ink),
                textStyle = TextStyle(color = colors.ink, fontSize = 16.sp, lineHeight = 24.sp),
                visualTransformation =
                    if (isPassword && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = kind.keyboardType,
                        imeAction = imeAction,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { onImeAction() },
                        onGo = { onImeAction() },
                    ),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text(placeholder, color = AuthPlaceholder, fontSize = 16.sp, lineHeight = 24.sp)
                        }
                        input()
                    }
                },
            )
            if (isPassword) {
                AuthRevealButton(revealed = revealed, onToggle = { revealed = !revealed })
            }
        }
        if (error != null) {
            Text(
                error,
                color = Color(0xFFB63834),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

enum class AuthAccessFieldKind(
    val icon: ImageVector,
    val keyboardType: KeyboardType,
) {
    Email(Icons.Outlined.Email, KeyboardType.Email),
    Password(Icons.Outlined.Lock, KeyboardType.Password),
    OneTimeCode(Icons.Outlined.Lock, KeyboardType.NumberPassword),
    Person(Icons.Outlined.Person, KeyboardType.Text),
    Id(Icons.Outlined.Badge, KeyboardType.Text),
}

@Composable
private fun AuthRevealButton(
    revealed: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (pressed) tween(70) else spring(dampingRatio = 0.42f, stiffness = 700f),
        label = "revealScale",
    )
    Box(
        modifier =
            Modifier
                .padding(end = 8.dp)
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onToggle,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (revealed) "Ocultar contraseña" else "Mostrar contraseña",
            tint = colors.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}
