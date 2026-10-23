package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.CardReloadCheckout
import com.vaiinilla.app.domain.guardianship.CardReloadRange
import com.vaiinilla.app.domain.guardianship.CardReloadStatus
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentAccountRepository
import com.vaiinilla.app.domain.guardianship.TypedAmount
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.order.STRIPE_CONFIRMATION_MAX_POLLS
import com.vaiinilla.app.ui.order.STRIPE_CONFIRMATION_POLL_INTERVAL_MS
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/** Recargar el saldo de un hijo con tarjeta: monto → PaymentSheet → el backend confirma. */
class ChildReloadModel(
    private val account: DependentAccountRepository,
    val range: CardReloadRange?,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val pollIntervalMs: Long = STRIPE_CONFIRMATION_POLL_INTERVAL_MS,
    private val maxPolls: Int = STRIPE_CONFIRMATION_MAX_POLLS,
) {
    sealed interface Phase {
        data object Editing : Phase

        data object Starting : Phase

        data object Paying : Phase

        data object Confirming : Phase

        data class Credited(
            val amount: BigDecimal,
        ) : Phase

        data class Failed(
            val message: String,
        ) : Phase
    }

    var amountText by mutableStateOf("")
    var phase by mutableStateOf<Phase>(Phase.Editing)
        private set

    /** Lo que PaymentSheet tiene que presentar; se borra al terminar (el secreto no se guarda). */
    var checkout by mutableStateOf<CardReloadCheckout?>(null)
        private set

    val amount: BigDecimal? get() = TypedAmount.parse(amountText)

    val problem: String? get() = range?.problem(amount)

    val isBusy: Boolean get() = phase == Phase.Starting || phase == Phase.Paying || phase == Phase.Confirming

    val canPay: Boolean
        get() {
            val value = amount ?: return false
            return value.signum() > 0 && problem == null && (phase == Phase.Editing || phase is Phase.Failed)
        }

    fun choose(quick: Int) {
        amountText = quick.toString()
        if (phase is Phase.Failed) phase = Phase.Editing
    }

    fun edit(text: String) {
        amountText = MoneyInput.sanitize(text).take(8)
        if (phase is Phase.Failed) phase = Phase.Editing
    }

    suspend fun start() {
        val value = amount ?: return
        if (!canPay) return
        phase = Phase.Starting
        withContext(io) { account.startCardReload(value, newKey()) }
            .onSuccess {
                checkout = it
                phase = Phase.Paying
            }.onFailure { phase = Phase.Failed(it.toUserFacingMessage("No pudimos abrir el pago.")) }
    }

    suspend fun finish(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> onPaymentSheetCompleted()
            is PaymentSheetResult.Canceled -> onPaymentSheetClosed(failed = false)
            is PaymentSheetResult.Failed -> onPaymentSheetClosed(failed = true)
        }
    }

    suspend fun onPaymentSheetCompleted() {
        phase = Phase.Confirming
        confirm()
    }

    fun onPaymentSheetClosed(failed: Boolean) {
        // El error crudo de Stripe no se guarda ni se muestra.
        phase = if (failed) Phase.Failed("La tarjeta no pasó. Prueba con otra o vuelve a intentar.") else Phase.Editing
        checkout = null
    }

    /** El resultado de PaymentSheet nunca acredita: se espera a que el backend diga "acreditada". */
    suspend fun confirm() {
        val reloadId = checkout?.reload?.id ?: return
        repeat(maxPolls) { index ->
            val reload = withContext(io) { account.cardReload(reloadId) }.getOrNull()
            when (reload?.status) {
                CardReloadStatus.CREDITED -> {
                    phase = Phase.Credited(reload.amount)
                    checkout = null
                    return
                }
                CardReloadStatus.FAILED, CardReloadStatus.CANCELED -> {
                    phase = Phase.Failed("El banco no aprobó el cobro. No se cargó nada.")
                    checkout = null
                    return
                }
                else -> if (index < maxPolls - 1) delay(pollIntervalMs)
            }
        }
        checkout = null
        phase = Phase.Failed("El pago sigue en revisión. Si se aprueba, el saldo llega solo.")
    }

    companion object {
        val QUICK_AMOUNTS = listOf(100, 200, 300, 500)
    }
}

