package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OperationalRole

data class AssistantFaq(
    val id: String,
    val question: String,
    val answer: String,
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

    val infiniteTransition = rememberInfiniteTransition(label = "orb_ambient")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 6500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "orb_rotation",
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "orb_pulse",
    )

    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
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
                            answer =
                                "Toca Escanear QR en la tarjeta del pedido. " +
                                    "La app abrirá el escáner para confirmar el " +
                                    "código del alumno y marcarlo entregado de inmediato.",
                            targetHighlight = "scan_button",
                        ),
                        AssistantFaq(
                            id = "pause_product",
                            question = "¿Cómo pauso un producto agotado?",
                            answer =
                                "Usa el switch al lado de cada producto en la " +
                                    "sección Productos. El cambio se refleja " +
                                    "en tiempo real en la tienda del alumno.",
                            targetHighlight = "product_switch",
                        ),
                        AssistantFaq(
                            id = "add_product",
                            question = "¿Cómo agrego un nuevo producto?",
                            answer =
                                "Toca el botón circular + en el encabezado " +
                                    "de Productos para abrir la ficha de creación " +
                                    "con nombre, precio y foto.",
                            targetHighlight = "add_product",
                        ),
                    )
                OperationalRole.KITCHEN ->
                    listOf(
                        AssistantFaq(
                            id = "prep",
                            question = "¿Cómo indico que empecé la comanda?",
                            answer =
                                "Toca el botón Preparando en el ticket activo. " +
                                    "Esto actualiza el estado visible tanto para caja " +
                                    "como en el seguimiento del alumno.",
                            targetHighlight = "prep_button",
                        ),
                        AssistantFaq(
                            id = "ready",
                            question = "¿Cómo aviso que ya está listo?",
                            answer =
                                "Cuando termines todos los productos de la comanda, " +
                                    "pulsa Ya se preparó. El ticket pasará a estado Listo " +
                                    "y se notificará a Caja para su entrega.",
                            targetHighlight = "ready_button",
                        ),
                        AssistantFaq(
                            id = "queue",
                            question = "¿Dónde veo las siguientes comandas?",
                            answer =
                                "En la sección Siguientes verás la cola ordenada con " +
                                    "número de ticket, cantidad de productos y minutos transcurridos.",
                            targetHighlight = "queue_section",
                        ),
                    )
                else ->
                    listOf(
                        AssistantFaq(
                            id = "general",
                            question = "¿Cómo funciona este modo?",
                            answer = "Gestiona tus actividades operativas en tiempo real sin salir de tu turno.",
                        ),
                    )
            }
        }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        // Chatbot Popup Card
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 3 } + scaleIn(tween(250), 0.85f),
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 3 } + scaleOut(tween(200), 0.85f),
            modifier = Modifier.padding(bottom = 76.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.88f),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 14.dp,
                color = Color(0xF7FFFEF9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24171816)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
                                text = "Guía Vaiinilla",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF171816),
                            )
                        }
                        IconButton(
                            onClick = {
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

                    if (selectedFaq != null) {
                        val faq = selectedFaq!!
                        Text(
                            text = faq.question,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF171816),
                        )
                        Text(
                            text = faq.answer,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF30332E),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                text = "Hacer otra pregunta",
                                color = Color(0xFF304427),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier =
                                    Modifier
                                        .clickable {
                                            selectedFaq = null
                                            onHighlightTarget(null)
                                        }.padding(vertical = 4.dp),
                            )
                        }
                    } else {
                        Text(
                            text = "¿En qué te puedo ayudar?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF73766D),
                        )
                        faqs.forEach { faq ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFF7F3E7))
                                        .clickable {
                                            selectedFaq = faq
                                            onHighlightTarget(faq.targetHighlight)
                                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFF96C83F),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = faq.question,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF171816),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Animated Orb Button
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(0, floatY.dp.roundToPx()) }
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color(0x7AFFFEF9))
                    .border(1.5.dp, Color(0xE6FFFFFF), CircleShape)
                    .shadow(16.dp, CircleShape, spotColor = Color(0x30304427))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        expanded = !expanded
                        if (!expanded) {
                            selectedFaq = null
                            onHighlightTarget(null)
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            // Ambient outer glow ring
            Box(
                modifier =
                    Modifier
                        .size(54.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0x33B7DE63)),
            )

            // Inner swirling multi-gradient core
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF304427),
                                    Color(0xFF96C83F),
                                    Color(0xFFF7F3E7),
                                    Color(0xFFB7DE63),
                                    Color(0xFFD7F49A),
                                    Color(0xFF304427),
                                ),
                            ),
                        ),
            )

            // Gloss highlight overlay
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0x99FFFFFF),
                                    Color(0x00FFFFFF),
                                ),
                            ),
                        ),
            )
        }
    }
}
