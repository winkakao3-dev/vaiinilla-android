package com.vaiinilla.app.ui.parentmode

import com.vaiinilla.app.domain.guardianship.CardReload
import com.vaiinilla.app.domain.guardianship.CardReloadCheckout
import com.vaiinilla.app.domain.guardianship.ChildGuardian
import com.vaiinilla.app.domain.guardianship.ChildMovement
import com.vaiinilla.app.domain.guardianship.ChildWallet
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentAccountRepository
import com.vaiinilla.app.domain.guardianship.DependentContextGrant
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.domain.guardianship.GuardianLinkCode
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.MenuCalendar
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.PrivacyNotice
import com.vaiinilla.app.domain.guardianship.RefundMethod
import com.vaiinilla.app.domain.guardianship.ScheduledDayOrder
import com.vaiinilla.app.domain.guardianship.ScheduledOrder
import com.vaiinilla.app.domain.guardianship.StudentRequest
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.CatalogRepository
import java.math.BigDecimal

private fun <T> unused(): Result<T> = Result.failure(UnsupportedOperationException("No se usa en esta prueba"))

internal open class FakeGuardianship(
    var children: List<Dependent> = emptyList(),
) : GuardianshipRepository {
    override fun dependents() = Result.success(children)

    override fun privacyNotice() = Result.success(PrivacyNotice("v1", true, "Aviso", listOf("Uno")))

    override fun acceptPrivacyNotice(
        version: String,
        idempotencyKey: String,
    ): Result<List<Dependent>> = Result.success(children.map { it.copy(needsConsent = false) }.also { children = it })

    override fun addDependent(
        name: String,
        studentId: String,
        group: String?,
        consentVersion: String,
        idempotencyKey: String,
    ): Result<Dependent> = unused()

    override fun link(
        code: String,
        consentVersion: String,
        idempotencyKey: String,
    ): Result<Dependent> = unused()

    override fun updateControls(
        dependentId: String,
        controls: DependentControls,
        idempotencyKey: String,
    ): Result<Dependent> = unused()

    override fun updateLowBalanceAlert(
        dependentId: String,
        amount: BigDecimal?,
        idempotencyKey: String,
    ): Result<Dependent> = unused()

    override fun createGuardianCode(
        dependentId: String,
        idempotencyKey: String,
    ): Result<GuardianLinkCode> = unused()

    override fun removeGuardian(
        dependentId: String,
        guardianshipId: String,
        idempotencyKey: String,
    ): Result<Dependent> = unused()

    override fun makePrincipal(
        dependentId: String,
        guardianshipId: String,
        idempotencyKey: String,
    ): Result<Dependent> = unused()

    override fun unlink(
        dependentId: String,
        idempotencyKey: String,
    ): Result<Unit> = unused()

    override fun createLinkCode(idempotencyKey: String): Result<GuardianLinkCode> = unused()

    override fun issueContext(dependentId: String): Result<DependentContextGrant> =
        Result.success(DependentContextGrant("t", 900))

    override fun myControls(): Result<MyGuardianControls?> = Result.success(null)

    override fun createAccountCode(
        dependentId: String,
        idempotencyKey: String,
    ): Result<GuardianLinkCode> = unused()

    override fun claimAccount(
        code: String,
        idempotencyKey: String,
    ): Result<String> = unused()

    override fun studentRequests(dependentId: String): Result<List<StudentRequest>> = Result.success(emptyList())

    override fun requestForStudent(
        dependentId: String,
        kind: StudentRequestKind,
        refundMethod: RefundMethod?,
        note: String?,
        idempotencyKey: String,
    ): Result<StudentRequest> = unused()

    override fun setPin(
        dependentId: String,
        pin: String?,
        idempotencyKey: String,
    ): Result<Unit> = unused()

    override fun blockCredentials(
        dependentId: String,
        idempotencyKey: String,
    ): Result<Int> = unused()
}

internal class FakeAccount(
    private val wallet: ChildWallet,
    private val orders: List<ScheduledOrder> = emptyList(),
) : DependentAccountRepository {
    override fun wallet() = Result.success(wallet)

    override fun startCardReload(
        amount: BigDecimal,
        idempotencyKey: String,
    ): Result<CardReloadCheckout> = unused()

    override fun cardReload(id: String): Result<CardReload> = unused()

    override fun menuCalendar(): Result<MenuCalendar> = unused()

    override fun controls(): Result<MyGuardianControls?> = Result.success(null)

    override fun reserve(
        days: List<ScheduledDayOrder>,
        idempotencyKey: String,
    ): Result<Int> = unused()

    override fun orders() = Result.success(orders)

    override fun cancel(
        order: ScheduledOrder,
        idempotencyKey: String,
    ): Result<Unit> = unused()
}

internal class FakeCatalog : CatalogRepository {
    override fun getCatalog(): Result<Catalog> = Result.success(Catalog(emptyList(), emptyList(), null))

    override fun getOperationalStatus(): Result<OperationalStatus> = unused()

    override fun createProduct(
        draft: CatalogProductDraft,
        idempotencyKey: String,
    ): Result<Product> = unused()

    override fun setProductAvailability(
        productId: Int,
        available: Boolean,
        idempotencyKey: String,
    ): Result<Product> = unused()

    override fun uploadProductImage(
        productId: Int,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<Product> = unused()
}

internal object ParentFixtures {
    val ana =
        Dependent(
            userId = "u-ana",
            name = "Ana López",
            group = "3°B",
            photoUrl = null,
            studentId = "A-001",
            hasOwnAccount = false,
            balance = BigDecimal("40.00"),
            controls = DependentControls(dailyLimit = BigDecimal("60.00"), lowBalanceAlert = BigDecimal("50.00")),
            guardians =
                listOf(
                    ChildGuardian("t1", "Marta López", isPrincipal = true, isYou = true),
                    ChildGuardian("t2", "Luis López", isPrincipal = false, isYou = false),
                ),
            hasCredential = true,
            needsConsent = true,
        )

    val tilin =
        Dependent(
            userId = "u-tilin",
            name = "Tilin Ramírez",
            group = "3B",
            photoUrl = null,
            studentId = "A100",
            hasOwnAccount = false,
            balance = null,
            controls = null,
            isAwaitingSchool = true,
        )

    val wallet =
        ChildWallet(
            balance = BigDecimal("40.00"),
            movements =
                listOf(
                    ChildMovement(
                        "m1",
                        "recarga_stripe",
                        "Recarga con tarjeta",
                        BigDecimal("100.00"),
                        BigDecimal("140.00"),
                        "2026-10-21T10:00:00Z",
                    ),
                    ChildMovement(
                        "m2",
                        "compra",
                        "Compra",
                        BigDecimal("-100.00"),
                        BigDecimal("40.00"),
                        "2026-10-21T13:00:00Z",
                    ),
                ),
            reloadRange = null,
        )

    fun services(
        guardianship: GuardianshipRepository,
        account: DependentAccountRepository = FakeAccount(wallet),
    ) = ParentModeServices(guardianship, { account }, FakeCatalog())
}
