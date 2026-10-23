package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.launch

private enum class OptionsSheet { ADD, LINK_CODE, CLAIM }

/**
 * Cartera › Opciones avanzadas, solo en escuelas: lo de papás y alumnos queda aquí, lejos de quien
 * solo paga su comida, junto con la guía de cómo funciona para cada uno.
 */
@Composable
internal fun SchoolOptionsPage(
    children: ChildrenState,
    onAccountClaimed: suspend () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    var sheet by rememberSaveable { mutableStateOf<OptionsSheet?>(null) }
    var guideIndex by rememberSaveable { mutableIntStateOf(0) }

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
                "Opciones avanzadas",
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                letterSpacing = (-0.6).sp,
            )

            SectionLabel("Si eres papá o tutor")
            OptionRow(
                icon = Icons.Outlined.Add,
                title = if (children.children.isEmpty()) "Agregar a mi hijo" else "Agregar otro hijo",
                detail = "Con su nombre y matrícula, o con el código que saca él si ya tiene la app.",
                tag = "school-options-add",
            ) { sheet = OptionsSheet.ADD }

            SectionLabel("Si eres alumno")
            OptionRow(
                icon = Icons.Outlined.Tag,
                title = "Código para mi papá",
                detail = "Si ya tienes tu cuenta, tu papá te vincula con este código.",
                tag = "school-options-link-code",
            ) { sheet = OptionsSheet.LINK_CODE }
            // Quien ya cuida hijos no puede quedarse con la cuenta de otro: el servidor lo rechaza.
            if (children.children.isEmpty()) {
                OptionRow(
                    icon = Icons.Outlined.Key,
                    title = "Tengo un código de mi papá",
                    detail =
                        "Si tu papá te dio de alta, con su código te quedas " +
                            "con tu cuenta, tu saldo y lo apartado.",
                    tag = "school-options-claim",
                ) { sheet = OptionsSheet.CLAIM }
            }

            Spacer(Modifier.height(34.dp))
            Text("Cómo funciona", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
            SlidingSegments(
                labels = listOf("Papá o tutor", "Alumno"),
                selectedIndex = guideIndex,
                onSelect = {
                    haptics.selection()
                    guideIndex = it
                },
                trackColor = colors.paper2,
                indicatorColor = colors.ink,
                selectedContentColor = colors.onInk,
                idleContentColor = colors.ink,
                borderColor = colors.line,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            )
            AnimatedContent(
                targetState = guideIndex,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        fadeIn(spring(stiffness = 300f)) togetherWith
                            fadeOut(spring(stiffness = 600f))
                    }
                },
                label = "guide",
            ) { index ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    (if (index == 0) PARENT_STEPS else STUDENT_STEPS).forEachIndexed { step, text ->
                        GuideStep(step + 1, text, colors.ink, colors.paper2, colors.line)
                    }
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

    when (sheet) {
        OptionsSheet.ADD ->
            AddChildSheet(children.services.guardianship, onAdded = children::upsert, onDismiss = {
                sheet =
                    null
            })
        OptionsSheet.LINK_CODE -> LinkCodeSheet(children.services.guardianship, onDismiss = { sheet = null })
        OptionsSheet.CLAIM ->
            ClaimAccountSheet(
                guardianship = children.services.guardianship,
                onClaimed = {
                    onAccountClaimed()
                    scope.launch { children.load() }
                },
                onDismiss = { sheet = null },
            )
        null -> Unit
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = LocalVaiinillaColors.current.muted,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 28.dp, bottom = 2.dp),
    )
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
        "Entra a la escuela con el código o el QR que te dio. Tu propia cuenta sigue igual: pides y pagas como siempre.",
        "Aquí toca \"Agregar a mi hijo\" con su nombre y su matrícula. Si tu hijo ya tiene la app, pídele su código y escríbelo.",
        "Tu hijo aparece en Cartera › Mis hijos; si la escuela revisa a los tutores, primero como \"Esperando\". Ya aprobado, le recargas saldo con tarjeta, o lo recargan en Caja.",
        "Ponle un límite al día, bloquea productos y marca sus alergias. El límite y los bloqueos aplican cuando compra sin ti; las alergias, siempre, también a lo que tú le apartas.",
        "Apártale la comida de la semana antes de la hora de corte y cancélala si no va a ir.",
        "Si no tiene teléfono, en Caja lo encuentran por su nombre o con su credencial QR, y pagan con su saldo.",
        "Cuando tenga teléfono, en su ficha toca \"Pasar a su propia cuenta\" y dale el código. Se queda con todo lo suyo.",
    )

private val STUDENT_STEPS =
    listOf(
        "Si tu papá te dio de alta, no necesitas la app: en Caja te encuentran por tu nombre o con tu credencial.",
        "Si después tienes teléfono, pídele a tu papá su código y escríbelo aquí en \"Tengo un código de mi papá\".",
        "Si tienes la app, entra a la escuela con su código y saca aquí \"Código para mi papá\". Dura 15 minutos y sirve una vez.",
        "Tu papá ve tu saldo y lo que compras, te recarga y te puede poner un límite al día.",
        "Lo que tu papá bloquea sale con candado en el menú. En el carrito ves cuánto te queda hoy antes de pagar.",
        "En Cartera ves quién te cuida y tu límite.",
    )
