// Archivo: com/thiago/apk_mobile/presentation/inventory/InventarioScreen.kt (CORREGIDO)

package com.thiago.apk_mobile.presentation.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.thiago.apk_mobile.data.Movimiento
import com.thiago.apk_mobile.data.Producto
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.MetricsUiState
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    modifier: Modifier = Modifier,
    onProductoClick: (Int, String) -> Unit,
    inventarioViewModel: InventarioViewModel = viewModel(factory = getInventarioViewModelFactory(LocalContext.current))
) {
    val lazyProductos: LazyPagingItems<Producto> = inventarioViewModel.productosPaginados.collectAsLazyPagingItems()
    val searchQuery by inventarioViewModel.searchQuery.collectAsState()
    val metricsUiState by inventarioViewModel.metricsUiState.collectAsState()

    var mostrarDialogoAgregar by remember { mutableStateOf(false) }
    var mostrarDialogoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }
    var productoParaMovimiento by remember { mutableStateOf<Producto?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { InventarioTopAppBar() },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoAgregar = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Producto")
            }
        }
    ) { paddingValues ->
        InventarioBody(
            lazyProductos = lazyProductos,
            metrics = metricsUiState,
            searchQuery = searchQuery,
            onSearchQueryChange = inventarioViewModel::onSearchQueryChange,
            onProductoClick = onProductoClick,
            onEditClick = { producto -> mostrarDialogoEditar = producto },
            onDeleteClick = { producto -> productoAEliminar = producto },
            onRegisterMovementClick = { producto -> productoParaMovimiento = producto },
            modifier = modifier.padding(paddingValues)
        )
    }

    if (mostrarDialogoAgregar) {
        DialogoAgregarProducto(
            onDismiss = { mostrarDialogoAgregar = false },
            onConfirm = { nombre, precio, stock ->
                val nuevoProducto = Producto(
                    nombre = nombre, descripcion = "", precio = precio, cantidadEnStock = 0, ubicacion = ""
                )
                scope.launch {
                    val id = inventarioViewModel.insertarProducto(nuevoProducto)
                    if (stock > 0) {
                        inventarioViewModel.registrarMovimientoStock(
                            Movimiento(productoId = id.toInt(), tipo = "ENTRADA", cantidadAfectada = stock, razon = "Stock Inicial")
                        )
                    }
                    lazyProductos.refresh()
                }
                mostrarDialogoAgregar = false
            }
        )
    }

    mostrarDialogoEditar?.let { producto ->
        DialogoEditarProducto(
            producto = producto,
            onDismiss = { mostrarDialogoEditar = null },
            onConfirm = { productoEditado ->
                scope.launch {
                    inventarioViewModel.actualizarProducto(productoEditado)
                    lazyProductos.refresh()
                }
                mostrarDialogoEditar = null
            }
        )
    }

    productoAEliminar?.let { producto ->
        DialogoConfirmacion(
            titulo = "¿Eliminar ${producto.nombre}?",
            mensaje = "Esta acción eliminará el producto y todo su historial de movimientos. ¿Estás seguro?",
            onDismiss = { productoAEliminar = null },
            onConfirm = {
                scope.launch {
                    inventarioViewModel.eliminarProducto(producto)
                    lazyProductos.refresh()
                }
                productoAEliminar = null
            }
        )
    }

    productoParaMovimiento?.let { producto ->
        DialogoRegistrarMovimiento(
            stockActual = producto.cantidadEnStock,
            onDismiss = { productoParaMovimiento = null },
            onConfirm = { tipo, cantidad, razon ->
                val nuevoMovimiento = Movimiento(
                    productoId = producto.productoId,
                    tipo = tipo,
                    cantidadAfectada = cantidad,
                    razon = razon
                )
                inventarioViewModel.registrarMovimientoStock(nuevoMovimiento)
                lazyProductos.refresh()
                productoParaMovimiento = null
            }
        )
    }
}

