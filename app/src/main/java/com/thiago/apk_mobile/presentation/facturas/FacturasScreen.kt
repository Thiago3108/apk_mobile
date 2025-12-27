package com.thiago.apk_mobile.presentation.facturas

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.thiago.apk_mobile.R
import com.thiago.apk_mobile.data.FacturaConArticulos
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.util.BluetoothPrinterHelper
import com.thiago.apk_mobile.util.PrintingHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturasScreen(
    inventarioViewModel: InventarioViewModel,
    onNavigateToForm: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val facturas by inventarioViewModel.facturas.collectAsState()
    val filterState by inventarioViewModel.facturaFilterState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var facturaAEliminar by remember { mutableStateOf<FacturaConArticulos?>(null) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var facturaAImprimir by remember { mutableStateOf<FacturaConArticulos?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val printerHelper = remember { BluetoothPrinterHelper(context) }

    // --- Lógica para permisos de Bluetooth ---
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
            if (allGranted && facturaAImprimir != null) {
                showPrintDialog = true // Vuelve a intentar mostrar el diálogo si se concedió el permiso
            }
        }
    )

    // Diálogo de selección de impresora
    if (showPrintDialog && facturaAImprimir != null) {
        val pairedDevices = remember(hasBluetoothPermissions) { 
            if(hasBluetoothPermissions) printerHelper.getPairedDevices() else emptyList()
        }
        DeviceSelectionDialog(
            devices = pairedDevices,
            onDeviceSelected = { device ->
                val facturaToPrintNow = facturaAImprimir
                
                showPrintDialog = false
                facturaAImprimir = null

                if (facturaToPrintNow != null) {
                    scope.launch {
                        val inputStream = context.resources.openRawResource(R.raw.factura_template)
                        val template = BufferedReader(InputStreamReader(inputStream)).readText()
                        val facturaDisplay = inventarioViewModel.getFacturaDisplayById(facturaToPrintNow.factura.facturaId).first()

                        if (facturaDisplay != null) {
                            val printableText = PrintingHelper.generatePrintableFactura(template, facturaDisplay)
                            val result = printerHelper.printText(device.address, printableText)
                            result.onSuccess {
                                snackbarHostState.showSnackbar("Impresión enviada correctamente.")
                            }.onFailure {
                                snackbarHostState.showSnackbar("Error al imprimir: ${it.message}")
                            }
                        }
                    }
                }
            },
            onDismiss = { 
                showPrintDialog = false
                facturaAImprimir = null 
            }
        )
    }

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

    if (facturaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { facturaAEliminar = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar la factura de ${facturaAEliminar!!.factura.nombreCliente}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        inventarioViewModel.deleteFactura(facturaAEliminar!!.factura.facturaId)
                        facturaAEliminar = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { facturaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        facturaItem -> 
                        FacturaCard(
                            factura = facturaItem,
                            onClick = { onNavigateToDetail(facturaItem.factura.facturaId) },
                            onDeleteClick = { facturaAEliminar = facturaItem },
                            onEditClick = { onNavigateToEdit(facturaItem.factura.facturaId) },
                            onPrintClick = {
                                facturaAImprimir = facturaItem
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
                Text("No hay impresoras Bluetooth emparejadas. Por favor, empareja tu impresora MTP-4B desde los ajustes de Bluetooth de tu teléfono.")
            } else {
                LazyColumn {
                    items(devices) { device ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device) }
                            .padding(vertical = 8.dp)) {
                            // Es más seguro usar el alias si el nombre es nulo
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
fun FacturaCard(
    factura: FacturaConArticulos,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onPrintClick: () -> Unit
) {
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
                    DropdownMenuItem(text = { Text("Imprimir") }, onClick = {
                        onPrintClick()
                        expanded = false
                    })
                    DropdownMenuItem(text = { Text("Editar") }, onClick = {
                        onEditClick()
                        expanded = false
                    })
                    DropdownMenuItem(
                        text = { Text("Eliminar") }, 
                        onClick = { 
                            onDeleteClick()
                            expanded = false 
                        }
                    )
                }
            }
        }
    }
}
