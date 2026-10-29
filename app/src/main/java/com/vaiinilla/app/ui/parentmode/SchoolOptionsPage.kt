package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.GlassCircleButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.WholeWordTitle
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private enum class OptionsSheet { ADD, UNLOCK, LINK_CODE, CLAIM }

/** Quién abre lo de la escuela en Cartera. Cada uno ve solo lo suyo: al tutor no le sale lo del alumno. */
internal enum class SchoolRole { PARENT, STUDENT }

/** Cartera › Soy tutor / Soy alumno, solo en escuelas: lo que puede hacer cada uno y cómo funciona. */
@Composable
internal fun SchoolOptionsPage(
    children: ChildrenState,
    role: SchoolRole,
    /** El alumno que ya tiene tutor entra con su propia cuenta: no tiene otra que reclamar. */
    hasTutors: Boolean,
    onAccountClaimed: suspend () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    var sheet by rememberSaveable { mutableStateOf<OptionsSheet?>(null) }
    LaunchedEffect(role) { if (role == SchoolRole.PARENT) children.loadParentAccess() }
    // Quien ya cuida hijos siempre puede agregar otro; quien no, primero escribe el código de la escuela.
    val parentAccess = if (children.children.isEmpty()) children.isParentUnlocked else true

    Box(modifier = Modifier.fillMaxSize().testTag("school-options")) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp)
                    .padding(top = 58.dp, bottom = 48.dp),
        ) {
            Text(
                "CARTERA · ESCUELA",
                color = colors.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
            )
            WholeWordTitle(
                text = if (role == SchoolRole.PARENT) "Tutores" else "Alumnos",
                color = colors.ink,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = (-1.6).sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )

            if (role == SchoolRole.PARENT) {
                ParentOption(children, parentAccess, reduced) { sheet = it }
            } else {
                OptionRow(
                    icon = Icons.Outlined.Tag,
                    title = if (hasTutors) "Que otro tutor me agregue" else "Que mi tutor me agregue",
                    detail = "Te damos un código y lo escribe en su app.",
                    tag = "school-options-link-code",
                ) { sheet = OptionsSheet.LINK_CODE }
                // Quien ya cuida hijos no puede quedarse con la cuenta de otro: el servidor lo rechaza.
                if (children.children.isEmpty() && !hasTutors) {
                    OptionRow(
                        icon = Icons.Outlined.Key,
                        title = "Mi tutor ya me dio de alta",
                        detail = "Escribe el código que te dio y te quedas con tu saldo y lo apartado.",
                        tag = "school-options-claim",
                    ) { sheet = OptionsSheet.CLAIM }
                }
            }

            Spacer(Modifier.height(34.dp))
            Text(
                "Cómo funciona",
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 19.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                schoolGuideSteps(role, hasTutors).forEachIndexed { step, text ->
                    GuideStep(step + 1, text, colors.ink, colors.paper2, colors.line)
                }
            }
        }
        GlassCircleButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Regresar",
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 16.dp, top = 6.dp),
        )
    }

    SchoolOptionsSheets(sheet, children, onAccountClaimed, scope) { sheet = null }
}

/** Agregar a un hijo, o antes el código de la escuela si todavía no lo escribió. */
@Composable
private fun ParentOption(
    children: ChildrenState,
    parentAccess: Boolean?,
    reduced: Boolean,
    onSheet: (OptionsSheet) -> Unit,
) {
    AnimatedContent(
        targetState = parentAccess,
        transitionSpec = {
            if (reduced) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                (
                    fadeIn(
                        spring(stiffness = 300f),
                    ) + slideInVertically(spring(stiffness = 400f)) { it / 6 }
                ) togetherWith
                    fadeOut(spring(stiffness = 600f))
            }
        },
        label = "parent-access",
    ) { access ->
        when (access) {
            true ->
                OptionRow(
                    icon = Icons.Outlined.Add,
                    title = if (children.children.isEmpty()) "Agregar a mi hijo" else "Agregar otro hijo",
                    detail = "Con su nombre y matrícula, o con el código que saca él si ya tiene la app.",
                    tag = "school-options-add",
                ) { onSheet(OptionsSheet.ADD) }
            false ->
                OptionRow(
                    icon = Icons.Outlined.Password,
                    title = "Tengo el código de la escuela",
                    detail = "El que la escuela les da a los tutores. Con él puedes agregar a tus hijos.",
                    tag = "school-options-unlock",
                ) { onSheet(OptionsSheet.UNLOCK) }
            null ->
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(56.dp),
                    corner = 14.dp,
                )
        }
    }
}

