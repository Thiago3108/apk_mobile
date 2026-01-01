package com.thiago.apk_mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.apk_mobile.data.FacturaDao
import com.thiago.apk_mobile.data.PedidoDao
import com.thiago.apk_mobile.data.ProductoDao
import com.thiago.apk_mobile.data.ReciboDao

import com.thiago.apk_mobile.data.model.Pedido
import com.thiago.apk_mobile.data.model.Producto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val reparacionesPendientes: Int = 0,
    val reparacionesListas: Int = 0,
    val ingresosHoy: Double = 0.0,
    val ultimosPedidos: List<Pedido> = emptyList(),
    val reparacionesTerminadasHoy: Int = 0,
    val equiposEntregadosHoy: Int = 0,
    val productosSinStock: List<Producto> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reciboDao: ReciboDao,
    private val facturaDao: FacturaDao,
    private val pedidoDao: PedidoDao,
    private val productoDao: ProductoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sinArreglarFlow = reciboDao.getSinArreglarCount()
            val arregladoFlow = reciboDao.getArregladoCount()
            val facturasFlow = facturaDao.getFacturasForToday()
            val pedidosFlow = pedidoDao.getUltimosPedidos(3)
            val terminadasFlow = reciboDao.getReparacionesTerminadasHoy()
            val entregadosFlow = reciboDao.getEquiposEntregadosHoy()
            val productosSinStockFlow = productoDao.getProductosSinStock()

            val combinedFlow = combine(
                sinArreglarFlow, arregladoFlow, facturasFlow, pedidosFlow, terminadasFlow
            ) { sinArreglar, arreglado, facturas, pedidos, terminadas ->
                // Objeto temporal para los primeros 5 flujos
                object {
                    val reparacionesPendientes = sinArreglar
                    val reparacionesListas = arreglado
                    val ingresosHoy = facturas.sumOf { it.factura.total }
                    val ultimosPedidos = pedidos
                    val reparacionesTerminadasHoy = terminadas
                }
            }

            combinedFlow.combine(entregadosFlow) { fiveValues, entregados ->
                // Objeto temporal con 6 valores
                object {
                    val base = fiveValues
                    val equiposEntregadosHoy = entregados
                }
            }.combine(productosSinStockFlow) { sixValues, productosSinStock ->
                // Estado final de la UI
                HomeUiState(
                    reparacionesPendientes = sixValues.base.reparacionesPendientes,
                    reparacionesListas = sixValues.base.reparacionesListas,
                    ingresosHoy = sixValues.base.ingresosHoy,
                    ultimosPedidos = sixValues.base.ultimosPedidos,
                    reparacionesTerminadasHoy = sixValues.base.reparacionesTerminadasHoy,
                    equiposEntregadosHoy = sixValues.equiposEntregadosHoy,
                    productosSinStock = productosSinStock
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}