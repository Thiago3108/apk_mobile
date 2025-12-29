package com.thiago.apk_mobile.ui.recibos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thiago.apk_mobile.data.model.Recibo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciboDetailScreen(
    reciboId: Long,
    viewModel: RecibosViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    LaunchedEffect(reciboId) {
        viewModel.getReciboById(reciboId)
    }

    val recibo by viewModel.selectedRecibo.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle del Recibo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(16.dp)
        ) {
            if (recibo == null) {
                CircularProgressIndicator()
            } else {
                recibo?.let { 
                    CountdownTimer(it.fechaEntregaEstimada)
                    Spacer(modifier = Modifier.height(16.dp))
                    ReciboInfoCard(it)
                    Spacer(modifier = Modifier.height(16.dp))
                    EstadoActions(it, viewModel)
                }
            }
        }
    }
}

@Composable
fun CountdownTimer(targetTime: Long) {
    var remainingTime by remember { mutableStateOf(targetTime - System.currentTimeMillis()) }

    LaunchedEffect(targetTime) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime = targetTime - System.currentTimeMillis()
        }
    }

    val days = TimeUnit.MILLISECONDS.toDays(remainingTime)
    val hours = TimeUnit.MILLISECONDS.toHours(remainingTime) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60

    val timeText = if (remainingTime > 0) {
        String.format("%02d Días, %02d:%02d:%02d", days, hours, minutes, seconds)
    } else {
        "Tiempo de entrega vencido"
    }

    val color = if (remainingTime > 0) MaterialTheme.colorScheme.primary else Color.Red

    Text(
        text = timeText,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ReciboInfoCard(recibo: Recibo) {
    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow("Estado:", recibo.estado)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("Cliente:", recibo.nombreCliente)
            InfoRow("Teléfono:", recibo.telefonoCliente)
            InfoRow("Cédula:", recibo.cedulaCliente)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("Equipo:", recibo.referenciaCelular)
            InfoRow("Procedimiento:", recibo.procedimiento)
            recibo.claveDispositivo?.let { InfoRow("Clave:", it) }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("Precio:", "$${recibo.precio}")
            InfoRow("Abono:", "$${recibo.abono}")
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            InfoRow("Registrado:", sdf.format(Date(recibo.fechaRegistro)))
             recibo.fechaDeEntregaReal?.let {
                InfoRow("Entregado (Garantía):", sdf.format(Date(it)))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f))
        Text(text = value, modifier = Modifier.weight(0.6f))
    }
}

@Composable
fun EstadoActions(recibo: Recibo, viewModel: RecibosViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        when (recibo.estado) {
            "SIN_ARREGLAR" -> {
                Button(onClick = { 
                    viewModel.actualizarRecibo(recibo.copy(estado = "ARREGLADO"))
                }) {
                    Text("Marcar como Arreglado")
                }
            }
            "ARREGLADO" -> {
                Button(onClick = { 
                     val updatedRecibo = recibo.copy(
                        estado = "ENTREGADO",
                        fechaDeEntregaReal = System.currentTimeMillis()
                    )
                    viewModel.actualizarRecibo(updatedRecibo)
                }) {
                    Text("Marcar como Entregado")
                }
            }
            "ENTREGADO" -> {
                 Text("Este equipo ya fue entregado.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
