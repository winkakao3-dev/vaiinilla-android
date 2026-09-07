package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OperationalRole
import kotlin.math.cos
import kotlin.math.sin

private const val FLUID_ORB_SHADER = """
uniform float2 resolution;
uniform float time;
uniform float energy;
uniform float3 orbColor;

float2 hash22(float2 p) {
    p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float noise(float2 p) {
    float K1 = 0.366025404;
    float K2 = 0.211324865;
    float2 i = floor(p + (p.x + p.y) * K1);
    float2 a = p - i + (i.x + i.y) * K2;
    float2 o = step(a.yx, a.xy);
    float2 b = a - o + K2;
    float2 c = a - 1.0 + 2.0 * K2;
    float3 h = max(0.5 - float3(dot(a,a), dot(b,b), dot(c,c)), 0.0);
    float3 n = h*h*h*h * float3(
        dot(a, hash22(i)),
        dot(b, hash22(i + o)),
        dot(c, hash22(i + 1.0))
    );
    return dot(n, float3(70.0));
}

float fbm(float2 p) {
    float f = 0.0;
    f += 0.5000 * noise(p); p *= 2.01;
    f += 0.2500 * noise(p); p *= 2.02;
    f += 0.1250 * noise(p); p *= 2.03;
    f += 0.0625 * noise(p);
    return f;
}

half4 main(float2 fragCoord) {
    float2 safeResolution = max(resolution, float2(1.0));

    // WebGL's UV origin is bottom-left. Android's fragment coordinates start
    // top-left, so Y is flipped to preserve the Rare UI motion 1:1.
    float2 uv = float2(
        fragCoord.x / safeResolution.x,
        1.0 - (fragCoord.y / safeResolution.y)
    );

    // Rare UI fluid-orb source, ported line-for-line to AGSL.
    float t = time * 0.11;
    float2 p = uv * 3.0;

    float2 q = float2(
        fbm(p + float2(0.0, t * 0.7)),
        fbm(p + float2(5.2, t * 0.6))
    );

    float2 r = float2(
        fbm(p + 3.0*q + float2(1.7, 9.2) + t * 0.5),
        fbm(p + 3.0*q + float2(8.3, 2.8) + t * 0.45)
    );

    float f = fbm(p + 2.0*r);
    float diagonal = (uv.x + (1.0 - uv.y)) * 0.5;
    float flow = f * 0.28 + diagonal * 0.72;

    // Interaction only perturbs the original field briefly. At energy == 0
    // this is numerically the same motion as the Rare UI source.
    flow += energy * 0.035 * sin(uv.x * 8.0 + uv.y * 5.0 + t * 1.8);

    float3 cream = float3(0.969, 0.953, 0.906);
    float3 pale = mix(cream, orbColor, 0.30);
    float3 mid = mix(cream, orbColor, 0.65);
    float3 full = orbColor;

    float3 col;
    if (flow < 0.28) {
        col = cream;
    } else if (flow < 0.50) {
        col = mix(cream, pale, smoothstep(0.28, 0.50, flow));
    } else if (flow < 0.72) {
        col = mix(pale, mid, smoothstep(0.50, 0.72, flow));
    } else {
        col = mix(mid, full, smoothstep(0.72, 1.0, flow));
    }

    float warp = fbm(p * 1.3 + t * 0.3) * 0.06;
    col += warp * orbColor;

    return half4(half3(col), 1.0);
}
"""

@Composable
private fun FluidOrbField(
    reactionEnergy: Float,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val flow = rememberInfiniteTransition(label = "fluid_orb_flow")
    val shaderTime by
        flow.animateFloat(
            initialValue = 0f,
            targetValue = 1_000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1_000_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "fluid_orb_time",
        )

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        FluidRuntimeShaderField(
            time = shaderTime,
            reactionEnergy = reactionEnergy,
            isDarkTheme = isDarkTheme,
            modifier = modifier,
        )
    } else {
        // Pre-Android 13 fallback. The primary implementation above remains a real shader;
        // this only preserves compatibility with the app's minSdk 26 contract.
        Canvas(
            modifier =
                modifier.semantics {
                    contentDescription = "Asistente Vaiinilla"
                },
        ) {
            val diameter = size.minDimension
            val t = shaderTime * 0.31f
            val energy = reactionEnergy.coerceIn(0f, 1f)
            val colors =
                listOf(
                    Color(0xFF171816),
                    Color(0xFF35432A),
                    Color(0xFF96C83F),
                    Color(0xFFB7DE63),
                    Color(0xFFF7F3E7),
                )
            repeat(17) { index ->
                val phase = index * 1.73f
                val x = 0.5f + sin(t + phase) * (0.12f + (index % 3) * 0.025f)
                val y = 0.5f + cos(t * 0.81f - phase * 0.7f) * (0.12f + (index % 4) * 0.018f)
                drawCircle(
                    color = colors[index % colors.size].copy(alpha = 0.34f),
                    radius = diameter * (0.12f + (index % 5) * 0.018f + energy * 0.008f),
                    center = Offset(x * size.width, y * size.height),
                )
            }
        }
    }
}

