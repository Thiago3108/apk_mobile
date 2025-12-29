package com.thiago.apk_mobile.ui.recibos

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.thiago.apk_mobile.R
import com.thiago.apk_mobile.data.model.Recibo
import com.thiago.apk_mobile.util.BluetoothPrinterHelper
import com.thiago.apk_mobile.util.PrintingHelper
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecibosScreen(
    viewModel: RecibosViewModel = hiltViewModel(),
    onNavigateToCrearRecibo: () -> Unit,
    onReciboClick: (Long) -> Unit,
    onNavigateToEditRecibo: (Long) -> Unit
) {
    val recibos by viewModel.recibos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showDateRangePicker by remember { mutableStateOf(false) }
    var reciboAEliminar by remember { mutableStateOf<Recibo?>(null) }

    // --- Lógica de Impresión ---
    var showPrintDialog by remember { mutableStateOf(false) }
    var reciboAImprimir by remember { mutableStateOf<Recibo?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val printerHelper = remember { BluetoothPrinterHelper(context) }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

    var hasBluetoothPermissions by remember {
        mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allGranted = permissions.values.all { it }
            hasBluetoothPermissions = allGranted
            if (allGranted && reciboAImprimir != null) {
                showPrintDialog = true
            }
        }
    )

    if (showPrintDialog && reciboAImprimir != null) {
        val pairedDevices = remember(hasBluetoothPermissions) { 
            if(hasBluetoothPermissions) printerHelper.getPairedDevices() else emptyList()
        }
        DeviceSelectionDialog(
            devices = pairedDevices,
            onDeviceSelected = { device ->
                val reciboToPrintNow = reciboAImprimir
                
                showPrintDialog = false
                reciboAImprimir = null

                if (reciboToPrintNow != null) {
                    scope.launch {
                        val inputStream = context.resources.openRawResource(R.raw.recibo_template)
                        val template = BufferedReader(InputStreamReader(inputStream)).readText()
                        val printableText = PrintingHelper.generatePrintableRecibo(template, reciboToPrintNow)
                        val result = printerHelper.printText(device.address, printableText)
                        result.onSuccess {
                            snackbarHostState.showSnackbar("Impresión enviada correctamente.")
                        }.onFailure {
                            snackbarHostState.showSnackbar("Error al imprimir: ${it.message}")
                        }
                    }
                }
            },
            onDismiss = { 
                showPrintDialog = false
                reciboAImprimir = null 
            }
        )
    }

    // --- Fin Lógica de Impresión ---

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    ReciboItem(
                        recibo = recibo,
                        onClick = { onReciboClick(recibo.id) },
                        onEdit = { onNavigateToEditRecibo(recibo.id) },
                        onDelete = { reciboAEliminar = recibo },
                        onPrint = {
                            reciboAImprimir = recibo
                            if (hasBluetoothPermissions) {
                                showPrintDialog = true
                            } else {
                                permissionLauncher.launch(permissionsToRequest)
                            }
                        }
                    )
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

        reciboAEliminar?.let { recibo ->
            AlertDialog(
                onDismissRequest = { reciboAEliminar = null },
                title = { Text("Confirmar Eliminación") },
                text = { Text("¿Estás seguro de que quieres eliminar el recibo de ${recibo.nombreCliente}?") },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.eliminarRecibo(recibo)
                        reciboAEliminar = null
                    }) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    Button(onClick = { reciboAEliminar = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceSelectionDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Impresora") },
        text = {
            if (devices.isEmpty()) {
                Text("No hay impresoras Bluetooth emparejadas. Por favor, empareja tu impresora desde los ajustes de Bluetooth de tu teléfono.")
            } else {
                LazyColumn {
                    items(devices) { device ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device) }
                            .padding(vertical = 8.dp)) {
                            Text(device.name ?: device.address ?: "Dispositivo desconocido")
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}


@Composable
fun ReciboItem(
    recibo: Recibo,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
) {
    val cardColor = when (recibo.estado) {
        "SIN_ARREGLAR" -> Color(0xFFFADBD8)
        "ARREGLADO" -> Color(0xFFFEF9E7)
        "ENTREGADO" -> Color(0xFFE8F8F5)
        else -> CardDefaults.cardColors().containerColor
    }
    var expanded by remember { mutableStateOf(false) }

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
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                            recibo.fechaDeEntregaReal?.let {
                                fechaReal ->
                                Text("Entregado:", style = MaterialTheme.typography.bodySmall)
                                Text(sdf.format(Date(fechaReal)), style = MaterialTheme.typography.bodySmall)
                            } ?: run {
                                Text("Entrega est.:", style = MaterialTheme.typography.bodySmall)
                                Text(sdf.format(Date(recibo.fechaEntregaEstimada)), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Imprimir") }, onClick = { 
                            onPrint()
                            expanded = false 
                        })
                        DropdownMenuItem(text = { Text("Editar") }, onClick = { 
                            onEdit()
                            expanded = false
                        })
                        DropdownMenuItem(text = { Text("Eliminar") }, onClick = { 
                            onDelete()
                            expanded = false
                        })
                    }
                }
            }
        )
    }
}