package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.GuardianLinkCode
import com.vaiinilla.app.domain.guardianship.GuardianLinkCodeRules
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.PersonNames
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Una hoja que saca un código al abrirse y lo muestra en fichas, con pasos debajo. */
@Composable
private fun CodeSheet(
    title: String,
    subtitle: String?,
    length: Int,
    sayDay: Boolean,
    generate: () -> Result<GuardianLinkCode>,
    steps: List<String>,
    footnote: String?,
    showsAnother: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf<GuardianLinkCode?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        error = null
        code = null
        withContext(Dispatchers.IO) { generate() }
            .onSuccess { code = it }
            .onFailure { error = it.toUserFacingMessage("No pudimos sacar el código.") }
    }
    LaunchedEffect(Unit) { load() }

    ParentSheet(title = title, subtitle = subtitle, colors = colors, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val message = error
            if (message != null) {
                ParentErrorCard(message = message, colors = colors, onRetry = { scope.launch { load() } })
            } else {
                CodeTiles(code = code?.code, length = length, colors = colors)
                Text(
                    code?.let { expiryText(it, sayDay) } ?: " ",
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
        if (steps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 14.dp)) {
                steps.forEachIndexed { index, step ->
                    GuideStep(index + 1, step, colors.textPrimary, colors.cardInner, colors.cardBorder)
                }
            }
        }
        footnote?.let { Text(it, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp) }
        if (showsAnother && code != null) {
            Text(
                "Sacar otro",
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .physicalPress(scale = PhysicalPressScale.Small) { scope.launch { load() } }
                        .padding(14.dp),
            )
        }
    }
}

/** El código de seis letras que el alumno le dicta a su papá. Dura 15 minutos y sirve una vez. */
@Composable
internal fun LinkCodeSheet(
    guardianship: GuardianshipRepository,
    onDismiss: () -> Unit,
) {
    CodeSheet(
        title = "Código para tu papá",
        subtitle = "Que lo escriba en su app, en Cartera › Agregar a mi hijo",
        length = 6,
        sayDay = false,
        generate = { guardianship.createLinkCode(newKey()) },
        steps = emptyList(),
        footnote = null,
        showsAnother = true,
        onDismiss = onDismiss,
    )
}

/** El código de seis que el principal le pasa a otro tutor: lo escribe como el de un hijo. */
@Composable
internal fun GuardianCodeSheet(
    child: Dependent,
    guardianship: GuardianshipRepository,
    onDismiss: () -> Unit,
) {
    CodeSheet(
        title = "Otro tutor",
        subtitle = "Para que también cuide a ${child.firstName}",
        length = 6,
        sayDay = true,
        generate = { guardianship.createGuardianCode(child.userId, newKey()) },
        steps =
            listOf(
                "Que entre a la escuela con su propia cuenta, con el código o el QR de la escuela.",
                "En Cartera › Opciones avanzadas › \"Agregar a mi hijo\", que elija \"Con código\" y escriba este.",
                "La escuela lo aprueba y desde ahí ve a ${child.firstName}, le recarga y le aparta.",
            ),
        footnote = "El límite, los bloqueos y las alergias los sigues poniendo tú. Puedes quitarlo cuando quieras.",
        showsAnother = false,
        onDismiss = onDismiss,
    )
}

/** El tutor le pasa a su hijo sin cuenta el código de ocho letras para que entre con su propio Google. */
@Composable
internal fun AccountCodeSheet(
    child: Dependent,
    guardianship: GuardianshipRepository,
    onDismiss: () -> Unit,
) {
    CodeSheet(
        title = "Su propia cuenta",
        subtitle = "Para cuando ${child.firstName} tenga teléfono",
        length = 8,
        sayDay = true,
        generate = { guardianship.createAccountCode(child.userId, newKey()) },
        steps =
            listOf(
                "Que ${child.firstName} baje Vaiinilla y entre con su propio Google.",
                "Que entre a la escuela con su código o su QR.",
                "En Cartera › Opciones avanzadas › \"Tengo un código de mi papá\", que escriba este código.",
            ),
        footnote = "Se queda con su saldo, lo apartado y tus controles. Tú lo sigues viendo aquí.",
        showsAnother = false,
        onDismiss = onDismiss,
    )
}

/**
 * El hijo, ya con su Google y dentro de la escuela, escribe el código de su papá y se queda con su cuenta.
 * [onClaimed] vuelve a pedir el contexto de cliente: el de antes era de la cuenta nueva y ya no sirve.
 */
@Composable
internal fun ClaimAccountSheet(
    guardianship: GuardianshipRepository,
    onClaimed: suspend () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle("Entrar a mi cuenta")) }
    var error by remember { mutableStateOf<String?>(null) }
    var sendKey by remember { mutableStateOf(newKey()) }
    val problem =
        if (GuardianLinkCodeRules.typedLength(code) >= 8 && GuardianLinkCodeRules.normalizedAccountCode(code) == null) {
            "Son ocho letras o números, sin I, O, 0 ni 1."
        } else {
            null
        }

    ParentSheet(
        title = "Tu cuenta de la escuela",
        subtitle = "Con el código que te dio tu papá",
        colors = colors,
        busy = state.isBusy,
        onDismiss = onDismiss,
        footer = {
            error?.let { ParentErrorCard(message = it, colors = colors, onDismiss = { error = null }) }
            MorphingButton(
                state = state,
                enabled = !state.isBusy && GuardianLinkCodeRules.normalizedAccountCode(code) != null,
                colors = colors,
                testTag = "claim-account-submit",
            ) {
                val normalized = GuardianLinkCodeRules.normalizedAccountCode(code) ?: return@MorphingButton
                error = null
                state = MorphState.Working("Entrando…")
                scope.launch {
                    withContext(Dispatchers.IO) { guardianship.claimAccount(normalized, sendKey) }
                        .onSuccess { name ->
                            onClaimed()
                            state = MorphState.Done("Hola, ${PersonNames.first(name)}")
                            delay(if (reduced) 300 else 1000)
                            onDismiss()
                        }.onFailure {
                            state = MorphState.Idle("Entrar a mi cuenta")
                            sendKey = newKey()
                            error = it.toUserFacingMessage("No pudimos entrar. Intenta otra vez.")
                        }
                }
            }
        },
    ) {
        OperationalField(
            value = code,
            onValueChange = { code = it.uppercase() },
            label = "Código",
            colors = colors,
            placeholder = "ABCD2345",
            supporting = problem ?: "Son ocho letras o números. Tu papá lo saca en tu ficha y dura 24 horas.",
            isError = problem != null,
            enabled = !state.isBusy,
        )
        Text(
            "Tu saldo, lo apartado y lo que tu papá te puso pasan a esta cuenta. Solo funciona con una cuenta que no ha pedido nada todavía.",
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}