@Composable
fun InventarioBody(
    lazyProductos: LazyPagingItems<Producto>,
    metrics: MetricsUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProductoClick: (Int, String) -> Unit,
    onEditClick: (Producto) -> Unit,
    onDeleteClick: (Producto) -> Unit,
    onRegisterMovementClick: (Producto) -> Unit,
    modifier: Modifier = Modifier
) {
    // LA SOLUCIÓN ESTÁ AQUÍ:
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 80.dp) // <-- AÑADIMOS PADDING INFERIOR
    ) {
        item(key = "search_field") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Buscar Producto...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
        }

        item(key = "metrics_card") {
            MetricasCard(metrics = metrics)
            Divider(modifier = Modifier.padding(bottom = 8.dp))
        }

        if (lazyProductos.itemCount == 0 && searchQuery.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay productos que coincidan con la búsqueda: \"$searchQuery\".")
                }
            }
        } else if (lazyProductos.itemCount == 0 && searchQuery.isBlank()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay productos. Añade uno con el botón '+'.")
                }
            }
        } else {
            items(
                count = lazyProductos.itemCount,
                key = lazyProductos.itemKey { it.productoId },
                contentType = lazyProductos.itemContentType { "producto" }
            ) { index ->
                val producto = lazyProductos[index]
                if (producto != null) {
                    ProductoCard(
                        producto = producto,
                        onClick = { onProductoClick(producto.productoId, producto.nombre) },
                        onEdit = { onEditClick(producto) },
                        onDelete = { onDeleteClick(producto) },
                        onRegisterMovement = { onRegisterMovementClick(producto) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun DialogoAgregarProducto(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, precio: Double, stock: Int) -> Unit
) {
    var nombreInput by remember { mutableStateOf("") }
    var precioInput by remember { mutableStateOf("") }
    var stockInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Nuevo Producto") },
        text = {
            Column {
                OutlinedTextField(value = nombreInput, onValueChange = { nombreInput = it }, label = { Text("Nombre") })
                OutlinedTextField(value = precioInput, onValueChange = { precioInput = it }, label = { Text("Precio") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = stockInput, onValueChange = { stockInput = it }, label = { Text("Stock Inicial") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                val precio = precioInput.toDoubleOrNull() ?: 0.0
                val stock = stockInput.toIntOrNull() ?: 0
                if (nombreInput.isNotBlank()) {
                    onConfirm(nombreInput, precio, stock)
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoEditarProducto(
    producto: Producto,
    onDismiss: () -> Unit,
    onConfirm: (Producto) -> Unit
) {
    var nombreInput by remember { mutableStateOf(producto.nombre) }
    var precioInput by remember { mutableStateOf(producto.precio.toString()) }
    var ubicacionInput by remember { mutableStateOf(producto.ubicacion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Producto") },
        text = {
            Column {
                OutlinedTextField(value = nombreInput, onValueChange = { nombreInput = it }, label = { Text("Nombre") })
                OutlinedTextField(value = precioInput, onValueChange = { precioInput = it }, label = { Text("Precio") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = ubicacionInput, onValueChange = { ubicacionInput = it }, label = { Text("Ubicación") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val precio = precioInput.toDoubleOrNull() ?: producto.precio
                val productoEditado = producto.copy(nombre = nombreInput, precio = precio, ubicacion = ubicacionInput)
                onConfirm(productoEditado)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
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
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ProductoCard(
    producto: Producto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegisterMovement: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Click en toda la fila para ver detalle/historial
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = producto.nombre, style = MaterialTheme.typography.titleMedium)
            Text(text = "ID: ${producto.productoId} - Precio: $${producto.precio}", style = MaterialTheme.typography.bodySmall)
        }

        Text(
            text = "Stock: ${producto.cantidadEnStock}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

                DropdownMenuItem(text = { Text("Editar Producto") }, onClick = {
                    onEdit()
                    expanded = false
                })
                DropdownMenuItem(text = { Text("Eliminar Producto") }, onClick = {
                    onDelete()
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioTopAppBar() {
    TopAppBar(title = { Text("Mi Inventario") })
}

@Composable
fun MetricasCard(metrics: MetricsUiState) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Resumen del Inventario",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Unidades Totales", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = metrics.stockTotal.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Valor Total Estimado", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = currencyFormat.format(metrics.valorTotal),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}
