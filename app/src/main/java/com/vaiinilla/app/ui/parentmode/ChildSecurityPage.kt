package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.guardianship.ChildGuardian
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.PersonNames
import com.vaiinilla.app.domain.guardianship.StudentRequest
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.guardianship.StudentRequestStatus
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.launch

private val YouAvatarFill = Color(0xFFC9A8FF)
private val YouAvatarInk = Color(0xFF2A1748)
private val OtherAvatarFill = Color(0xFFA8D8F6)
private val OtherAvatarInk = Color(0xFF0F2E44)

/**
 * Seguridad y tutores de un hijo (Doop E11), abierta desde su ficha: cómo compra en Caja, quién lo cuida y lo que se
 * hace cuando crece o deja la escuela. Lo que cambia el PIN, la credencial o las solicitudes es solo del principal.
 */
@Composable
internal fun ChildSecurityPage(
    state: ChildAccountState,
    onChildChanged: (Dependent) -> Unit,
    onSheet: (ChildSheet) -> Unit,
    onBlockCredential: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val scope = rememberCoroutineScope()
    val child = state.child
    var removing by remember { mutableStateOf<ChildGuardian?>(null) }
    var promoting by remember { mutableStateOf<ChildGuardian?>(null) }
    Box(modifier = Modifier.fillMaxSize().background(colors.paper).testTag("child-security-page")) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 56.dp, bottom = 40.dp),
        ) {
            Text(
                "Seguridad de ${child.firstName}",
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                letterSpacing = (-1.2).sp,
                modifier = Modifier.padding(horizontal = 4.dp).arrive(0),
            )
            if (child.isPrincipal) {
                CardCaption("Cómo compra", modifier = Modifier.padding(top = 24.dp).arrive(1))
                SecurityCard(Modifier.arrive(1)) {
                    CardRow(
                        title = "PIN en Caja",
                        detail =
                            if (child.hasPin) {
                                "Caja se lo pide antes de cobrarle con su saldo"
                            } else {
                                "Sin PIN le cobran con su nombre o su credencial"
                            },
                        tag = "child-pin",
                        trailing = { LinkText(if (child.hasPin) "Cambiar" else "Ponerle") },
                    ) { onSheet(ChildSheet.PIN) }
                    if (child.hasCredential) {
                        CardDivider()
                        CardRow(
                            title = "Bloquear su credencial",
                            detail = "Si la perdió, su QR y su tarjeta dejan de servir. En Caja le dan otra.",
                            tag = "child-block-credential",
                            enabled = !state.isBlockingCredential,
                            trailing = {
                                if (state.isBlockingCredential) {
                                    CircularProgressIndicator(color = colors.ink, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                } else {
                                    LinkText("Bloquear", colors.errorText)
                                }
                            },
                            onClick = onBlockCredential,
                        )
                    }
                }
            }
            CardCaption("Tutores", modifier = Modifier.padding(top = 24.dp).arrive(2))
            SecurityCard(Modifier.testTag("child-guardians").arrive(2)) {
                child.guardians.forEachIndexed { index, guardian ->
                    if (index > 0) CardDivider()
                    GuardianRow(
                        child = child,
                        guardian = guardian,
                        busy = state.changingGuardianId == guardian.id,
                        onRemove = { removing = guardian },
                        onPromote = { promoting = guardian },
                    )
                }
                if (child.isPrincipal) {
                    if (child.guardians.isNotEmpty()) CardDivider()
                    CardRow(
                        title = "Invitar a otro tutor",
                        detail = "Ve, recarga y aparta; los controles los sigues poniendo tú",
                        tag = "child-invite-guardian",
                        leading = {
                            InitialsAvatar("+", colors.line, colors.accent, Color.Transparent, Modifier.size(36.dp), RoundedCornerShape(12.dp), 20)
                        },
                    ) { onSheet(ChildSheet.GUARDIAN_CODE) }
                }
            }
            if (!child.isPrincipal) {
                child.principalFirstName?.let { principal ->
                    Text(
                        "$principal pone el límite, los bloqueos y las alergias. Tú ves, recargas y apartas.",
                        color = colors.muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
                    )
                }
            }
            if (!child.hasOwnAccount || child.isPrincipal) {
                CardCaption("Cuando crezca o se vaya", modifier = Modifier.padding(top = 24.dp).arrive(3))
                SecurityCard(Modifier.testTag("child-requests").arrive(3)) {
                    if (!child.hasOwnAccount) {
                        CardRow(
                            title = "Pasar a su propia cuenta",
                            detail = "Cuando tenga teléfono, entra con su Google y sigue con todo lo suyo",
                            tag = "child-own-account",
                        ) { onSheet(ChildSheet.OWN_ACCOUNT) }
                    }
                    if (child.isPrincipal) {
                        if (!child.hasOwnAccount) CardDivider()
                        RequestRow(
                            kind = StudentRequestKind.REFUND,
                            requests = state.requests,
                            title = "Devolver su saldo",
                            idle = "Pide que te regresen los ${compactMoney(state.balance)} que le quedan",
                        ) { onSheet(ChildSheet.REFUND) }
                        CardDivider()
                        RequestRow(
                            kind = StudentRequestKind.DATA_REMOVAL,
                            requests = state.requests,
                            title = "Pedir que borren sus datos",
                            idle =
                                if (state.balance.signum() > 0) {
                                    "Primero pide que te devuelvan su saldo"
                                } else {
                                    "La escuela deja de tenerlo en su lista y borra lo que lo identifica"
                                },
                            titleColor = colors.errorText,
                        ) { onSheet(ChildSheet.DATA_REMOVAL) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        GlassCircleButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Regresar a la ficha",
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, top = 6.dp),
        )
    }
    removing?.let { guardian ->
        ParentConfirmDialog(
            title = "¿Quitar a ${guardian.name}?",
            message = "Deja de ver a ${child.firstName}, de recargarle y de apartarle comida.",
            confirmLabel = "Quitar",
            destructive = true,
            onConfirm = { scope.launch { state.removeGuardian(guardian)?.let(onChildChanged) } },
            onDismiss = { removing = null },
        )
    }
    promoting?.let { guardian ->
        ParentConfirmDialog(
            title = "¿${guardian.firstName} pone los controles?",
            message =
                "Decidirá el límite, los bloqueos y las alergias, e invitará " +
                    "o quitará tutores. Tú sigues viendo y recargando.",
            confirmLabel = "Sí, que los ponga ${guardian.firstName}",
            destructive = false,
            onConfirm = { scope.launch { state.makePrincipal(guardian)?.let(onChildChanged) } },
            onDismiss = { promoting = null },
        )
    }
}

@Composable
private fun SecurityCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(LocalVaiinillaColors.current.paper2),
    ) { content() }
}

