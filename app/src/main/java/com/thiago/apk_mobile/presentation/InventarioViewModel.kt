package com.thiago.apk_mobile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.thiago.apk_mobile.data.*
import com.thiago.apk_mobile.presentation.facturas.ArticuloFactura
import com.thiago.apk_mobile.presentation.facturas.ArticuloVendidoDisplay
import com.thiago.apk_mobile.presentation.facturas.FacturaDisplay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MovimientoFilterState(
    val tipo: String? = null,
    val fechaInicio: Long = System.currentTimeMillis() - 315360000000L, // Approx 10 years ago
    val fechaFin: Long = System.currentTimeMillis() + 86400000L // Approx 1 day from now
)

data class FacturaFilterState(
    val query: String = "",
    val fechaInicio: Long = System.currentTimeMillis() - 315360000000L, // Approx 10 years ago
    val fechaFin: Long = System.currentTimeMillis() + 86400000L // Approx 1 day from now
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

    private val _facturaFilterState = MutableStateFlow(FacturaFilterState())
    val facturaFilterState: StateFlow<FacturaFilterState> = _facturaFilterState

    val detallesPedido: StateFlow<List<DetallePedido>> = repository.obtenerDetallesPedido()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productos: StateFlow<List<Producto>> = repository.getProductos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val facturas: StateFlow<List<FacturaConArticulos>> = _facturaFilterState
        .flatMapLatest { filter ->
            repository.getFacturas(filter.query, filter.fechaInicio, filter.fechaFin)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    @OptIn(ExperimentalCoroutinesApi::class)
    val productosPaginados: Flow<PagingData<Producto>> = _searchQuery
        .flatMapLatest { query ->
            repository.obtenerProductosPaginados(query)
        }
        .cachedIn(viewModelScope)

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sugerenciasBusqueda: StateFlow<List<Producto>> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.length < 2) flowOf(emptyList())
            else repository.obtenerSugerencias(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun getFacturaDisplayById(facturaId: Long): Flow<FacturaDisplay?> = flow {
        repository.getFacturaConArticulosById(facturaId).collect { facturaConArticulos ->
            if (facturaConArticulos == null) {
                emit(null)
                return@collect
            }

            val articulosDisplay = facturaConArticulos.articulos.mapNotNull { articulo ->
                val producto = repository.obtenerProductoPorId(articulo.productoId)
                if (producto != null) {
                    ArticuloVendidoDisplay(
                        productoNombre = producto.nombre,
                        cantidad = articulo.cantidad,
                        precioUnitario = articulo.precioUnitario,
                        total = articulo.cantidad * articulo.precioUnitario
                    )
                } else {
                    null // O manejar el caso de un producto no encontrado
                }
            }
            emit(FacturaDisplay(facturaConArticulos.factura, articulosDisplay))
        }
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

    fun deleteFactura(facturaId: Long) {
        viewModelScope.launch {
            repository.deleteFacturaById(facturaId)
        }
    }

    fun registrarMovimientoStock(movimiento: Movimiento) {
        viewModelScope.launch {
            repository.registrarMovimiento(movimiento)
        }
    }

    fun generarFactura(nombreCliente: String, cedulaCliente: String, articulos: List<ArticuloFactura>) {
        viewModelScope.launch {
            repository.crearFactura(nombreCliente, cedulaCliente, articulos)
        }
    }

    fun onFacturaSearchQueryChange(newQuery: String) {
        _facturaFilterState.update { it.copy(query = newQuery) }
    }

    fun setFacturaDateRangeFilter(inicio: Long, fin: Long) {
        _facturaFilterState.update {
            it.copy(
                fechaInicio = inicio,
                fechaFin = fin + 86399000L // Include the whole end day
            )
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

    // --- Funciones para manejar los pedidos desde la BD ---
    fun agregarDetallePedido(detalle: DetallePedido) {
        viewModelScope.launch {
            repository.insertarDetallePedido(detalle)
        }
    }

    fun borrarDetallePedido(detalle: DetallePedido) {
        viewModelScope.launch {
            repository.borrarDetallePedido(detalle)
        }
    }

    fun recibirPedido(detalles: List<DetallePedido>) {
        viewModelScope.launch {
            repository.procesarRecepcionPedido(detalles)
        }
    }

    fun limpiarPedidos() {
        viewModelScope.launch {
            repository.limpiarTodosLosPedidos()
        }
    }
}
