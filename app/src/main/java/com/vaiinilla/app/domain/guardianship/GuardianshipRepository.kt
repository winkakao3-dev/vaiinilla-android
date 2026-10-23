package com.vaiinilla.app.domain.guardianship

import java.math.BigDecimal

/** Lo que el tutor hace con su propia sesión: ver, agregar y cuidar a sus hijos. */
interface GuardianshipRepository {
    fun dependents(): Result<List<Dependent>>

    /** El aviso vigente; alta y vínculo mandan la versión que el tutor aceptó. */
    fun privacyNotice(): Result<PrivacyNotice>

    /** Acepta el aviso para todos los hijos que no lo tienen; devuelve a los hijos ya actualizados. */
    fun acceptPrivacyNotice(
        version: String,
        idempotencyKey: String,
    ): Result<List<Dependent>>

    fun addDependent(
        name: String,
        studentId: String,
        group: String?,
        consentVersion: String,
        idempotencyKey: String,
    ): Result<Dependent>

    /** El código de seis de un alumno con cuenta o el que saca el principal para otro tutor. */
    fun link(
        code: String,
        consentVersion: String,
        idempotencyKey: String,
    ): Result<Dependent>

    fun updateControls(
        dependentId: String,
        controls: DependentControls,
        idempotencyKey: String,
    ): Result<Dependent>

    /** Lo único que cambia un tutor que no es el principal: cuándo le avisan que el saldo bajó. */
    fun updateLowBalanceAlert(
        dependentId: String,
        amount: BigDecimal?,
        idempotencyKey: String,
    ): Result<Dependent>

    fun createGuardianCode(
        dependentId: String,
        idempotencyKey: String,
    ): Result<GuardianLinkCode>

    fun removeGuardian(
        dependentId: String,
        guardianshipId: String,
        idempotencyKey: String,
    ): Result<Dependent>

    fun makePrincipal(
        dependentId: String,
        guardianshipId: String,
        idempotencyKey: String,
    ): Result<Dependent>

    fun unlink(
        dependentId: String,
        idempotencyKey: String,
    ): Result<Unit>

    /** El alumno con cuenta propia genera el código para que su papá lo vincule. */
    fun createLinkCode(idempotencyKey: String): Result<GuardianLinkCode>

    fun issueContext(dependentId: String): Result<DependentContextGrant>

    /** Como hijo, con su propia sesión: quién lo cuida y qué no puede comprar; `null` sin tutor. */
    fun myControls(): Result<MyGuardianControls?>

    fun createAccountCode(
        dependentId: String,
        idempotencyKey: String,
    ): Result<GuardianLinkCode>

    /** El hijo, con su sesión nueva, se queda con la cuenta que le hizo su papá. Devuelve su nombre. */
    fun claimAccount(
        code: String,
        idempotencyKey: String,
    ): Result<String>

    /** El papá que entró sin código lo escribe para poder agregar o vincular hijos. */
    fun unlockParent(
        schoolCode: String,
        idempotencyKey: String,
    ): Result<Unit>

    fun studentRequests(dependentId: String): Result<List<StudentRequest>>

    fun requestForStudent(
        dependentId: String,
        kind: StudentRequestKind,
        refundMethod: RefundMethod?,
        note: String?,
        idempotencyKey: String,
    ): Result<StudentRequest>

    /** `pin` nulo lo quita. */
    fun setPin(
        dependentId: String,
        pin: String?,
        idempotencyKey: String,
    ): Result<Unit>

    /** Apaga su QR y su tarjeta. Devuelve cuántas se apagaron. */
    fun blockCredentials(
        dependentId: String,
        idempotencyKey: String,
    ): Result<Int>
}

/** Lo que el tutor hace como su hijo, con el token del contexto del hijo. */
interface DependentAccountRepository {
    fun wallet(): Result<ChildWallet>

    fun startCardReload(
        amount: BigDecimal,
        idempotencyKey: String,
    ): Result<CardReloadCheckout>

    fun cardReload(id: String): Result<CardReload>

    fun menuCalendar(): Result<MenuCalendar>

    /** Lo que no se le puede apartar (sus alergias); `null` si ya no hay tutela activa. */
    fun controls(): Result<MyGuardianControls?>

    /** Devuelve cuántos días quedaron apartados. */
    fun reserve(
        days: List<ScheduledDayOrder>,
        idempotencyKey: String,
    ): Result<Int>

    fun orders(): Result<List<ScheduledOrder>>

    fun cancel(
        order: ScheduledOrder,
        idempotencyKey: String,
    ): Result<Unit>
}

/** Abre la cuenta de un hijo con su contexto. Cada hijo tiene su propio token. */
fun interface DependentAccountFactory {
    fun forDependent(dependentId: String): DependentAccountRepository
}

class GuardianshipException(
    val code: String,
    message: String,
    val httpStatus: Int? = null,
    cause: Throwable? = null,
) : IllegalStateException(message, cause) {
    companion object {
        const val GUARDIAN_NOT_PRINCIPAL = "GUARDIAN_NOT_PRINCIPAL"
        const val PRODUCT_BLOCKED_BY_GUARDIAN = "PRODUCT_BLOCKED_BY_GUARDIAN"
        const val DAILY_LIMIT_EXCEEDED = "DAILY_LIMIT_EXCEEDED"

        /** Alta o vínculo de quien entró a la escuela sin su código: primero lo escribe. */
        const val PARENT_CODE_REQUIRED = "PARENT_CODE_REQUIRED"
        const val JOIN_CODE_INVALID = "JOIN_CODE_INVALID"
        const val RATE_LIMITED = "RATE_LIMITED"
    }
}
