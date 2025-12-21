package com.thiago.apk_mobile.presentation.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiago.apk_mobile.data.DetallePedido
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory

@Composable
fun PedidosScreen(
    inventarioViewModel: InventarioViewModel = viewModel(factory = getInventarioViewModelFactory(LocalContext.current))
) {
    // 1. Lista de productos del pedido (Usamos mutableStateListOf para que sea reactiva)
    val listaPedidos = remember {
        mutableStateListOf(
            DetallePedido(1, "Clavos 2 pulgadas", 50, 0.10),
            DetallePedido(2, "Martillo Galponero", 5, 15.0),
            DetallePedido(3, "Pintura Blanca 1G", 2, 25.0),
            DetallePedido(4, "Taladro Percutor", 1, 85.0)
        )
    }

    // 2. Estado para el checklist (ID -> Booleano)
    val itemsSeleccionados = remember { mutableStateMapOf<Int, Boolean>() }
    val cantidadSeleccionados = itemsSeleccionados.values.count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recepción de Mercancía",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Verifica las cantidades recibidas y marca los productos para ingresar al inventario.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 3. Lista de productos con edición
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listaPedidos) { item ->
                val isChecked = itemsSeleccionados[item.detalleId] ?: false

                PedidoItemRow(
                    detalle = item,
                    isChecked = isChecked,
                    onCheckedChange = { checked ->
                        itemsSeleccionados[item.detalleId] = checked
                    },
                    onCantidadChanged = { nuevaCant ->
                        // Actualizamos el objeto en la lista reactiva
                        val index = listaPedidos.indexOfFirst { it.detalleId == item.detalleId }
                        if (index != -1) {
                            listaPedidos[index] = item.copy(cantidadPedida = nuevaCant)
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // BOTÓN CANCELAR
            OutlinedButton(
                onClick = {
                    itemsSeleccionados.clear()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Desmarcar Todo")
            }

            // BOTÓN ACEPTAR
            Button(
                onClick = {
                    // Filtramos solo lo que el usuario marcó como "recibido"
                    val productosARecibir = listaPedidos.filter { itemsSeleccionados[it.detalleId] == true }

                    // Enviamos al ViewModel para procesar en la DB
                    inventarioViewModel.recibirPedido(productosARecibir)

                    // Limpiamos selección después del éxito
                    itemsSeleccionados.clear()
                },
                modifier = Modifier.weight(1f),
                enabled = cantidadSeleccionados > 0
            ) {
                Text("Recibir ($cantidadSeleccionados)")
            }
        }
    }
}

@Composable
fun PedidoItemRow(
    detalle: DetallePedido,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onCantidadChanged: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checklist
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )

            // Información del Producto
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = detalle.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Unitario: $${detalle.precioUnitario}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Entrada de cantidad (Manejo de discrepancias)
            OutlinedTextField(
                value = if (detalle.cantidadPedida == 0) "" else detalle.cantidadPedida.toString(),
                onValueChange = { newValue ->
                    // Solo permitimos números
                    if (newValue.all { it.isDigit() }) {
                        onCantidadChanged(newValue.toIntOrNull() ?: 0)
                    }
                },
                label = { Text("Llegó") },
                modifier = Modifier.width(90.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}