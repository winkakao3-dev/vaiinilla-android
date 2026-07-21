package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    categoryName: String,
    selectedOptionIds: Set<Int>,
    quantity: Int,
    previewPrice: String,
    previewTotal: String,
    canAdd: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onToggleOption: (groupId: Int, optionId: Int) -> Unit,
    onClearOptionalGroup: (groupId: Int) -> Unit,
    onQuantityChange: (delta: Int) -> Unit,
    onAdd: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                ),
            color = Cream,
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
            shadowElevation = 26.dp,
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .width(42.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Ink.copy(alpha = 0.14f))
                        .align(Alignment.CenterHorizontally),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(CreamDeep),
                    ) {
                        ProductImage(
                            imageUrl = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            color = Ink,
                            shape = RoundedCornerShape(17.dp),
                        ) {
                            Text(
                                text = moneyLabel(previewPrice),
                                color = Cream,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = categoryName.uppercase(),
                                color = MutedInk,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = product.name,
                                color = Ink,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CreamDeep),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                        }
                    }
                    Text(
                        text = product.description,
                        color = MutedInk,
                        modifier = Modifier.padding(top = 9.dp),
                    )

                    product.optionGroups.forEach { group ->
                        OptionGroupSection(
                            group = group,
                            selectedOptionIds = selectedOptionIds,
                            onToggleOption = onToggleOption,
                            onClearOptionalGroup = onClearOptionalGroup,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MetaRow("Ingredientes", product.ingredients)
                        MetaRow("Tiempo estimado", "${product.estimatedTimeMinutes} min")
                        MetaRow("Alérgenos", product.allergens)
                    }
                    Spacer(Modifier.height(18.dp))
                }

                Surface(color = Cream, shadowElevation = 14.dp) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tu selección", color = MutedInk, fontWeight = FontWeight.Bold)
                                val selection = product.optionGroups
                                    .flatMap { it.options }
                                    .filter { it.id in selectedOptionIds }
                                    .joinToString(" · ") { it.name }
                                Text(
                                    text = selection.ifBlank { "Sin opciones adicionales" },
                                    color = Ink,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            QuantityControl(
                                quantity = quantity,
                                onMinus = { onQuantityChange(-1) },
                                onPlus = { onQuantityChange(1) },
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onAdd,
                            enabled = canAdd,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Lime,
                                contentColor = Ink,
                                disabledContainerColor = CreamDeep,
                                disabledContentColor = MutedInk,
                            ),
                        ) {
                            Text(
                                text = "Agregar · ${moneyLabel(previewTotal)}",
                                fontWeight = FontWeight.Black,
                            )
                        }
                        errorMessage?.let { message ->
                            Text(
                                text = message,
                                color = Color(0xFF9D2E25),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGroupSection(
    group: OptionGroup,
    selectedOptionIds: Set<Int>,
    onToggleOption: (Int, Int) -> Unit,
    onClearOptionalGroup: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(group.name, color = Ink, fontWeight = FontWeight.ExtraBold)
            Text(
                text = if (group.minimumSelections > 0) "Obligatorio" else "Opcional",
                color = MutedInk,
                fontWeight = FontWeight.Bold,
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (group.minimumSelections == 0) {
                val anySelected = group.options.any { it.id in selectedOptionIds }
                OptionChip(
                    text = "Sin extra",
                    selected = !anySelected,
                    onClick = { onClearOptionalGroup(group.id) },
                )
            }
            group.options.forEach { option ->
                val extra = if (option.extraPrice == "0.00") "" else " +${moneyLabel(option.extraPrice)}"
                OptionChip(
                    text = option.name + extra,
                    selected = option.id in selectedOptionIds,
                    onClick = { onToggleOption(group.id, option.id) },
                )
            }
        }
    }
}

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) Lime else CreamDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = text, color = Ink, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Surface(
        color = CreamDeep,
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(label, color = Ink, fontWeight = FontWeight.ExtraBold)
            Text(value, color = MutedInk, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun QuantityControl(
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onMinus,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Ink),
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Quitar uno", tint = Cream)
        }
        Text(
            text = quantity.toString(),
            color = Ink,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        IconButton(
            onClick = onPlus,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Ink),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Agregar uno", tint = Cream)
        }
    }
}
