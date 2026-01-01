package com.thiago.apk_mobile.ui.settings

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.thiago.apk_mobile.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val uiState by viewModel.uiState.collectAsState()

    var showImportDialog by remember { mutableStateOf<String?>(null) } 

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        viewModel.onGoogleSignInSuccess(account)
                    }
                } catch (e: ApiException) {
                    viewModel.onGoogleSignInFailed()
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SettingsViewModel.UiEvent.ShowSnackbar -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
                is SettingsViewModel.UiEvent.RestartApp -> {
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(context, 12345, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
                    exitProcess(0)
                }
                 is SettingsViewModel.UiEvent.LaunchGoogleSignIn -> {
                    googleSignInLauncher.launch(event.signInIntent)
                }
            }
        }
    }

    showImportDialog?.let { fileId ->
        AlertDialog(
            onDismissRequest = { showImportDialog = null },
            title = { Text("¡ADVERTENCIA!") },
            text = { Text("Vas a reemplazar todos los datos actuales con los de la copia de seguridad. Esta acción no se puede deshacer. La aplicación se reiniciará.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.importDatabase(fileId)
                    showImportDialog = null
                }) {
                    Text("Importar")
                }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = null }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Ajustes y Copias de Seguridad") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val user = uiState.googleUser
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (user == null) {
                Button(onClick = { viewModel.signIn() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Iniciar sesión con Google Drive")
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Conectado como:", style = MaterialTheme.typography.bodySmall)
                            Text(user.email ?: "", fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { viewModel.signOut() }) {
                            Text("Cerrar sesión")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.exportDatabase() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear Nueva Copia de Seguridad")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Copias de Seguridad Disponibles", style = MaterialTheme.typography.titleMedium)
                    LazyColumn {
                        items(uiState.driveFiles) { file ->
                            val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                            Card(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { file.id?.let { showImportDialog = it } }) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Column {
                                        Text(file.name ?: "Nombre desconocido", fontWeight = FontWeight.Bold)
                                        val dateString = file.createdTime?.value?.let { sdf.format(Date(it)) } ?: "Fecha desconocida"
                                        Text(dateString, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
