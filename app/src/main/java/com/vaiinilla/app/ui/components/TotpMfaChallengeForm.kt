package com.vaiinilla.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaFactor
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun TotpMfaChallengeForm(
    factors: List<StudentAuthMfaFactor>,
    selectedFactorUid: String?,
    code: String,
    loading: Boolean,
    errorMessage: String?,
    onFactorSelected: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    submitBackground: Color? = null,
    submitContentColor: Color? = null,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Abre tu aplicación autenticadora e ingresa el código de 6 dígitos.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Text(
            "El código cambia cada 30 segundos.",
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        if (factors.size > 1) {
            Text(
                "Elige tu aplicación autenticadora",
                color = colors.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            factors.forEach { factor ->
                val selected = factor.uid == selectedFactorUid
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) colors.accent else colors.line,
                                shape = RoundedCornerShape(14.dp),
                            ).clickable(enabled = !loading) { onFactorSelected(factor.uid) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        factor.displayName?.takeIf { it.isNotBlank() } ?: "Aplicación autenticadora",
                        color = colors.ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        AuthAccessField(
            value = code,
            onValueChange = onCodeChange,
            label = "Código de verificación",
            placeholder = "000000",
            kind = AuthAccessFieldKind.OneTimeCode,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            error = errorMessage,
        )
        if (loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    strokeWidth = 3.dp,
                )
            }
        }
        EditorialPrimaryButton(
            text = "Verificar código",
            onClick = onSubmit,
            enabled = !loading && code.length == 6 && selectedFactorUid != null,
            background = submitBackground,
            contentColor = submitContentColor,
        )
        EditorialPrimaryButton(
            text = "Cancelar",
            onClick = onCancel,
            enabled = !loading,
            background = colors.paper2,
            contentColor = colors.ink,
        )
    }
}
