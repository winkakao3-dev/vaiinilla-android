package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.vaiinilla.app.ui.components.moneyLabel
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

/** Cómo se cobra una recarga de un hijo; lo dicen la hoja de recarga y la primera recarga al agregarlo. */
internal const val CHILD_RELOAD_CARD_NOTE =
    "Pago seguro con Stripe · cuándo llega el saldo depende del banco. Es para comer en esta escuela; si se va, pides que te lo devuelvan."

/**
 * Recargar el saldo de un hijo con tarjeta: monto → total con comisión → PaymentSheet → el backend confirma.
 * [resumeReloadId] es una recarga que ya se pagó y el banco no había confirmado: la hoja la vuelve a consultar
 * en lugar de dejar pagar otra.
 */
class ChildReloadModel(
    private val account: DependentAccountRepository,
    range: CardReloadRange?,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val pollIntervalMs: Long = STRIPE_CONFIRMATION_POLL_INTERVAL_MS,
    private val maxPolls: Int = STRIPE_CONFIRMATION_MAX_POLLS,
    resumeReloadId: String? = null,
) {
    sealed interface Phase {
        data object Editing : Phase

        data object Starting : Phase

        /** El pago ya existe y se cobra [chargedCents] (el monto más la comisión): se enseña antes de abrir Stripe. */
        data class Review(
            val chargedCents: Long,
        ) : Phase

        data object Paying : Phase

        data object Confirming : Phase

        /** Pagado, pero el banco aún no confirma. No se puede pagar otra vez hasta que se resuelva. */
        data object InReview : Phase

        data class Credited(
            val amount: BigDecimal,
        ) : Phase

        data class Failed(
            val message: String,
        ) : Phase
    }

    /** Se llena cuando la cartera del hijo llega después de abrir la hoja. */
    var range by mutableStateOf(range)

    var amountText by mutableStateOf("")
    var phase by mutableStateOf<Phase>(if (resumeReloadId != null) Phase.Confirming else Phase.Editing)
        private set

    /** La recarga pagada que falta por confirmar; `null` cuando no hay nada en el aire. */
    var unsettledReloadId by mutableStateOf(resumeReloadId)
        private set

    /** Lo que PaymentSheet tiene que presentar; se borra al terminar (el secreto no se guarda). */
    var checkout by mutableStateOf<CardReloadCheckout?>(null)
        private set

    // Una respuesta perdida al abrir el pago se reintenta con la misma llave y el mismo monto: el servidor
    // devuelve la recarga que ya creó en vez de crear (y cobrar) otra (PAGO-16).
    private var startKey = newKey()
    private var startKeyAmount: BigDecimal? = null

    val amount: BigDecimal? get() = TypedAmount.parse(amountText.trimEnd('.', ','))

    val problem: String?
        get() =
            if (amountText.isNotEmpty() && amount == null) {
                "Escribe un monto válido: hasta 6 cifras y 2 decimales."
            } else {
                range?.problem(amount)
            }

    val isBusy: Boolean get() = phase == Phase.Starting || phase == Phase.Paying || phase == Phase.Confirming

    val canPay: Boolean
        get() {
            val value = amount ?: return false
            return value.signum() > 0 && problem == null && (phase == Phase.Editing || phase is Phase.Failed)
        }

    /** El botón principal sirve para pagar, para seguir al pago o para volver a preguntar por el banco. */
    val canAct: Boolean get() = canPay || phase is Phase.Review || phase == Phase.InReview

    fun choose(quick: Int) {
        amountText = quick.toString()
        reopenEditing()
    }

    fun edit(text: String) {
        amountText = MoneyInput.sanitize(text).take(8)
        reopenEditing()
    }

    /** Cambiar el monto descarta un error o un total ya enseñado: el pago se vuelve a crear con el monto nuevo. */
    private fun reopenEditing() {
        if (phase is Phase.Failed || phase is Phase.Review) {
            phase = Phase.Editing
            checkout = null
        }
    }

    suspend fun start() {
        val value = amount ?: return
        if (!canPay) return
        phase = Phase.Starting
        if (startKeyAmount?.compareTo(value) != 0) startKey = newKey()
        startKeyAmount = value
        withContext(io) { account.startCardReload(value, startKey) }
            .onSuccess {
                checkout = it
                startKey = newKey()
                startKeyAmount = null
                val extra = it.reload.chargedCents - value.movePointRight(2).toLong()
                if (extra > 0) phase = Phase.Review(it.reload.chargedCents) else beginPaying(it)
            }.onFailure {
                startKey = keyAfterFailure(it, startKey)
                phase = Phase.Failed(it.toUserFacingMessage("No pudimos abrir el pago."))
            }
    }

    /** Ya vio el total con comisión: ahora sí se abre Stripe. */
    fun proceedToPayment() {
        if (phase is Phase.Review) checkout?.let(::beginPaying)
    }

    // El id queda guardado antes de abrir la hoja: si el sistema recrea la pantalla a mitad del pago, el
    // resultado se sigue contra esta recarga en vez de perderse (PAGO-16, PAGO-17).
    private fun beginPaying(opened: CardReloadCheckout) {
        unsettledReloadId = opened.reload.id
        phase = Phase.Paying
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
        unsettledReloadId = checkout?.reload?.id ?: unsettledReloadId
        confirm()
    }

    fun onPaymentSheetClosed(failed: Boolean) {
        // El error crudo de Stripe no se guarda ni se muestra.
        phase = if (failed) Phase.Failed("La tarjeta no pasó. Prueba con otra o vuelve a intentar.") else Phase.Editing
        checkout = null
        unsettledReloadId = null
    }

    /** El resultado de PaymentSheet nunca acredita: se espera a que el backend diga "acreditada". */
    suspend fun confirm() {
        val reloadId = unsettledReloadId ?: checkout?.reload?.id
        if (reloadId == null) {
            // Sin el id no hay cómo preguntar; lo que sí se sabe es que no hay que pagar otra vez a ciegas.
            phase = Phase.InReview
            return
        }
        phase = Phase.Confirming
        repeat(maxPolls) { index ->
            val reload = withContext(io) { account.cardReload(reloadId) }.getOrNull()
            when (reload?.status) {
                CardReloadStatus.CREDITED -> {
                    phase = Phase.Credited(reload.amount)
                    settle()
                    return
                }
                CardReloadStatus.FAILED, CardReloadStatus.CANCELED -> {
                    phase = Phase.Failed("El banco no aprobó el cobro. No se cargó nada.")
                    settle()
                    return
                }
                else -> if (index < maxPolls - 1) delay(pollIntervalMs)
            }
        }
        checkout = null
        unsettledReloadId = reloadId
        phase = Phase.InReview
    }

    private fun settle() {
        checkout = null
        unsettledReloadId = null
    }

    companion object {
        val QUICK_AMOUNTS = listOf(100, 200, 300, 500)
    }
}

