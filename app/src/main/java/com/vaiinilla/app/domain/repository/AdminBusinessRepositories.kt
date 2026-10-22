package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.InvitationsPage
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.SpaceBatchResult
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.admin.StaffInvitation
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.admin.StaffRole
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeSetup

/** Personal e invitaciones: solo la administración. */
interface AdminStaffRepository {
    fun invitations(
        state: InvitationState?,
        cursor: String?,
    ): Result<InvitationsPage>

    fun invite(
        email: String,
        role: StaffRole,
        idempotencyKey: String,
    ): Result<StaffInvitation>

    /** Manda una invitación nueva; la anterior queda reemplazada. */
    fun resend(
        invitationId: String,
        idempotencyKey: String,
    ): Result<StaffInvitation>

    fun revoke(
        invitationId: String,
        idempotencyKey: String,
    ): Result<StaffInvitation>

    fun memberships(): Result<List<StaffMembership>>

    fun changeRole(
        membershipId: String,
        role: StaffRole,
        idempotencyKey: String,
    ): Result<StaffMembership>

    fun deactivate(
        membershipId: String,
        idempotencyKey: String,
    ): Result<Unit>
}

/** Flujo de mi tienda, pagos con tarjeta y cashback. */
interface AdminStoreRepository {
    fun config(): Result<StoreConfig>

    fun saveFlow(flow: StoreFlow): Result<StoreConfig>

    fun saveVideos(videos: List<StoreVideo>): Result<StoreConfig>

    fun setVisibleInDirectory(visible: Boolean): Result<StoreConfig>

    /** Prende o apaga el modo escuela; solo cambia lo que muestran las apps. */
    fun setSchoolMode(isSchool: Boolean): Result<StoreConfig> =
        Result.failure(UnsupportedOperationException("Modo escuela no soportado"))

    /** `null` si el negocio aún no conecta su cuenta de Stripe. */
    fun stripe(): Result<StripeSetup?>

    /** El enlace de Stripe para conectar o terminar de configurar la cuenta. */
    fun stripeOnboardingLink(idempotencyKey: String): Result<String>

    fun setCardPayments(
        enabled: Boolean,
        idempotencyKey: String,
    ): Result<StripeSetup>

    fun setFeePaidByCustomer(
        paidByCustomer: Boolean,
        idempotencyKey: String,
    ): Result<StripeSetup>

    /** La regla vigente; `null` si nunca se ha configurado. */
    fun cashbackRule(): Result<CashbackRule?>

    fun saveCashbackRule(
        rule: CashbackRule,
        idempotencyKey: String,
    ): Result<CashbackRule>

    fun walletSummary(period: AdminPeriod): Result<AdminWalletSummary>

    /** Invitaciones pendientes, caja abierta y pedidos activos para "Ahora mismo". */
    fun liveCounts(): Result<AdminLiveCounts>
}

/** Mesas, canchas y demás espacios, con el horario de reservas. */
interface AdminSpacesRepository {
    fun spaces(): Result<List<ManagedSpace>>

    fun create(
        name: String,
        type: SpaceType,
        idempotencyKey: String,
    ): Result<ManagedSpace>

    fun createBatch(
        type: SpaceType,
        prefix: String,
        from: Int,
        to: Int,
        idempotencyKey: String,
    ): Result<SpaceBatchResult>

    fun update(
        spaceId: Int,
        patch: SpacePatch,
        idempotencyKey: String,
    ): Result<ManagedSpace>

    fun delete(
        spaceId: Int,
        idempotencyKey: String,
    ): Result<Unit>

    fun uploadImage(
        spaceId: Int,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<ManagedSpace>

    fun removeImage(
        spaceId: Int,
        idempotencyKey: String,
    ): Result<ManagedSpace>

    fun reservationSchedule(): Result<ReservationSchedule>

    fun saveReservationSchedule(
        schedule: ReservationSchedule,
        idempotencyKey: String,
    ): Result<ReservationSchedule>

    /** Cierra la cuenta de un espacio sin cobrarla, cuando el cliente se fue sin pagar. */
    fun closeWithoutCharging(
        spaceId: Int,
        note: String?,
        idempotencyKey: String,
    ): Result<Unit>
}
