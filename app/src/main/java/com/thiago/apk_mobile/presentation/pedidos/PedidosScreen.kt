package com.thiago.apk_mobile.presentation.pedidos

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiago.apk_mobile.data.DetallePedido
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    viewModel: InventarioViewModel = viewModel(factory = getInventarioViewModelFactory(LocalContext.current))
) {
    // Lista de productos en el pedido actual (Estado local para rapidez)
    val listaPedidos = remember { mutableStateListOf<DetallePedido>() }
    val itemsSeleccionados = remember { mutableStateMapOf<Int, Boolean>() }

    var textoBusqueda by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gestión de Pedidos", style = MaterialTheme.typography.headlineMedium)

        // 1. BARRA DE BÚSQUEDA / AGREGAR
        OutlinedTextField(
            value = textoBusqueda,
            onValueChange = { textoBusqueda = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            placeholder = { Text("Buscar o agregar producto...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (textoBusqueda.isNotEmpty()) {
                    IconButton(onClick = {
                        // Lógica: Agregar a la lista
                        val nuevoId = (listaPedidos.maxOfOrNull { it.detalleId } ?: 0) + 1
                        listaPedidos.add(DetallePedido(nuevoId, textoBusqueda, 1, 0.0))
                        textoBusqueda = ""
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
                    }
                }
            },
            singleLine = true
        )

        // 2. LISTA DE PRODUCTOS
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(listaPedidos, key = { it.detalleId }) { item ->
                val isChecked = itemsSeleccionados[item.detalleId] ?: false

                // Animación al eliminar
                AnimatedVisibility(visible = true, exit = fadeOut()) {
                    PedidoItemRow(
                        detalle = item,
                        isChecked = isChecked,
                        onCheckedChange = { itemsSeleccionados[item.detalleId] = it },
                        onCantidadChanged = { nuevaCant ->
                            val index = listaPedidos.indexOfFirst { it.detalleId == item.detalleId }
                            if (index != -1) listaPedidos[index] = item.copy(cantidadPedida = nuevaCant)
                        },
                        onDelete = {
                            listaPedidos.remove(item)
                            itemsSeleccionados.remove(item.detalleId)
                        }
                    )
                }
                HorizontalDivider()
            }
        }

        // 3. BOTONES DE ACCIÓN
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { listaPedidos.clear(); itemsSeleccionados.clear() },
                modifier = Modifier.weight(1f)
            ) { Text("Limpiar Todo") }

            Button(
                onClick = {
                    // Obtener marcados
                    val marcados = listaPedidos.filter { itemsSeleccionados[it.detalleId] == true }

                    // Procesar en Inventario
                    viewModel.recibirPedido(marcados)

                    // PUNTO 2: Eliminar de la vista los que fueron procesados
                    listaPedidos.removeAll(marcados)
                    itemsSeleccionados.clear()
                },
                modifier = Modifier.weight(1f),
                enabled = itemsSeleccionados.any { it.value }
            ) {
                Text("Aceptar (${itemsSeleccionados.values.count { it }})")
            }
        }
    }
}

@Composable
fun PedidoItemRow(
    detalle: DetallePedido,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onCantidadChanged: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)

        Column(modifier = Modifier.weight(1f)) {
            Text(detalle.nombre, style = MaterialTheme.typography.titleMedium)
            // Campo de cantidad pequeño
            OutlinedTextField(
                value = detalle.cantidadPedida.toString(),
                onValueChange = { if (it.all { c -> c.isDigit() }) onCantidadChanged(it.toIntOrNull() ?: 0) },
                modifier = Modifier.width(80.dp),
                label = { Text("Cant.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
        }
    }
}