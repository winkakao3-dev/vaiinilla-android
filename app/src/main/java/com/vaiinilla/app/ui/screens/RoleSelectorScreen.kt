package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.AccentInk
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.Ink2
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.Yolk
import com.vaiinilla.app.ui.components.TestOnlyModeBadge
import com.vaiinilla.app.ui.components.TestOnlyModeCard
import com.vaiinilla.app.ui.components.ThemeCycleButton
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun RoleSelectorScreen(
    testOnlyMode: Boolean,
    onTestOnlyModeChange: (Boolean) -> Unit,
    onRoleSelected: (OperationalRole) -> Unit,
    onOpenDemoGallery: () -> Unit,
    loadingRole: OperationalRole? = null,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val isLoading = loadingRole != null
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.ink, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("VA", color = colors.paper, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Text(
                    "Vaiinilla",
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.ink,
                )
                ThemeCycleButton()
                if (testOnlyMode) {
                    TestOnlyModeBadge(modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        item {
            TestOnlyModeCard(
                enabled = testOnlyMode,
                onEnabledChange = onTestOnlyModeChange,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (errorMessage != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.coral.copy(alpha = 0.15f),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            errorMessage,
                            color = colors.ink,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        TextButton(onClick = onDismissError) {
                            Text("Cerrar", color = colors.ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextButton(onClick = onOpenDemoGallery) {
                    Text(
                        "Ver todas las fases",
                        color = colors.ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
                Text(
                    "Salta a cualquier pantalla con fixtures locales.",
                    color = colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    "V",
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = colors.accent.copy(alpha = 0.18f),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Black,
                )
                Column(modifier = Modifier.padding(top = 8.dp, end = 12.dp)) {
                    Text(
                        "COMEDOR CONECTADO",
                        color = colors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        "Come mejor.\nEspera menos.",
                        color = colors.ink,
                        fontSize = 34.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        "Una sola demo para pedir, cobrar, preparar, entregar y administrar.",
                        color = colors.muted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .physicalPress(
                                enabled = !isLoading,
                                onClick = { onRoleSelected(OperationalRole.CLIENT) },
                            ),
                        color = colors.ink,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (loadingRole == OperationalRole.CLIENT) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.paper,
                                )
                            }
                            Text(
                                "Entrar como alumno",
                                color = colors.paper,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Elige una vista", color = colors.ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("5 roles", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        item {
            val options = roleOptions(colors)
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    options.take(2).forEach { option ->
                        RoleCard(
                            option = option,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            loading = option.role == loadingRole,
                            onClick = { option.role?.let(onRoleSelected) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    options.drop(2).take(2).forEach { option ->
                        RoleCard(
                            option = option,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            loading = option.role == loadingRole,
                            onClick = { option.role?.let(onRoleSelected) },
                        )
                    }
                }
                RoleCard(
                    option = options[4],
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    loading = options[4].role == loadingRole,
                    onClick = { options[4].role?.let(onRoleSelected) },
                )
            }
        }
    }
}

private data class RoleOption(
    val role: OperationalRole?,
    val title: String,
    val subtitle: String,
    val icon: String,
    val background: Color,
    val contentColor: Color,
)

private fun roleOptions(colors: com.vaiinilla.app.ui.theme.VaiinillaColors) = listOf(
    RoleOption(
        role = OperationalRole.CLIENT,
        title = "Alumno",
        subtitle = "Menú, pedido, seguimiento y saldo.",
        icon = "☻",
        background = Lime,
        contentColor = AccentInk,
    ),
    RoleOption(
        role = OperationalRole.CASHIER,
        title = "Caja",
        subtitle = "Cobros, entregas y recargas.",
        icon = "▣",
        background = Yolk,
        contentColor = Color(0xFF28200B),
    ),
    RoleOption(
        role = OperationalRole.KITCHEN,
        title = "Cocina",
        subtitle = "Comandas y preparación.",
        icon = "♨",
        background = Ink2,
        contentColor = Color(0xFFF7F4E9),
    ),
    RoleOption(
        role = OperationalRole.WAITER,
        title = "Mesero",
        subtitle = "Pedidos listos para mesa.",
        icon = "⌁",
        background = Coral,
        contentColor = Color(0xFF2C100E),
    ),
    RoleOption(
        role = null,
        title = "Administración",
        subtitle = "Reportes, menú, promociones e integraciones.",
        icon = "⌘",
        background = colors.paper2,
        contentColor = colors.ink,
    ),
)

@Composable
private fun RoleCard(
    option: RoleOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Surface(
        modifier = modifier
            .height(148.dp)
            .physicalPress(
                scale = PhysicalPressScale.Default,
                enabled = enabled,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(28.dp),
        color = option.background.copy(alpha = if (enabled) 1f else 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = option.contentColor,
                )
            } else {
                Text(option.icon, fontSize = 29.sp, color = option.contentColor)
            }
            Column {
                Text(
                    option.title,
                    color = option.contentColor,
                    fontSize = 19.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    option.subtitle,
                    color = option.contentColor.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}