/**
 * Cómo queda el saldo con la recarga y, si tiene límite diario, para cuántos días le alcanza (Doop E09).
 * `null` sin un monto válido.
 */
internal fun childReloadReach(
    balance: BigDecimal,
    amount: BigDecimal?,
    dailyLimit: BigDecimal?,
): String? {
    if (amount == null || amount.signum() <= 0) return null
    val after = balance + amount
    val days = dailyLimit?.takeIf { it.signum() > 0 }?.let { after.divide(it, 0, java.math.RoundingMode.DOWN).toInt() }
    return when {
        days == null || days < 1 -> "Queda en ${compactMoney(after)}."
        days == 1 -> "Queda en ${compactMoney(after)}: le alcanza para 1 día."
        else -> "Queda en ${compactMoney(after)}: le alcanza para $days días."
    }
}

/**
 * Recargar el saldo de un hijo con tarjeta: el monto en grande, montos rápidos y un solo botón que se * vuelve el progreso del pago y luego la confirmación. Usa la misma hoja de Stripe que los pedidos.
 */
@Composable
internal fun ChildReloadSheet(
    child: Dependent,
    balance: BigDecimal,
    account: DependentAccountRepository,
    range: CardReloadRange?,
    /** Una recarga ya pagada que el banco no confirmó: no se paga otra mientras siga así. */
    unsettledReloadId: String?,
    onUnsettledChange: (String?) -> Unit,
    onCredited: (BigDecimal) -> Unit,
    onDismiss: () -> Unit,
    /** El monto que el tutor ya eligió antes de abrir la hoja (la primera recarga al agregar a un hijo). */
    initialAmount: Int? = null,
) {
    val colors = rememberOperationalColors()
    val reduced = reducedMotion()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Si el sistema recrea la pantalla a mitad del pago, la recarga sigue siendo la misma al volver.
    var savedReloadId by rememberSaveable { mutableStateOf(unsettledReloadId) }
    val model =
        remember {
            ChildReloadModel(account, range, resumeReloadId = savedReloadId).also { model ->
                if (savedReloadId == null) initialAmount?.let(model::choose)
            }
        }
    LaunchedEffect(range) { model.range = range }
    LaunchedEffect(model.unsettledReloadId) {
        savedReloadId = model.unsettledReloadId
        onUnsettledChange(model.unsettledReloadId)
    }
    LaunchedEffect(Unit) { if (model.phase == ChildReloadModel.Phase.Confirming) model.confirm() }
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
    val quickShown = ChildReloadModel.QUICK_AMOUNTS.take(3)
    var typingOther by rememberSaveable { mutableStateOf(false) }
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
            is ChildReloadModel.Phase.Review ->
                MorphState.Idle("Pagar ${compactMoney(phase.chargedCents.centsToMoney())}")
            ChildReloadModel.Phase.Paying -> MorphState.Working("Pagando…")
            ChildReloadModel.Phase.Confirming -> MorphState.Working("Confirmando con el banco…")
            ChildReloadModel.Phase.InReview -> MorphState.Idle("Volver a revisar")
            is ChildReloadModel.Phase.Credited ->
                MorphState.Done(
                    "${compactMoney(phase.amount)} para ${child.firstName}",
                )
        }
    val reach = childReloadReach(balance, amount, child.controls?.dailyLimit)
    val hint =
        when (val phase = model.phase) {
            is ChildReloadModel.Phase.Failed -> phase.message
            ChildReloadModel.Phase.InReview ->
                "Tu pago sigue en revisión. Si el banco lo aprueba, el saldo llega solo; no pagues otra vez."
            else -> model.problem ?: reach ?: range?.label ?: "Escribe cuánto o elige un monto"
        }
    val hintColor =
        when {
            model.phase is ChildReloadModel.Phase.Failed || model.problem != null -> ErrorCoral
            hint == reach && model.phase !is ChildReloadModel.Phase.InReview -> LocalVaiinillaColors.current.accent
            else -> colors.textSecondary
        }
    val limit = child.controls?.dailyLimit

    ParentSheet(
        title = "Recargar a ${child.firstName}",
        subtitle = "Tiene ${compactMoney(balance)}" + (limit?.let { " · su límite es ${compactMoney(it)} al día" } ?: ""),
        colors = colors,
        busy = model.isBusy,
        onDismiss = onDismiss,
        leading = {
            InitialsAvatar(
                child.initials.take(1),
                ChildAvatarFill,
                ChildAvatarInk,
                Color.Transparent,
                Modifier.size(44.dp),
                shape = RoundedCornerShape(15.dp),
                fontSize = 18,
            )
        },
        footer = {
            MorphingButton(state = buttonState, enabled = model.canAct, colors = colors, testTag = "child-reload-pay") {
                when (model.phase) {
                    is ChildReloadModel.Phase.Review -> model.proceedToPayment()
                    ChildReloadModel.Phase.InReview -> scope.launch { model.confirm() }
                    else -> scope.launch { model.start() }
                }
            }
            CenteredNote(
                CHILD_RELOAD_CARD_NOTE,
                colors.textSecondary,
            )
        },
    ) {
        // El campo real está debajo; se ve el monto grande.
        Box(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
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
            AnimatedContent(
                targetState = "$" + model.amountText.ifEmpty { "0" },
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        (fadeIn(spring(stiffness = 400f)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.9f)) togetherWith
                            fadeOut(spring(stiffness = 800f))
                    }
                },
                label = "reload-amount",
            ) { text ->
                Text(
                    text,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    letterSpacing = (-2.6).sp,
                    maxLines = 1,
                )
            }
        }
        CenteredNote(hint, hintColor)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            quickShown.forEach { quick ->
                ChipButton(
                    label = moneyLabel(quick),
                    enabled = !model.isBusy,
                    colors = colors,
                    selected = !typingOther && amount != null && amount.compareTo(quick.toBigDecimal()) == 0,
                    modifier = Modifier.weight(1f),
                ) {
                    typingOther = false
                    model.choose(quick)
                }
            }
            val other = typingOther || (amount != null && quickShown.none { amount.compareTo(it.toBigDecimal()) == 0 })
            ChipButton(
                label = "Otra",
                enabled = !model.isBusy,
                colors = colors,
                selected = other,
                modifier = Modifier.weight(1f).testTag("child-reload-other"),
            ) {
                typingOther = true
                model.edit("")
                runCatching { focus.requestFocus() }
            }
        }
        ReloadBreakdown(amount, (model.phase as? ChildReloadModel.Phase.Review)?.chargedCents?.centsToMoney(), colors)
    }
}

