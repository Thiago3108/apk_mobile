package com.thiago.apk_mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thiago.apk_mobile.data.FacturaDao
import com.thiago.apk_mobile.data.PedidoDao
import com.thiago.apk_mobile.data.ReciboDao
import com.thiago.apk_mobile.data.model.Pedido
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val reparacionesPendientes: Int = 0,
    val reparacionesListas: Int = 0,
    val ingresosHoy: Double = 0.0,
    val ultimosPedidos: List<Pedido> = emptyList(),
    val reparacionesTerminadasHoy: Int = 0,
    val equiposEntregadosHoy: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reciboDao: ReciboDao,
    private val facturaDao: FacturaDao,
    private val pedidoDao: PedidoDao
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

            combine(sinArreglarFlow, arregladoFlow, facturasFlow, pedidosFlow) { sinArreglar, arreglado, facturas, pedidos ->
                Triple(sinArreglar, arreglado, facturas.sumOf { it.factura.total } to pedidos)
            }.combine(combine(terminadasFlow, entregadosFlow) { terminadas, entregados ->
                terminadas to entregados
            }) { triple, (terminadas, entregados) ->
                val (sinArreglar, arreglado, pair) = triple
                val (ingresos, pedidos) = pair
                
                HomeUiState(
                    reparacionesPendientes = sinArreglar,
                    reparacionesListas = arreglado,
                    ingresosHoy = ingresos,
                    ultimosPedidos = pedidos,
                    reparacionesTerminadasHoy = terminadas,
                    equiposEntregadosHoy = entregados
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
