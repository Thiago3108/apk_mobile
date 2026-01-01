package com.thiago.apk_mobile.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.thiago.apk_mobile.Destinations
import com.thiago.apk_mobile.data.model.Pedido
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Panel de Control") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ResumenReparacionesCard(
                    pendientes = uiState.reparacionesPendientes,
                    listas = uiState.reparacionesListas,
                    navController = navController
                )
            }
            item {
                AccesosRapidosCard(navController)
            }
            item {
                ResumenDiaCard(
                    ingresos = uiState.ingresosHoy,
                    reparacionesTerminadas = uiState.reparacionesTerminadasHoy,
                    equiposEntregados = uiState.equiposEntregadosHoy
                )
            }
            item {
                Text("Últimos Pedidos", style = MaterialTheme.typography.titleLarge)
            }
            items(uiState.ultimosPedidos) {
                pedido ->
                PedidoCard(pedido)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenReparacionesCard(pendientes: Int, listas: Int, navController: NavController) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate("recibos_section?estado=SIN_ARREGLAR") }
            ) {
                Text(pendientes.toString(), style = MaterialTheme.typography.headlineMedium, color = Color(0xFFD32F2F))
                Text("Pendientes")
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate("recibos_section?estado=ARREGLADO") }
            ) {
                Text(listas.toString(), style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFBC02D))
                Text("Listos")
            }
        }
    }
}

@Composable
fun AccesosRapidosCard(navController: NavController) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Accesos Rápidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                QuickAccessButton(Icons.Default.Receipt, "Nuevo Recibo") { navController.navigate(Destinations.RECIBO_FORM_ROUTE) }
                QuickAccessButton(Icons.Default.Description, "Nueva Factura") { navController.navigate(Destinations.FACTURA_FORM_WITH_ID_ROUTE.replace("{facturaId}", "0")) }
                QuickAccessButton(Icons.Default.ListAlt, "Nuevo Pedido") { navController.navigate(Destinations.PEDIDO_FORM_ROUTE) }
            }
        }
    }
}

@Composable
fun QuickAccessButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun ResumenDiaCard(ingresos: Double, reparacionesTerminadas: Int, equiposEntregados: Int) {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    format.maximumFractionDigits = 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Resumen del Día", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ingresos de Hoy: ${format.format(ingresos)}")
            Text("Reparaciones terminadas hoy: $reparacionesTerminadas")
            Text("Equipos entregados hoy: $equiposEntregados")
        }
    }
}

@Composable
fun PedidoCard(pedido: Pedido) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Pedido a ${pedido.proveedor}", modifier = Modifier.weight(1f))
            Text(sdf.format(Date(pedido.fecha)))
        }
    }
}
