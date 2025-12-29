package com.thiago.apk_mobile.ui.recibos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thiago.apk_mobile.data.model.Recibo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecibosScreen(
    viewModel: RecibosViewModel = hiltViewModel(),
    onNavigateToCrearRecibo: () -> Unit,
    onReciboClick: (Long) -> Unit
) {
    val recibos by viewModel.recibos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Recibos de Reparación") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCrearRecibo) {
                Icon(Icons.Default.Add, contentDescription = "Crear Recibo")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    label = { Text("Buscar por cliente...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showDateRangePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Filtrar por fecha")
                }
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(recibos) { recibo ->
                    ReciboItem(recibo = recibo, onClick = { onReciboClick(recibo.id) })
                }
            }
        }

        if (showDateRangePicker) {
            val datePickerState = rememberDateRangePickerState()
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(onClick = { 
                        showDateRangePicker = false
                        val start = datePickerState.selectedStartDateMillis ?: 0L
                        val end = datePickerState.selectedEndDateMillis ?: Long.MAX_VALUE
                        viewModel.onDateRangeChange(start, end)
                    }) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DateRangePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun ReciboItem(
    recibo: Recibo,
    onClick: () -> Unit
) {
    val cardColor = when (recibo.estado) {
        "SIN_ARREGLAR" -> Color(0xFFFADBD8) // Rojo suave
        "ARREGLADO" -> Color(0xFFFEF9E7) // Amarillo suave
        "ENTREGADO" -> Color(0xFFE8F8F5) // Verde suave
        else -> CardDefaults.cardColors().containerColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text("${recibo.referenciaCelular} - ${recibo.procedimiento}") },
            supportingContent = {
                Text("Cliente: ${recibo.nombreCliente}\nEstado: ${recibo.estado}")
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                    recibo.fechaDeEntregaReal?.let { fechaReal ->
                        Text("Entregado:", style = MaterialTheme.typography.bodySmall)
                        Text(sdf.format(Date(fechaReal)), style = MaterialTheme.typography.bodySmall)
                    } ?: run {
                        Text("Entrega est.:", style = MaterialTheme.typography.bodySmall)
                        Text(sdf.format(Date(recibo.fechaEntregaEstimada)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        )
    }
}
