package com.thiago.apk_mobile.ui.recibos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thiago.apk_mobile.data.model.Recibo
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReciboScreen(
    viewModel: RecibosViewModel = hiltViewModel(),
    reciboId: Long?,
    onReciboGuardado: () -> Unit
) {
    val isEditing = reciboId != null
    val reciboToEdit by viewModel.selectedRecibo.collectAsState()

    var nombreCliente by remember { mutableStateOf("") }
    var telefonoCliente by remember { mutableStateOf("") }
    var cedulaCliente by remember { mutableStateOf("") }
    var referenciaCelular by remember { mutableStateOf("") }
    var procedimiento by remember { mutableStateOf("") }
    var claveDispositivo by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var abono by remember { mutableStateOf("") }
    var diasEntrega by remember { mutableStateOf("") }
    var horasEntrega by remember { mutableStateOf("") }

    LaunchedEffect(reciboId) {
        if (isEditing) {
            viewModel.getReciboById(reciboId!!)
        }
    }

    LaunchedEffect(reciboToEdit) {
        if (isEditing && reciboToEdit != null) {
            nombreCliente = reciboToEdit!!.nombreCliente
            telefonoCliente = reciboToEdit!!.telefonoCliente
            cedulaCliente = reciboToEdit!!.cedulaCliente
            referenciaCelular = reciboToEdit!!.referenciaCelular
            procedimiento = reciboToEdit!!.procedimiento
            claveDispositivo = reciboToEdit!!.claveDispositivo ?: ""
            precio = reciboToEdit!!.precio.toString()
            abono = reciboToEdit!!.abono.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isEditing) "Editar Recibo" else "Crear Nuevo Recibo") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nombreCliente,
                onValueChange = { nombreCliente = it },
                label = { Text("Nombre del Cliente") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = telefonoCliente,
                onValueChange = { telefonoCliente = it },
                label = { Text("Teléfono del Cliente") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cedulaCliente,
                onValueChange = { cedulaCliente = it },
                label = { Text("Cédula del Cliente") },
                 keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = referenciaCelular,
                onValueChange = { referenciaCelular = it },
                label = { Text("Referencia del Celular") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = procedimiento,
                onValueChange = { procedimiento = it },
                label = { Text("Procedimiento") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = claveDispositivo,
                onValueChange = { claveDispositivo = it },
                label = { Text("Clave/Patrón (Opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = precio,
                onValueChange = { precio = it },
                label = { Text("Precio del Arreglo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = abono,
                onValueChange = { abono = it },
                label = { Text("Abono") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            if (!isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     OutlinedTextField(
                        value = diasEntrega,
                        onValueChange = { diasEntrega = it },
                        label = { Text("Días") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                     OutlinedTextField(
                        value = horasEntrega,
                        onValueChange = { horasEntrega = it },
                        label = { Text("Horas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }


            Button(
                onClick = {
                    val precioDouble = precio.toDoubleOrNull() ?: 0.0
                    val abonoDouble = abono.toDoubleOrNull() ?: 0.0

                    if (isEditing && reciboToEdit != null) {
                         val updatedRecibo = reciboToEdit!!.copy(
                            nombreCliente = nombreCliente,
                            telefonoCliente = telefonoCliente,
                            cedulaCliente = cedulaCliente,
                            referenciaCelular = referenciaCelular,
                            procedimiento = procedimiento,
                            claveDispositivo = claveDispositivo.ifEmpty { null },
                            precio = precioDouble,
                            abono = abonoDouble
                        )
                        viewModel.actualizarRecibo(updatedRecibo)
                    } else {
                        val dias = diasEntrega.toLongOrNull() ?: 0
                        val horas = horasEntrega.toLongOrNull() ?: 0
                        val fechaRegistro = System.currentTimeMillis()
                        val entregaMillis = TimeUnit.DAYS.toMillis(dias) + TimeUnit.HOURS.toMillis(horas)
                        val fechaEntregaEstimada = fechaRegistro + entregaMillis

                        val nuevoRecibo = Recibo(
                            nombreCliente = nombreCliente,
                            telefonoCliente = telefonoCliente,
                            cedulaCliente = cedulaCliente,
                            referenciaCelular = referenciaCelular,
                            procedimiento = procedimiento,
                            claveDispositivo = claveDispositivo.ifEmpty { null },
                            precio = precioDouble,
                            abono = abonoDouble,
                            fechaRegistro = fechaRegistro,
                            fechaEntregaEstimada = fechaEntregaEstimada
                        )
                        viewModel.insertarRecibo(nuevoRecibo)
                    }
                    onReciboGuardado()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if(isEditing) "Actualizar Recibo" else "Guardar Recibo")
            }
        }
    }
}
