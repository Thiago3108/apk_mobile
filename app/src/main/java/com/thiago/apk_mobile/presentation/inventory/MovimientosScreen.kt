package com.thiago.apk_mobile.presentation.inventory

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    // Escuchamos los cambios en los filtros y la lista de movimientos
    val filterState by inventarioViewModel.movimientoFilterState.collectAsStateWithLifecycle()
    val movimientosFlow = remember(productoId) { inventarioViewModel.obtenerMovimientosPorId(productoId) }
    val listaMovimientos by movimientosFlow.collectAsState(initial = emptyList())

    val stockActualFlow = remember(productoId) { inventarioViewModel.obtenerStockActualAsStateFlow(productoId) }
    val stockActual by stockActualFlow.collectAsState()

    var mostrarDialogoMovimiento by remember { mutableStateOf(false) }
    var mostrarPickerFecha by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(productoNombre, style = MaterialTheme.typography.titleMedium)
                        Text("Historial de Movimientos", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarPickerFecha = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Filtrar por fecha")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoMovimiento = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar Movimiento")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // === SECCIÓN DE FILTROS (UI) ===
            FiltrosTipoRow(
                tipoSeleccionado = filterState.tipo,
                onTipoSelected = { inventarioViewModel.setTipoMovimientoFilter(it) }
            )

            // Indicador de rango de fecha activo
            val df = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            Text(
                text = "Mostrando: ${df.format(Date(filterState.fechaInicio))} - ${df.format(Date(filterState.fechaFin))}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider()

            // === LISTA DE MOVIMIENTOS ===
            if (listaMovimientos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay movimientos con los filtros seleccionados.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(listaMovimientos, key = { it.movimientoId }) { movimiento ->
                        MovimientoCard(movimiento = movimiento)
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                    }
                }
            }
        }
    }

    // DIÁLOGOS
    if (mostrarPickerFecha) {
        RangoFechasDialog(
            onDismiss = { mostrarPickerFecha = false },
            onDateRangeSelected = { inicio, fin ->
                inventarioViewModel.setRangoFechasFilter(inicio, fin)
                mostrarPickerFecha = false
            }
        )
    }

    if (mostrarDialogoMovimiento) {
        DialogoRegistrarMovimiento(
            stockActual = stockActual,
            onDismiss = { mostrarDialogoMovimiento = false },
            onConfirm = { tipo, cantidad, razon ->
                val nuevoMovimiento = Movimiento(
                    productoId = productoId,
                    tipo = tipo,
                    cantidadAfectada = cantidad,
                    razon = razon
                )
                inventarioViewModel.registrarMovimientoStock(nuevoMovimiento)
                mostrarDialogoMovimiento = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosTipoRow(
    tipoSeleccionado: String?,
    onTipoSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = tipoSeleccionado == null,
            onClick = { onTipoSelected(null) },
            label = { Text("Todos") }
        )
        FilterChip(
            selected = tipoSeleccionado == "ENTRADA",
            onClick = { onTipoSelected("ENTRADA") },
            label = { Text("Entradas") },
            leadingIcon = { if (tipoSeleccionado == "ENTRADA") Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
        )
        FilterChip(
            selected = tipoSeleccionado == "SALIDA",
            onClick = { onTipoSelected("SALIDA") },
            label = { Text("Salidas") },
            leadingIcon = { if (tipoSeleccionado == "SALIDA") Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangoFechasDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onDateRangeSelected(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedEndDateMillis != null
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.weight(1f).padding(16.dp),
            title = { Text("Selecciona el periodo") }
        )
    }
}

@Composable
fun MovimientoCard(movimiento: Movimiento) {
    val dateFormatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val fechaHora = dateFormatter.format(Date(movimiento.fecha))
    val esEntrada = movimiento.tipo == "ENTRADA"
    val color = if (esEntrada) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val icon = if (esEntrada) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = movimiento.razon, style = MaterialTheme.typography.bodyLarge)
            Text(text = fechaHora, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(
            text = "${if (esEntrada) "+" else "-"}${movimiento.cantidadAfectada}",
            color = color,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
