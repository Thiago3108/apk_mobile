package com.thiago.apk_mobile.presentation.facturas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thiago.apk_mobile.data.Producto
import com.thiago.apk_mobile.presentation.InventarioViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturaFormScreen(
    facturaId: Long?,
    inventarioViewModel: InventarioViewModel,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var nombreCliente by remember { mutableStateOf("") }
    var cedulaCliente by remember { mutableStateOf("") }
    val articulosFactura = remember { mutableStateListOf<ArticuloFactura>() }
    val isEditMode = facturaId != null
    var dataLoaded by remember { mutableStateOf(!isEditMode) }

    // Cargar los datos si estamos en modo edición
    LaunchedEffect(facturaId) {
        if (isEditMode && facturaId != null) {
            combine(
                inventarioViewModel.getFacturaDisplayById(facturaId).filterNotNull(),
                inventarioViewModel.productos.filter { it.isNotEmpty() }
            ) { factura, productos ->
                factura to productos
            }.first().let { (facturaToEdit, productos) ->
                nombreCliente = facturaToEdit.factura.nombreCliente
                cedulaCliente = facturaToEdit.factura.cedulaCliente

                val articulosParaCargar = facturaToEdit.articulos.mapNotNull { articuloVendido ->
                    productos.find { it.nombre == articuloVendido.productoNombre }?.let { producto ->
                        ArticuloFactura(producto, articuloVendido.cantidad)
                    }
                }
                articulosFactura.clear()
                articulosFactura.addAll(articulosParaCargar)
                dataLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Editar Factura" else "Nueva Factura") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!dataLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = nombreCliente,
                    onValueChange = { nombreCliente = it },
                    label = { Text("Nombre del cliente") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cedulaCliente,
                    onValueChange = { cedulaCliente = it },
                    label = { Text("Cédula del cliente") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                ProductSuggester(
                    inventarioViewModel = inventarioViewModel,
                    articulosActuales = articulosFactura.toList()
                ) { producto, cantidad ->
                    val existing = articulosFactura.find { it.producto.productoId == producto.productoId }
                    if (existing != null) {
                        val updated = existing.copy(cantidad = existing.cantidad + cantidad)
                        articulosFactura[articulosFactura.indexOf(existing)] = updated
                    } else {
                        articulosFactura.add(ArticuloFactura(producto, cantidad))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Artículos a facturar", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(articulosFactura) { articulo ->
                       ArticuloItemRow(articulo = articulo, onRemove = { articulosFactura.remove(articulo) })
                    }
                }

                val totalFactura = articulosFactura.sumOf { it.producto.precioVenta * it.cantidad }
                Text("Total: $${String.format("%.2f", totalFactura)}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.End))
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (isEditMode && facturaId != null) {
                            inventarioViewModel.updateFactura(facturaId, nombreCliente, cedulaCliente, articulosFactura)
                        } else {
                            inventarioViewModel.generarFactura(nombreCliente, cedulaCliente, articulosFactura)
                        }
                        onSave()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = nombreCliente.isNotBlank() && cedulaCliente.isNotBlank() && articulosFactura.isNotEmpty()
                ) {
                    Text("Guardar Factura")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSuggester(
    inventarioViewModel: InventarioViewModel,
    articulosActuales: List<ArticuloFactura>,
    onAdd: (Producto, Int) -> Unit
) {
    val searchQuery by inventarioViewModel.searchQuery.collectAsState()
    val sugerencias by inventarioViewModel.sugerenciasBusqueda.collectAsState()
    var selectedProduct by remember { mutableStateOf<Producto?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val productoSeleccionado = selectedProduct
    val cantidadEnFactura = articulosActuales.find { it.producto.productoId == productoSeleccionado?.productoId }?.cantidad ?: 0
    val stockDisponible = (productoSeleccionado?.cantidadEnStock ?: 0) - cantidadEnFactura
    val cantidadAAnadir = quantity.toIntOrNull() ?: 0
    val canAdd = productoSeleccionado != null && cantidadAAnadir > 0 && cantidadAAnadir <= stockDisponible

    Column {
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded && sugerencias.isNotEmpty() }
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { inventarioViewModel.onSearchQueryChange(it) },
                label = { Text("Buscar producto") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) }
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                sugerencias.filter { it.cantidadEnStock > 0 }.forEach { producto ->
                    DropdownMenuItem(
                        text = { Text("${producto.nombre} (Venta: $${producto.precioVenta})") },
                        onClick = {
                            selectedProduct = producto
                            inventarioViewModel.onSearchQueryChange(producto.nombre)
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { char -> char.isDigit() } },
                label = { Text("Cant.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                isError = !canAdd && cantidadAAnadir > 0
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (canAdd) {
                        onAdd(productoSeleccionado!!, cantidadAAnadir)
                        selectedProduct = null
                        quantity = "1"
                        inventarioViewModel.onSearchQueryChange("")
                    }
                },
                enabled = canAdd
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
        if (!canAdd && productoSeleccionado != null && cantidadAAnadir > 0) {
            Text(
                text = "Stock insuficiente. Disponible: $stockDisponible",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun ArticuloItemRow(articulo: ArticuloFactura, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = articulo.producto.nombre, style = MaterialTheme.typography.bodyLarge)
            Text(text = "${articulo.cantidad} x $${String.format("%.2f", articulo.producto.precioVenta)}", style = MaterialTheme.typography.bodySmall)
        }
        Text(text = "$${String.format("%.2f", articulo.producto.precioVenta * articulo.cantidad)}", style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Quitar Artículo")
        }
    }
}