@Composable
private fun LinkText(
    text: String,
    color: Color = LocalVaiinillaColors.current.accent,
) {
    Text(text, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
}

@Composable
private fun GuardianRow(
    child: Dependent,
    guardian: ChildGuardian,
    busy: Boolean,
    onRemove: () -> Unit,
    onPromote: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val manages = child.isPrincipal && !guardian.isYou
    CardRow(
        title = if (guardian.isYou) "Tú" else guardian.name,
        detail =
            when {
                guardian.isPrincipal && guardian.isYou -> "Pones los controles"
                guardian.isPrincipal -> "Pone los controles"
                else -> "Puede ver, recargar y apartar"
            },
        tag = "guardian-${guardian.id}",
        enabled = manages && !busy,
        leading = {
            InitialsAvatar(
                PersonNames.initials(guardian.name),
                if (guardian.isYou) YouAvatarFill else OtherAvatarFill,
                if (guardian.isYou) YouAvatarInk else OtherAvatarInk,
                Color.Transparent,
                Modifier.size(36.dp),
                RoundedCornerShape(12.dp),
                14,
            )
        },
        trailing = {
            when {
                busy -> CircularProgressIndicator(color = colors.ink, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                manages ->
                    Box {
                        Box(
                            modifier = Modifier.size(32.dp).semantics { contentDescription = "Opciones de ${guardian.firstName}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = colors.muted)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Que ponga los controles") },
                                leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onPromote()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Quitar") },
                                leadingIcon = { Icon(Icons.Outlined.PersonRemove, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onRemove()
                                },
                            )
                        }
                    }
                guardian.isPrincipal -> Tag("Principal", colors.accent.copy(alpha = 0.14f), colors.accent)
            }
        },
    ) { menuOpen = true }
}

@Composable
private fun RequestRow(
    kind: StudentRequestKind,
    requests: List<StudentRequest>,
    title: String,
    idle: String,
    titleColor: Color = LocalVaiinillaColors.current.ink,
    onOpen: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val latest = StudentRequest.latest(kind, requests)
    val pending = latest?.status == StudentRequestStatus.PENDING
    CardRow(
        title = title,
        detail = latest?.let(::requestStatusLine) ?: idle,
        tag = "child-request-${kind.wireValue}",
        titleColor = titleColor,
        enabled = !pending,
        trailing =
            if (pending) {
                {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = "Esperando a la escuela",
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                null
            },
        onClick = onOpen,
    )
}