/**
 * Recargar el saldo de un hijo con tarjeta: el monto en grande, montos rápidos y un solo botón que se
 * vuelve el progreso del pago y luego la confirmación. Usa la misma hoja de Stripe que los pedidos.
 */
@Composable
internal fun ChildReloadSheet(
    child: Dependent,
    balance: BigDecimal,
    account: DependentAccountRepository,
    range: CardReloadRange?,
    onCredited: (BigDecimal) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val reduced = reducedMotion()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val model = remember { ChildReloadModel(account, range) }
    val focus = remember { FocusRequester() }
    val paymentSheet = PaymentSheet.Builder { result -> scope.launch { model.finish(result) } }.build()
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(model.phase) {
        if (model.phase == ChildReloadModel.Phase.Paying) {
            val checkout = model.checkout ?: return@LaunchedEffect
            PaymentConfiguration.init(
                context = context,
                publishableKey = checkout.publishableKey,
                stripeAccountId = checkout.stripeAccountId,
            )
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = checkout.clientSecret,
                configuration = PaymentSheet.Configuration(merchantDisplayName = "Vaiinilla"),
            )
        }
        val phase = model.phase
        if (phase is ChildReloadModel.Phase.Credited) {
            onCredited(phase.amount)
            delay(if (reduced) 400 else 1300)
            onDismiss()
        }
    }
    val amount = model.amount
    val buttonState =
        when (val phase = model.phase) {
            ChildReloadModel.Phase.Editing, is ChildReloadModel.Phase.Failed ->
                MorphState.Idle(
                    if (amount != null &&
                        amount.signum() > 0
                    ) {
                        "Recargar ${compactMoney(amount)} a ${child.firstName}"
                    } else {
                        "Recargar a ${child.firstName}"
                    },
                )
            ChildReloadModel.Phase.Starting -> MorphState.Working("Abriendo el pago…")
            ChildReloadModel.Phase.Paying -> MorphState.Working("Pagando…")
            ChildReloadModel.Phase.Confirming -> MorphState.Working("Confirmando con el banco…")
            is ChildReloadModel.Phase.Credited ->
                MorphState.Done(
                    "${compactMoney(phase.amount)} para ${child.firstName}",
                )
        }
    val hint =
        (model.phase as? ChildReloadModel.Phase.Failed)?.message
            ?: model.problem
            ?: range?.label
            ?: "Escribe cuánto o elige un monto"

    ParentSheet(
        title = "Recargar a ${child.firstName}",
        subtitle = "Tiene ${compactMoney(balance)}",
        colors = colors,
        busy = model.isBusy,
        onDismiss = onDismiss,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChildReloadModel.QUICK_AMOUNTS.forEach { quick ->
                    ChipButton(
                        label = "$$quick",
                        enabled = !model.isBusy,
                        colors = colors,
                        selected = amount != null && amount.compareTo(quick.toBigDecimal()) == 0,
                        modifier = Modifier.weight(1f),
                    ) { model.choose(quick) }
                }
            }
            MorphingButton(state = buttonState, enabled = model.canPay, colors = colors, testTag = "child-reload-pay") {
                scope.launch { model.start() }
            }
            CenteredNote(
                "Pagas con tarjeta en Stripe. El saldo llega cuando el banco lo aprueba. Es para comer en esta escuela; si se va, pides que te lo devuelvan.",
                colors.textSecondary,
            )
        },
    ) {
        // El campo real está debajo; se ve el monto grande.
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            BasicTextField(
                value = model.amountText,
                onValueChange = model::edit,
                enabled = !model.isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier =
                    Modifier
                        .alpha(0.02f)
                        .focusRequester(focus)
                        .testTag("child-reload-amount")
                        .semantics { contentDescription = "Monto a recargar" },
            )
            Text(
                "$" + model.amountText.ifEmpty { "0" },
                color = colors.textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 60.sp,
                letterSpacing = (-2).sp,
                maxLines = 1,
            )
        }
        CenteredNote(
            hint,
            if (model.phase is ChildReloadModel.Phase.Failed ||
                model.problem != null
            ) {
                ErrorCoral
            } else {
                colors.textSecondary
            },
        )
    }
}
