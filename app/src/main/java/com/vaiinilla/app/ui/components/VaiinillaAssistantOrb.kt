package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.R
import com.vaiinilla.app.domain.model.OperationalRole

data class AssistantFaq(
    val id: String,
    val question: String,
    val stepTitle: String,
    val stepInstruction: String,
    val targetHighlight: String? = null,
)

@Composable
fun VaiinillaAssistantOrb(
    role: OperationalRole,
    onHighlightTarget: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFaq by remember { mutableStateOf<AssistantFaq?>(null) }

    // Kinetic Sand Physics State:
    // Sits peaceful at rest. Only reacts organically on interaction (clicks, questions, steps)
    var kineticAngleTarget by remember { mutableFloatStateOf(0f) }
    var kineticSquishTarget by remember { mutableFloatStateOf(1f) }
    var clickCount by remember { mutableStateOf(0) }

    fun triggerKineticReaction() {
        clickCount++
        // Gentle fluid shift (+- 24° to 30°), viscous motion
        val delta = if (clickCount % 2 == 0) 28f else -28f
        kineticAngleTarget += delta
        kineticSquishTarget = 0.93f
    }

    val kineticRotation by animateFloatAsState(
        targetValue = kineticAngleTarget,
        animationSpec =
            spring(
                dampingRatio = 0.84f, // High damping: viscous settling like kinetic sand
                stiffness = Spring.StiffnessVeryLow, // Slow, fluid movement
            ),
        label = "kinetic_sand_rotation",
    )

    val kineticSquish by animateFloatAsState(
        targetValue = kineticSquishTarget,
        animationSpec =
            spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessLow,
            ),
        label = "kinetic_squish",
        finishedListener = {
            if (kineticSquishTarget != 1f) {
                kineticSquishTarget = 1f
            }
        },
    )

    // Gentle ambient floating offset (subtle breathing, not spinning)
    val ambientTransition = rememberInfiniteTransition(label = "orb_ambient")
    val floatY by ambientTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "orb_float",
    )

    val faqs =
        remember(role) {
            when (role) {
                OperationalRole.CASHIER ->
                    listOf(
                        AssistantFaq(
                            id = "scan",
                            question = "¿Cómo entrego un pedido con QR?",
                            stepTitle = "Escanear QR de alumno",
                            stepInstruction =
                                "Toca el botón 'Escanear QR' resaltado en verde en la tarjeta de la orden. " +
                                    "Apunta la cámara al código que te muestre el alumno para validar la entrega al instante.",
                            targetHighlight = "scan_button",
                        ),
                        AssistantFaq(
                            id = "pause",
                            question = "¿Cómo pausar un producto agotado?",
                            stepTitle = "Pausar disponibilidad",
                            stepInstruction =
                                "Desactiva el interruptor verde resaltado junto al producto en la lista. " +
                                    "Se ocultará de inmediato en la app para todos los alumnos.",
                            targetHighlight = "product_switch",
                        ),
                        AssistantFaq(
                            id = "add",
                            question = "¿Cómo agregar un nuevo producto?",
                            stepTitle = "Nuevo producto al menú",
                            stepInstruction =
                                "Toca el botón circular (+) resaltado arriba a la derecha para subir la foto, " +
                                    "poner el nombre y el precio del nuevo platillo o bebida.",
                            targetHighlight = "add_product",
                        ),
                    )
                OperationalRole.KITCHEN ->
                    listOf(
                        AssistantFaq(
                            id = "prep",
                            question = "¿Cómo tomo una comanda en preparación?",
                            stepTitle = "Comenzar preparación",
                            stepInstruction =
                                "Toca 'Marcar en preparación' resaltado en la comanda. Esto avisa a caja y al alumno " +
                                    "que sus alimentos ya están en la plancha.",
                            targetHighlight = "prep_button",
                        ),
                        AssistantFaq(
                            id = "ready",
                            question = "¿Cómo aviso que la comida está lista?",
                            stepTitle = "Notificar comanda lista",
                            stepInstruction =
                                "Toca 'Comanda lista' resaltado en verde lima. Se enviará una notificación al alumno " +
                                    "para que pase inmediatamente a recoger su pedido.",
                            targetHighlight = "ready_button",
                        ),
                    )
                else -> emptyList()
            }
        }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        // Floating Bubble / Clippy Mode Overlay
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.92f),
            modifier =
                Modifier
                    .padding(bottom = 76.dp)
                    .width(320.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 18.dp,
                color = Color(0xF7FFFEF9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2E171816)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF96C83F)),
                            )
                            Text(
                                text = if (selectedFaq != null) "Guía en vivo" else "Asistente Vaiinilla",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFF171816),
                            )
                        }
                        IconButton(
                            onClick = {
                                triggerKineticReaction()
                                expanded = false
                                selectedFaq = null
                                onHighlightTarget(null)
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xFF73766D),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Clippy Step Guidance vs Question Selection
                    if (selectedFaq != null) {
                        val faq = selectedFaq!!
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Step Pill
                            Box(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x2696C83F))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "🎯 ${faq.stepTitle}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF2B4420),
                                )
                            }

                            // Interactive instruction
                            Text(
                                text = faq.stepInstruction,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF222520),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "‹ Ver preguntas",
                                    color = Color(0xFF55594F),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    modifier =
                                        Modifier
                                            .clickable {
                                                triggerKineticReaction()
                                                selectedFaq = null
                                                onHighlightTarget(null)
                                            }.padding(vertical = 6.dp, horizontal = 4.dp),
                                )

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFFB7DE63),
                                    shadowElevation = 4.dp,
                                    modifier =
                                        Modifier
                                            .height(36.dp)
                                            .clickable {
                                                triggerKineticReaction()
                                                expanded = false
                                                selectedFaq = null
                                                onHighlightTarget(null)
                                            },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF171816),
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            text = "¡Entendido!",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = Color(0xFF171816),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "¿En qué te puedo guiar?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF73766D),
                        )
                        faqs.forEach { faq ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF7F3E7))
                                        .clickable {
                                            triggerKineticReaction()
                                            selectedFaq = faq
                                            onHighlightTarget(faq.targetHighlight)
                                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x2696C83F)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFF436B1E),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = faq.question,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF171816),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Animated Orb Button with Kinetic Sand Physics
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(0, floatY.dp.roundToPx()) }
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x33171816))
                    .border(1.5.dp, Color(0x66B7DE63), CircleShape)
                    .shadow(16.dp, CircleShape, spotColor = Color(0x402A3E1B))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        triggerKineticReaction()
                        expanded = !expanded
                        if (!expanded) {
                            selectedFaq = null
                            onHighlightTarget(null)
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            // Ambient soft glow behind orb
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x3396C83F)),
            )

            // Kinetic Sand Orb: Smooth rotation & squish reaction on interaction
            Image(
                painter = painterResource(id = R.drawable.vaiinilla_assistant_orb),
                contentDescription = "Asistente Vaiinilla",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(54.dp)
                        .rotate(kineticRotation)
                        .scale(scaleX = 1f, scaleY = kineticSquish)
                        .clip(CircleShape),
            )

            // Glass highlight lens
            Box(
                modifier =
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0x40FFFFFF),
                                    Color(0x00FFFFFF),
                                ),
                            ),
                        ),
            )
        }
    }
}
