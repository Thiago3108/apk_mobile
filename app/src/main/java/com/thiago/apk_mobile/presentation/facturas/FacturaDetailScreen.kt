package com.thiago.apk_mobile.presentation.facturas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thiago.apk_mobile.presentation.InventarioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturaDetailScreen(
    facturaId: Long,
    inventarioViewModel: InventarioViewModel,
    onBack: () -> Unit
) {
    val facturaState by inventarioViewModel.getFacturaDisplayById(facturaId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Factura") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        val factura = facturaState
        if (factura == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            FacturaDetailContent(factura = factura, modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
private fun FacturaDetailContent(factura: FacturaDisplay, modifier: Modifier = Modifier) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Cliente: ${factura.factura.nombreCliente}", style = MaterialTheme.typography.titleLarge)
        Text("Cédula: ${factura.factura.cedulaCliente}", style = MaterialTheme.typography.bodyMedium)
        Text("Fecha: ${dateFormatter.format(Date(factura.factura.fecha))}", style = MaterialTheme.typography.bodySmall)

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Artículos Comprados", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(factura.articulos) { articulo ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(articulo.productoNombre, style = MaterialTheme.typography.bodyLarge)
                        Text("Cant: ${articulo.cantidad} x $${String.format("%.2f", articulo.precioUnitario)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("$${String.format("%.2f", articulo.total)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Total: $${String.format("%.2f", factura.factura.total)}",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.End)
        )
    }
}
