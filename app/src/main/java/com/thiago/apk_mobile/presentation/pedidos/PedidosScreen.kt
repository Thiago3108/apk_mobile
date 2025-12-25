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
    val listaPedidos by viewModel.detallesPedido.collectAsStateWithLifecycle()
    val itemsSeleccionados = remember { mutableStateMapOf<Int, Boolean>() }

    var textoBusqueda by remember { mutableStateOf("") }
    var mostrarSugerencias by remember { mutableStateOf(false) }
    var mostrarDialogoProducto by remember { mutableStateOf(false) }
    var productoAEditar by remember { mutableStateOf<DetallePedido?>(null) }
    var itemParaBorrar by remember { mutableStateOf<DetallePedido?>(null) }

    val sugerencias by viewModel.sugerenciasBusqueda.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(listaPedidos) {
        val idsEnLista = listaPedidos.map { it.detalleId }.toSet()
        itemsSeleccionados.keys.retainAll { it in idsEnLista }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Recepción Inteligente", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

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
                    placeholder = { Text("Buscar o añadir producto...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            productoAEditar = null
                            mostrarDialogoProducto = true
                        }) { Icon(Icons.Default.Add, "Añadir producto nuevo") }
                    },
                    singleLine = true
                )

                if (mostrarSugerencias && sugerencias.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(sugerencias) { producto ->
                                ListItem(
                                    headlineContent = { Text(producto.nombre) },
                                    supportingContent = { Text("Stock: ${producto.cantidadEnStock} • Precio: $${producto.precio}") },
                                    modifier = Modifier.clickable {
                                        productoAEditar = DetallePedido(nombre = producto.nombre, cantidadPedida = 1, precioUnitario = producto.precio)
                                        textoBusqueda = producto.nombre
                                        mostrarSugerencias = false
                                        focusManager.clearFocus()
                                        mostrarDialogoProducto = true
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            if (listaPedidos.isEmpty()) {
                item {
                    Text(
                        "No hay productos en el pedido. Usa la barra de búsqueda para añadir.",
                        modifier = Modifier.padding(top = 32.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(listaPedidos, key = { it.detalleId }) { item ->
                PedidoItemRow(
                    detalle = item,
                    isChecked = itemsSeleccionados[item.detalleId] ?: false,
                    onCheckedChange = { isChecked -> itemsSeleccionados[item.detalleId] = isChecked },
                    onEdit = {
                        productoAEditar = item
                        mostrarDialogoProducto = true
                    },
                    onDelete = { itemParaBorrar = item }
                )
                HorizontalDivider()
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // --- BOTÓN "LIMPIAR" MODIFICADO ---
            OutlinedButton(
                onClick = { itemsSeleccionados.clear() }, // Solo limpia el estado de la UI
                modifier = Modifier.weight(1f),
                enabled = itemsSeleccionados.any { it.value } // Se activa solo si hay algo seleccionado
            ) {
                Text("Deseleccionar") // Nuevo texto
            }
            
            Button(
                onClick = {
                    val marcados = listaPedidos.filter { itemsSeleccionados[it.detalleId] == true }
                    viewModel.recibirPedido(marcados)
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
        DialogoProductoPedido(
            itemInicial = productoAEditar,
            nombreSugerido = textoBusqueda,
            onDismiss = { mostrarDialogoProducto = false; productoAEditar = null },
            onConfirm = { detalle ->
                viewModel.agregarDetallePedido(detalle)
                textoBusqueda = ""
                mostrarDialogoProducto = false
                productoAEditar = null
            }
        )
    }

    itemParaBorrar?.let { item ->
        DialogoConfirmacion(
            titulo = "Eliminar Producto",
            mensaje = "¿Seguro que quieres quitar '${item.nombre}' del pedido?",
            onDismiss = { itemParaBorrar = null },
            onConfirm = {
                viewModel.borrarDetallePedido(item)
                itemParaBorrar = null
            }
        )
    }
}

@Composable
fun DialogoProductoPedido(
    itemInicial: DetallePedido?,
    nombreSugerido: String,
    onDismiss: () -> Unit,
    onConfirm: (DetallePedido) -> Unit
) {
    var nombre by remember { mutableStateOf(itemInicial?.nombre ?: nombreSugerido) }
    var cantidad by remember { mutableStateOf((itemInicial?.cantidadPedida ?: 1).toString()) }
    var precio by remember { mutableStateOf((itemInicial?.precioUnitario ?: 0.0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (itemInicial != null) "Editar Producto" else "Añadir Producto") },
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
            Button(onClick = {
                val detalleFinal = DetallePedido(
                    detalleId = itemInicial?.detalleId ?: 0,
                    nombre = nombre,
                    cantidadPedida = cantidad.toIntOrNull() ?: 1,
                    precioUnitario = precio.toDoubleOrNull() ?: 0.0
                )
                onConfirm(detalleFinal)
            }) {
                Text(if (itemInicial != null) "Guardar Cambios" else "Agregar")
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
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
