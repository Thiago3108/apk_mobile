package com.thiago.apk_mobile.presentation.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thiago.apk_mobile.data.Movimiento
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    productoId: Int,
    productoNombre: String,
    onBackClick: () -> Unit,
    inventarioViewModel: InventarioViewModel = viewModel(factory = getInventarioViewModelFactory(LocalContext.current))
) {
    val movimientosFlow = inventarioViewModel.obtenerMovimientosPorId(productoId)
    val listaMovimientos by movimientosFlow.collectAsState(initial = emptyList())

    val stockActualFlow = remember(productoId) {
        inventarioViewModel.obtenerStockActualAsStateFlow(productoId)
    }
    val stockActual by stockActualFlow.collectAsState(initial = 0)

    var mostrarDialogo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial: $productoNombre") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar Movimiento")
            }
        }
    ) { paddingValues ->
        if (listaMovimientos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay movimientos. Usa el botón '+' para añadir.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 8.dp)
            ) {
                items(listaMovimientos, key = { it.movimientoId }) { movimiento ->
                    MovimientoCard(movimiento = movimiento)
                    Divider()
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoRegistrarMovimiento(
            stockActual = stockActual ?: 0,
            onDismiss = { mostrarDialogo = false },
            onConfirm = { tipo, cantidad, razon ->
                val nuevoMovimiento = Movimiento(
                    productoId = productoId,
                    tipo = tipo,
                    cantidadAfectada = cantidad,
                    razon = razon
                )
                inventarioViewModel.registrarMovimientoStock(nuevoMovimiento)
                mostrarDialogo = false
            }
        )
    }
}

@Composable
fun MovimientoCard(movimiento: Movimiento) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val fechaHora = dateFormatter.format(Date(movimiento.fecha))
    val color = if (movimiento.tipo == "ENTRADA") Color(0xFF4CAF50) else Color(0xFFF44336)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Tipo: ${movimiento.tipo}", color = color, style = MaterialTheme.typography.titleMedium)
            Text(text = "Razón: ${movimiento.razon}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Fecha: $fechaHora", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(text = "${movimiento.cantidadAfectada} unidades", color = color, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun DialogoRegistrarMovimiento(
    stockActual: Int,
    onDismiss: () -> Unit,
    onConfirm: (tipo: String, cantidad: Int, razon: String) -> Unit
) {
    var tipoMovimiento by remember { mutableStateOf("ENTRADA") }
    var cantidadInput by remember { mutableStateOf("0") }
    var razonInput by remember { mutableStateOf("") }

    val cantidadActual = cantidadInput.toIntOrNull() ?: 0
    val esSalida = tipoMovimiento == "SALIDA"
    val hayStockSuficiente = !esSalida || cantidadActual <= stockActual
    val puedeConfirmar = cantidadActual > 0 && hayStockSuficiente

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Movimiento (Stock: $stockActual)") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    FilterChip(selected = tipoMovimiento == "ENTRADA", onClick = { tipoMovimiento = "ENTRADA" }, label = { Text("Entrada") })
                    FilterChip(selected = tipoMovimiento == "SALIDA", onClick = { tipoMovimiento = "SALIDA" }, label = { Text("Salida") })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Cantidad:", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { if (cantidadActual > 0) cantidadInput = (cantidadActual - 1).toString() },
                        enabled = cantidadActual > 0,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrementar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = cantidadInput,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.filter { it.isDigit() }
                            cantidadInput = (digitsOnly.toIntOrNull() ?: 0).toString()
                        },
                        label = { Text("Unidades") },
                        isError = !hayStockSuficiente,
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { if (cantidadActual < Int.MAX_VALUE) cantidadInput = (cantidadActual + 1).toString() },
                        enabled = cantidadActual < Int.MAX_VALUE && (!esSalida || cantidadActual < stockActual),
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Incrementar")
                    }
                }

                if (!hayStockSuficiente) {
                    Text(
                        text = "Stock insuficiente",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = razonInput,
                    onValueChange = { razonInput = it },
                    label = { Text("Razón/Motivo (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tipoMovimiento, cantidadActual, razonInput.ifBlank { "Sin razón" }) },
                enabled = puedeConfirmar
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}