@Composable
private fun SchoolOptionsSheets(
    sheet: OptionsSheet?,
    children: ChildrenState,
    onAccountClaimed: suspend () -> Unit,
    scope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    when (sheet) {
        OptionsSheet.ADD ->
            AddChildSheet(
                children.services.guardianship,
                onAdded = children::upsert,
                onDismiss = onDismiss,
                onParentUnlocked = children::markParentUnlocked,
            )
        OptionsSheet.UNLOCK ->
            ParentUnlockSheet(
                guardianship = children.services.guardianship,
                onUnlocked = children::markParentUnlocked,
                onDismiss = onDismiss,
            )
        OptionsSheet.LINK_CODE -> LinkCodeSheet(children.services.guardianship, onDismiss = onDismiss)
        OptionsSheet.CLAIM ->
            ClaimAccountSheet(
                guardianship = children.services.guardianship,
                onClaimed = {
                    onAccountClaimed()
                    scope.launch { children.load() }
                },
                onDismiss = onDismiss,
            )
        null -> Unit
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    detail: String,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(tag)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(
                        44.dp,
                    ).clip(CircleShape)
                    .background(colors.paper2)
                    .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 13.dp)) {
            Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(detail, color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(18.dp),
        )
    }
}

private val PARENT_STEPS =
    listOf(
        "Entra a la escuela con el código que te dio: es el que te deja agregar a tus hijos. " +
            "Si entraste sin él, escríbelo aquí en \"Tengo el código de la escuela\". " +
            "Tu propia cuenta sigue igual: pides y pagas como siempre.",
        "Toca \"Agregar a mi hijo\" y escribe su nombre y su matrícula. " +
            "Si tu hijo ya tiene la app, pídele su código: lo saca en su Cartera, en \"Soy alumno\".",
        "Tu hijo aparece en Cartera › Mis hijos; si la escuela revisa a los tutores, primero como \"Esperando\". Ya aprobado, le recargas saldo con tarjeta, o lo recargan en Caja.",
        "Ponle un límite al día, bloquea productos y marca sus alergias. El límite y los bloqueos aplican cuando compra sin ti; las alergias, siempre, también a lo que tú le apartas.",
        "Apártale la comida de la semana antes de la hora de corte y cancélala si no va a ir.",
        "Si no tiene teléfono, en Caja lo encuentran por su nombre o con su credencial QR, y pagan con su saldo.",
        "Cuando tenga teléfono, en su ficha toca \"Pasar a su propia cuenta\" y dale el código. Se queda con todo lo suyo.",
    )

private val STUDENT_STEPS =
    listOf(
        "Toca \"Que mi tutor me agregue\": te damos un código de seis letras. Dura 15 minutos y sirve una vez.",
        "Tu tutor lo escribe en su app, en Cartera › Soy tutor › Agregar a mi hijo › Con código.",
        "Si tu tutor te dio de alta antes de que tuvieras teléfono, pídele su código y escríbelo en " +
            "\"Mi tutor ya me dio de alta\". Te quedas con tu saldo y lo apartado.",
        "Tu tutor ve tu saldo y lo que compras, te recarga y te puede poner un límite al día. " +
            "Lo que bloquee sale con candado en el menú.",
        "En Cartera ves quién te cuida y cuánto te queda hoy.",
        "Si no traes teléfono, en Caja te encuentran por tu nombre o con tu credencial.",
    )

/** La guía de cada rol; al alumno que ya tiene tutor se le habla de agregar a otro. */
internal fun schoolGuideSteps(
    role: SchoolRole,
    hasTutors: Boolean,
): List<String> =
    when {
        role == SchoolRole.PARENT -> PARENT_STEPS
        !hasTutors -> STUDENT_STEPS
        else -> STUDENT_STEPS.map { it.replace("Que mi tutor me agregue", "Que otro tutor me agregue") }
    }
