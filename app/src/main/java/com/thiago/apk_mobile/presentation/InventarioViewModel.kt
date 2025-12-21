// ARCHIVO: com/thiago/apk_mobile/presentation/InventarioViewModel.kt (CORREGIDO)

package com.thiago.apk_mobile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.thiago.apk_mobile.data.InventarioRepository
import com.thiago.apk_mobile.data.Movimiento
import com.thiago.apk_mobile.data.Producto
import com.thiago.apk_mobile.data.*
import com.thiago.apk_mobile.data.DetallePedido
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MovimientoFilterState(
    val tipo: String? = null,
    val fechaInicio: Long = System.currentTimeMillis() - 3153600000000L,
    val fechaFin: Long = System.currentTimeMillis() + 86400000L
)

data class MetricsUiState(
    val stockTotal: Int = 0,
    val valorTotal: Double = 0.0
)

class InventarioViewModel(private val repository: InventarioRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _movimientoFilterState = MutableStateFlow(MovimientoFilterState())
    val movimientoFilterState: StateFlow<MovimientoFilterState> = _movimientoFilterState

    val productosPaginados: Flow<PagingData<Producto>> = _searchQuery
        .flatMapLatest { query ->
            repository.obtenerProductosPaginados(query)
        }
        .cachedIn(viewModelScope)

    val metricsUiState: StateFlow<MetricsUiState> =
        repository.stockTotal.combine(repository.valorTotal) { stock, valor ->
            MetricsUiState(
                stockTotal = stock,
                valorTotal = valor ?: 0.0
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MetricsUiState()
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    suspend fun insertarProducto(producto: Producto): Long {
        return repository.insertarProducto(producto)
    }

    suspend fun actualizarProducto(producto: Producto) {
        repository.actualizarProducto(producto)
    }

    suspend fun eliminarProducto(producto: Producto) {
        repository.eliminarProducto(producto)
    }

    fun registrarMovimientoStock(movimiento: Movimiento) {
        viewModelScope.launch {
            repository.registrarMovimiento(movimiento)
        }
    }

    fun obtenerMovimientosPorId(productoId: Int): Flow<List<Movimiento>> {
        return _movimientoFilterState
            .flatMapLatest { filtros ->
                repository.obtenerMovimientos(
                    productoId = productoId,
                    tipo = filtros.tipo,
                    fechaInicio = filtros.fechaInicio,
                    fechaFin = filtros.fechaFin
                )
            }
    }

    fun obtenerStockActualAsStateFlow(productoId: Int): StateFlow<Int> {
        return repository.obtenerProductoPorIdAsFlow(productoId)
            // CORREGIDO: Si el producto es nulo, el stock es 0
            .map { it?.cantidadEnStock ?: 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )
    }

    fun setTipoMovimientoFilter(tipo: String?) {
        _movimientoFilterState.update { it.copy(tipo = tipo) }
    }

    fun setRangoFechasFilter(inicio: Long, fin: Long) {
        _movimientoFilterState.update {
            it.copy(
                fechaInicio = inicio,
                fechaFin = fin + 86399000L
            )
        }
    }

    fun recibirPedido(detalles: List<DetallePedido>) {
        viewModelScope.launch {
            repository.procesarRecepcionPedido(detalles)
        }
    }
}