/**
 * Recarga, comisión y lo que pagas. La comisión la calcula el servidor al abrir el pago: antes de eso no se inventa,
 * se dice que se ve antes de pagar.
 */
@Composable
private fun ReloadBreakdown(
    amount: BigDecimal?,
    charged: BigDecimal?,
    colors: com.vaiinilla.app.ui.screens.OperationalColors,
) {
    if (amount == null || amount.signum() <= 0) return
    val line = LocalVaiinillaColors.current.ink.copy(alpha = 0.1f)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
        androidx.compose.material3.HorizontalDivider(color = line, thickness = 1.dp)
        BreakdownRow("Recarga", amount.money2(), colors.textSecondary, Modifier.padding(top = 8.dp))
        if (charged != null) {
            BreakdownRow("Comisión de la tarjeta", (charged - amount).max(BigDecimal.ZERO).money2(), colors.textSecondary)
            BreakdownRow("Pagas", charged.money2(), colors.textPrimary, bold = true)
        } else {
            BreakdownRow("Comisión de la tarjeta", "La ves antes de pagar", colors.textSecondary)
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            label,
            color = color,
            fontWeight = if (bold) FontWeight.Black else FontWeight.Bold,
            fontSize = if (bold) 16.sp else 14.5.sp,
            modifier = Modifier.weight(1f),
        )
        Text(value, color = color, fontWeight = if (bold) FontWeight.Black else FontWeight.Bold, fontSize = if (bold) 16.sp else 14.5.sp)
    }
}

/** "$200.00": el desglose va con centavos, como el cobro. */
private fun BigDecimal.money2(): String = "$" + String.format(java.util.Locale.US, "%,.2f", this)

private fun Long.centsToMoney(): BigDecimal = BigDecimal.valueOf(this, 2)