@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.TIRAMISU)
@Composable
private fun FluidRuntimeShaderField(
    time: Float,
    reactionEnergy: Float,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val shader = remember { runCatching { android.graphics.RuntimeShader(FLUID_ORB_SHADER) }.getOrNull() }
    if (shader == null) {
        Box(modifier = modifier.background(Color(0xFF35432A)))
        return
    }

    Canvas(
        modifier =
            modifier.semantics {
                contentDescription = "Asistente Vaiinilla"
            },
    ) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("energy", reactionEnergy.coerceIn(0f, 1f))
        shader.setFloatUniform("orbColor", 0.588f, 0.784f, 0.247f)

        drawContext.canvas.nativeCanvas.drawRect(
            0f,
            0f,
            size.width,
            size.height,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
            },
        )
    }
}

data class AssistantGuideStep(
    val targetHighlight: String,
    val title: String,
    val instruction: String,
)

data class AssistantFaq(
    val id: String,
    val question: String,
    val answer: String,
    val steps: List<AssistantGuideStep>,
)

@Composable
fun VaiinillaAssistantOrb(
    role: OperationalRole,
    onHighlightTarget: (String?) -> Unit,
    modifier: Modifier = Modifier,
    reactionSignal: Int = 0,
    isDarkTheme: Boolean = false,
    contextLabel: String = "",
    guideActionSignal: Int = 0,
    onGuideAction: (String) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFaq by remember { mutableStateOf<AssistantFaq?>(null) }
    var guideActive by remember { mutableStateOf(false) }
    var guideStepIndex by remember { mutableIntStateOf(0) }
    var completedFaqId by remember { mutableStateOf<String?>(null) }
    var reactionTarget by remember { mutableFloatStateOf(0f) }

    fun triggerFluidReaction() {
        reactionTarget = if (reactionTarget < 0.5f) 1f else 0.72f
    }

    fun cancelGuide() {
        guideActive = false
        guideStepIndex = 0
        onHighlightTarget(null)
    }

    LaunchedEffect(reactionSignal) {
        if (reactionSignal > 0) triggerFluidReaction()
    }

    LaunchedEffect(guideActionSignal) {
        if (!guideActive || guideActionSignal <= 0) return@LaunchedEffect
        val faq = selectedFaq ?: return@LaunchedEffect
        triggerFluidReaction()
        val nextStep = guideStepIndex + 1
        if (nextStep < faq.steps.size) {
            guideStepIndex = nextStep
            onHighlightTarget(faq.steps[nextStep].targetHighlight)
        } else {
            completedFaqId = faq.id
            guideActive = false
            guideStepIndex = 0
            onHighlightTarget(null)
            expanded = true
        }
    }

    val reactionEnergy by
        animateFloatAsState(
            targetValue = reactionTarget,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
            label = "fluid_orb_reaction",
            finishedListener = { if (reactionTarget > 0f) reactionTarget = 0f },
        )

    val ambientTransition = rememberInfiniteTransition(label = "orb_ambient")
    val floatY by
        ambientTransition.animateFloat(
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
                            answer =
                                "Escanea el QR del alumno desde la comanda activa. " +
                                    "Al validarlo, la entrega queda registrada con el pedido correcto.",
                            steps =
                                listOf(
                                    AssistantGuideStep(
                                        targetHighlight = "scan_button",
                                        title = "Escanear QR",
                                        instruction =
                                            "Toca “Escanear QR” en la comanda activa y apunta la cámara " +
                                                "al código del alumno.",
                                    ),
                                ),
                        ),
                        AssistantFaq(
                            id = "pause",
                            question = "¿Cómo pauso un producto agotado?",
                            answer =
                                "Desactiva su disponibilidad. El producto deja de mostrarse como disponible " +
                                    "para nuevos pedidos.",
                            steps =
                                listOf(
                                    AssistantGuideStep(
                                        targetHighlight = "product_switch",
                                        title = "Disponibilidad del producto",
                                        instruction = "Toca el interruptor del producto que quieres pausar.",
                                    ),
                                ),
                        ),
                        AssistantFaq(
                            id = "add",
                            question = "¿Cómo agrego un nuevo producto?",
                            answer =
                                "Abre Nuevo producto, elige la foto, completa nombre, precio y estación, " +
                                    "y guarda cuando todo esté listo.",
                            steps =
                                listOf(
                                    AssistantGuideStep(
                                        targetHighlight = "add_product",
                                        title = "Nuevo producto",
                                        instruction =
                                            "Toca el botón + del catálogo para abrir el formulario " +
                                                "del nuevo producto.",
                                    ),
                                ),
                        ),
                    )
                OperationalRole.KITCHEN ->
                    listOf(
                        AssistantFaq(
                            id = "prep",
                            question = "¿Cómo marco una comanda en preparación?",
                            answer =
                                "Toca Preparando en la tarjeta activa. Caja y el alumno ven al instante que " +
                                    "los alimentos ya están en plancha y el estado cambia a “En preparación”.",
                            steps =
                                listOf(
                                    AssistantGuideStep(
                                        targetHighlight = "prep_button",
                                        title = "Preparando",
                                        instruction =
                                            "Toca “Preparando” en la comanda activa para avisar que los alimentos " +
                                                "ya están en proceso.",
                                    ),
                                    AssistantGuideStep(
                                        targetHighlight = "ready_button",
                                        title = "¡Listo!",
                                        instruction =
                                            "Cuando termines, toca “Ya se preparó”. La comanda pasa a entrega " +
                                                "y Caja recibe el cambio.",
                                    ),
                                ),
                        ),
                        AssistantFaq(
                            id = "ready",
                            question = "¿Cómo aviso que la comida está lista?",
                            answer =
                                "Marca la comanda como lista cuando todos sus productos terminaron. " +
                                    "Caja y el alumno reciben el nuevo estado.",
                            steps =
                                listOf(
                                    AssistantGuideStep(
                                        targetHighlight = "ready_button",
                                        title = "Ya se preparó",
                                        instruction =
                                            "Toca “Ya se preparó” en la comanda activa para enviarla a entrega.",
                                    ),
                                ),
                        ),
                    )
                else -> emptyList()
            }
        }

    val panel = if (isDarkTheme) Color(0xFF1A1C18) else Color(0xFFF7F3E7)
    val panelInner = if (isDarkTheme) Color(0xFF242720) else Color(0xFFECE7D8)
    val panelInk = if (isDarkTheme) Color(0xFFF7F3E7) else Color(0xFF171816)
    val panelMuted = if (isDarkTheme) Color(0xFFAAAFA2) else Color(0xFF666A60)
    val lime = Color(0xFFB7DE63)
    val limeInk = Color(0xFF171816)

    Box(modifier = modifier) {
        if (guideActive) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (isDarkTheme) 0.42f else 0.34f)),
            )
            val faq = selectedFaq
            val step = faq?.steps?.getOrNull(guideStepIndex)
            if (faq != null && step != null) {
                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 24.dp, end = 24.dp, bottom = 118.dp)
                            .fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = panel,
                    border = androidx.compose.foundation.BorderStroke(1.dp, lime.copy(alpha = 0.5f)),
                    shadowElevation = 18.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            text = "PASO ${guideStepIndex + 1} DE ${faq.steps.size}",
                            color = lime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                        )
                        Text(
                            text = "Toca “${step.title}”",
                            color = panelInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = step.instruction,
                            color = panelMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Cancelar guía",
                                color = panelMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier =
                                    Modifier
                                        .clickable {
                                            triggerFluidReaction()
                                            cancelGuide()
                                            expanded = true
                                        }.padding(vertical = 9.dp, horizontal = 4.dp),
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = panelInner,
                                modifier =
                                    Modifier.clickable {
                                        triggerFluidReaction()
                                        onGuideAction(step.targetHighlight)
                                    },
                            ) {
                                Text(
                                    text = "Hazlo por mí",
                                    color = panelInk,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded && !guideActive,
            enter = fadeIn(tween(170)) + scaleIn(initialScale = 0.97f),
            exit = fadeOut(tween(130)) + scaleOut(targetScale = 0.97f),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 18.dp,
                color = panel,
                border = androidx.compose.foundation.BorderStroke(1.dp, panelMuted.copy(alpha = 0.16f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(lime))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Asistente\nVaiinilla",
                            color = panelInk,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            lineHeight = 13.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        if (contextLabel.isNotBlank()) {
                            Text(
                                text = contextLabel,
                                color = panelMuted,
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.width(92.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                triggerFluidReaction()
                                expanded = false
                                selectedFaq = null
                                completedFaqId = null
                                onHighlightTarget(null)
                            },
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Cerrar asistente",
                                tint = panelMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    val faq = selectedFaq
                    if (faq == null) {
                        Text(
                            text =
                                when (role) {
                                    OperationalRole.KITCHEN ->
                                        "Elige una pregunta y te guío sobre esta misma pantalla."
                                    OperationalRole.CASHIER ->
                                        "Puedo señalarte exactamente dónde hacer cada tarea."
                                    else -> "¿En qué te puedo guiar?"
                                },
                            color = panelMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                        Text(
                            text = "SUGERIDAS AHORA",
                            color = panelMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                        )
                        faqs.forEach { option ->
                            AssistantQuestionRow(
                                text = option.question,
                                background = panelInner,
                                ink = panelInk,
                                lime = lime,
                                onClick = {
                                    triggerFluidReaction()
                                    selectedFaq = option
                                },
                            )
                        }
                    } else {
                        Text(
                            text = "‹ Preguntas",
                            color = panelMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.clickable {
                                    triggerFluidReaction()
                                    selectedFaq = null
                                    completedFaqId = null
                                },
                        )
                        if (completedFaqId == faq.id) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(lime.copy(alpha = if (isDarkTheme) 0.14f else 0.22f))
                                        .border(
                                            1.dp,
                                            lime.copy(alpha = 0.32f),
                                            RoundedCornerShape(8.dp),
                                        ).padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = lime,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "Guía completada en esta pantalla",
                                    color = if (isDarkTheme) lime else Color(0xFF45621F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            text = faq.question,
                            color = panelInk,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = faq.answer,
                            color = panelMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = lime,
                            modifier =
                                Modifier.clickable {
                                    if (faq.steps.isEmpty()) return@clickable
                                    triggerFluidReaction()
                                    guideStepIndex = 0
                                    guideActive = true
                                    expanded = false
                                    completedFaqId = null
                                    onHighlightTarget(faq.steps.first().targetHighlight)
                                },
                        ) {
                            Text(
                                text = "Iniciar guía en vivo",
                                color = limeInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                        Text(
                            text = "SIGUIENTES PREGUNTAS",
                            color = panelMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                        )
                        faqs.filterNot { it.id == faq.id }.take(2).forEach { option ->
                            AssistantQuestionRow(
                                text = option.question,
                                background = panelInner,
                                ink = panelInk,
                                lime = lime,
                                onClick = {
                                    triggerFluidReaction()
                                    selectedFaq = option
                                    completedFaqId = null
                                },
                            )
                        }
                    }
                }
            }
        }

        if (!expanded || guideActive) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 10.dp)
                        .offset { IntOffset(0, floatY.dp.roundToPx()) }
                        .size(68.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            triggerFluidReaction()
                            if (guideActive) {
                                cancelGuide()
                                expanded = true
                            } else {
                                expanded = !expanded
                                if (!expanded) {
                                    selectedFaq = null
                                    completedFaqId = null
                                    onHighlightTarget(null)
                                }
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                FluidOrbField(
                    reactionEnergy = reactionEnergy,
                    isDarkTheme = isDarkTheme,
                    modifier =
                        Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .scale(1f + reactionEnergy * 0.045f),
                )
            }
        }
    }
}

@Composable
private fun AssistantQuestionRow(
    text: String,
    background: Color,
    ink: Color,
    lime: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(16.dp).clip(CircleShape).background(lime.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(lime))
        }
        Text(
            text = text,
            color = ink,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}
