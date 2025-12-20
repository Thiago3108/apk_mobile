// Archivo: InventarioViewModelFactory.kt (CORREGIDO)

package com.thiago.apk_mobile.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thiago.apk_mobile.data.InventarioDatabase
import com.thiago.apk_mobile.data.InventarioRepository

/**
 * Factory para crear una instancia de InventarioViewModel con la dependencia del Repositorio.
 */
class InventarioViewModelFactory(private val repository: InventarioRepository) : ViewModelProvider.Factory {

    // Sobreescribe el método para crear el ViewModel
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventarioViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Singleton para el Factory
private lateinit var INSTANCE: InventarioViewModelFactory

fun getInventarioViewModelFactory(context: Context? = null): InventarioViewModelFactory {
    synchronized(InventarioViewModelFactory::class.java) {
        if (!::INSTANCE.isInitialized) {
            check(context != null) { "Contexto es requerido para inicializar el factory por primera vez" }
            val db = InventarioDatabase.getDatabase(context.applicationContext)
            
            // CORREGIDO: Se eliminan los DAOs que ya no existen (pedidoDao, detallePedidoDao)
            val repository = InventarioRepository(
                productoDao = db.productoDao(),
                movimientoDao = db.movimientoDao()
            )
            INSTANCE = InventarioViewModelFactory(repository)
        }
    }
    return INSTANCE
}