package com.thiago.apk_mobile.presentation.pedidos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiago.apk_mobile.data.DetallePedido
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory

@Composable
fun PedidosScreen(
    inventarioViewModel: InventarioViewModel = viewModel(factory = getInventarioViewModelFactory(LocalContext.current))
) {
    val listaSimulada = remember {
        mutableStateListOf(
            DetallePedido(1, "Clavos 2 pulgadas", 50, 0.10),
            DetallePedido(2, "Martillo Galponero", 5, 15.0),
            DetallePedido(3, "Pintura Blanca 1G", 2, 25.0)
        )
    }

    val itemsSeleccionados = remember { mutableStateMapOf<Int, Boolean>() }
    val cantidadSeleccionados = itemsSeleccionados.values.count { it }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Recepción de Pedidos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(listaSimulada) { item ->
                val isChecked = itemsSeleccionados[item.detalleId] ?: false
                PedidoItemRow(
                    detalle = item,
                    isChecked = isChecked,
                    onCheckedChange = { checked -> itemsSeleccionados[item.detalleId] = checked }
                )
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { itemsSeleccionados.clear() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    val seleccionados = listaSimulada.filter { itemsSeleccionados[it.detalleId] == true }
                    inventarioViewModel.recibirPedido(seleccionados)
                    itemsSeleccionados.clear()
                },
                modifier = Modifier.weight(1f),
                enabled = cantidadSeleccionados > 0
            ) {
                Text("Aceptar ($cantidadSeleccionados)")
            }
        }
    }
}

@Composable
fun PedidoItemRow(detalle: DetallePedido, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!isChecked) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = detalle.nombre, style = MaterialTheme.typography.titleLarge)
                Text(text = "Cantidad esperada: ${detalle.cantidadPedida}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(text = "$${detalle.precioUnitario}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}