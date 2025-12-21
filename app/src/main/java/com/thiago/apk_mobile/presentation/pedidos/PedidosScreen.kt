package com.thiago.apk_mobile.presentation.pedidos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiago.apk_mobile.data.DetallePedido
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    viewModel: InventarioViewModel = viewModel(factory = getInventarioViewModelFactory(LocalContext.current))
) {
    val listaPedidos = remember { mutableStateListOf<DetallePedido>() }
    val itemsSeleccionados = remember { mutableStateMapOf<Int, Boolean>() }

    var textoBusqueda by remember { mutableStateOf("") }
    var mostrarSugerencias by remember { mutableStateOf(false) }
    var mostrarDialogoProducto by remember { mutableStateOf(false) }
    var productoAEditar by remember { mutableStateOf<DetallePedido?>(null) }

    val sugerencias by viewModel.sugerenciasBusqueda.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Recepción Inteligente", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // --- BARRA DE BÚSQUEDA Y SUGERENCIAS ---
        Box(modifier = Modifier.fillMaxWidth().zIndex(1f)) {
            Column {
                OutlinedTextField(
                    value = textoBusqueda,
                    onValueChange = {
                        textoBusqueda = it
                        viewModel.onSearchQueryChange(it)
                        mostrarSugerencias = it.isNotEmpty()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    placeholder = { Text("Buscar en inventario...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            productoAEditar = null
                            mostrarDialogoProducto = true
                        }) { Icon(Icons.Default.Add, null) }
                    },
                    singleLine = true
                )

                if (mostrarSugerencias && sugerencias.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(sugerencias) { producto ->
                                ListItem(
                                    headlineContent = { Text(producto.nombre) },
                                    supportingContent = { Text("Stock: ${producto.cantidadEnStock} • Precio: $${producto.precio}") },
                                    modifier = Modifier.clickable {
                                        // AUTOCOMPLETAR NOMBRE Y PRECIO
                                        productoAEditar = DetallePedido(
                                            detalleId = 0,
                                            nombre = producto.nombre,
                                            cantidadPedida = 1,
                                            precioUnitario = producto.precio
                                        )
                                        textoBusqueda = producto.nombre
                                        mostrarSugerencias = false
                                        focusManager.clearFocus()
                                        mostrarDialogoProducto = true
                                    }
                                )
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // --- LISTA DE PRODUCTOS DEL PEDIDO ---
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(listaPedidos) { item ->
                PedidoItemRow(
                    detalle = item,
                    isChecked = itemsSeleccionados[item.detalleId] ?: false,
                    onCheckedChange = { itemsSeleccionados[item.detalleId] = it },
                    onEdit = {
                        productoAEditar = item
                        mostrarDialogoProducto = true
                    },
                    onDelete = {
                        listaPedidos.remove(item)
                        itemsSeleccionados.remove(item.detalleId)
                    }
                )
                HorizontalDivider()
            }
        }

        // --- BOTONES ---
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { listaPedidos.clear(); itemsSeleccionados.clear() }, modifier = Modifier.weight(1f)) {
                Text("Limpiar")
            }
            Button(
                onClick = {
                    val marcados = listaPedidos.filter { itemsSeleccionados[it.detalleId] == true }
                    viewModel.recibirPedido(marcados)
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

    if (mostrarDialogoProducto) {
        DialogProductoPedido(
            nombreInicial = productoAEditar?.nombre ?: textoBusqueda,
            cantidadInicial = productoAEditar?.cantidadPedida ?: 1,
            precioInicial = productoAEditar?.precioUnitario ?: 0.0,
            onDismiss = { mostrarDialogoProducto = false; productoAEditar = null },
            onConfirm = { nombre, cant, precio ->
                if (productoAEditar != null && productoAEditar!!.detalleId != 0) {
                    // Estamos editando uno que ya estaba en la lista de abajo
                    val index = listaPedidos.indexOfFirst { it.detalleId == productoAEditar!!.detalleId }
                    if (index != -1) listaPedidos[index] = productoAEditar!!.copy(nombre = nombre, cantidadPedida = cant, precioUnitario = precio)
                } else {
                    // Es un producto nuevo o de sugerencia
                    val nuevoId = (listaPedidos.maxOfOrNull { it.detalleId } ?: 0) + 1
                    listaPedidos.add(DetallePedido(nuevoId, nombre, cant, precio))
                }
                textoBusqueda = ""
                mostrarDialogoProducto = false
                productoAEditar = null
            }
        )
    }
}

@Composable
fun DialogProductoPedido(
    nombreInicial: String,
    cantidadInicial: Int,
    precioInicial: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Double) -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var cantidad by remember { mutableStateOf(cantidadInicial.toString()) }
    var precio by remember { mutableStateOf(precioInicial.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Datos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { if (it.all { c -> c.isDigit() }) cantidad = it },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio Unitario") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(nombre, cantidad.toIntOrNull() ?: 0, precio.toDoubleOrNull() ?: 0.0) }) {
                Text("Agregar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun PedidoItemRow(
    detalle: DetallePedido,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(detalle.nombre, style = MaterialTheme.typography.titleMedium)
            Text("Cant: ${detalle.cantidadPedida} • Precio: $${detalle.precioUnitario}", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
    }
}