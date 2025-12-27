package com.thiago.apk_mobile.presentation.facturas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thiago.apk_mobile.data.FacturaConArticulos
import com.thiago.apk_mobile.presentation.InventarioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturasScreen(
    inventarioViewModel: InventarioViewModel,
    onNavigateToForm: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val facturas by inventarioViewModel.facturas.collectAsState()
    val filterState by inventarioViewModel.facturaFilterState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    val start = datePickerState.selectedStartDateMillis
                    val end = datePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        inventarioViewModel.setFacturaDateRangeFilter(start, end)
                    }
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DateRangePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Facturas") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToForm) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Factura")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // --- Filtros ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = filterState.query,
                    onValueChange = { inventarioViewModel.onFacturaSearchQueryChange(it) },
                    label = { Text("Buscar por cliente") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Filtrar por fecha")
                }
            }

            if (facturas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay facturas que coincidan con la búsqueda.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(facturas) {
                        facturaItem -> FacturaCard(factura = facturaItem, onClick = { onNavigateToDetail(facturaItem.factura.facturaId) })
                    }
                }
            }
        }
    }
}

@Composable
fun FacturaCard(factura: FacturaConArticulos, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = factura.factura.nombreCliente, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Fecha: ${dateFormatter.format(Date(factura.factura.fecha))}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Total: $${factura.factura.total}", style = MaterialTheme.typography.bodyMedium)
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Imprimir") }, onClick = { /* TODO */ ; expanded = false })
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { /* TODO */ ; expanded = false })
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { /* TODO */ ; expanded = false })
                }
            }
        }
    }
}
