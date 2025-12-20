// ARCHIVO: com/thiago/apk_mobile/data/InventarioRepository.kt (CORREGIDO)

package com.thiago.apk_mobile.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class InventarioRepository(
    private val productoDao: ProductoDao,
    private val movimientoDao: MovimientoDao
) {

    // --- PROPIEDADES DE MÉTRICAS ---
    val stockTotal: Flow<Int> = productoDao.obtenerStockTotal()
    val valorTotal: Flow<Double?> = productoDao.obtenerValorTotal() // El valor puede ser nulo

    fun obtenerProductosPaginados(query: String): Flow<PagingData<Producto>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { productoDao.obtenerTodosProductos(query) }
        ).flow
    }

    // --- FUNCIONES DE PRODUCTO ---

    suspend fun insertarProducto(producto: Producto): Long {
        return productoDao.insertar(producto)
    }

    suspend fun actualizarProducto(producto: Producto) {
        productoDao.actualizar(producto)
    }

    suspend fun eliminarProducto(producto: Producto) {
        productoDao.eliminar(producto)
    }

    // CORREGIDO: El producto puede no existir, devolvemos nullable
    suspend fun obtenerProductoPorId(id: Int): Producto? {
        return productoDao.obtenerProductoPorId(id)
    }

    // CORREGIDO: El Flow también puede emitir null o un producto inexistente
    fun obtenerProductoPorIdAsFlow(id: Int): Flow<Producto?> {
        return productoDao.obtenerProductoPorIdAsFlow(id)
    }

    // --- FUNCIONES DE MOVIMIENTO (LÓGICA DE NEGOCIO) ---

    suspend fun registrarMovimiento(movimiento: Movimiento) {
        movimientoDao.insertar(movimiento)
        val producto = productoDao.obtenerProductoPorId(movimiento.productoId)

        // CORREGIDO: Operar solo si el producto no es nulo
        producto?.let { p ->
            val nuevoStock = if (movimiento.tipo == "ENTRADA") {
                p.cantidadEnStock + movimiento.cantidadAfectada
            } else {
                (p.cantidadEnStock - movimiento.cantidadAfectada).coerceAtLeast(0)
            }
            productoDao.actualizar(p.copy(cantidadEnStock = nuevoStock))
        }
    }

    /** FUNCIÓN MODIFICADA PARA ACEPTAR FILTROS **/
    fun obtenerMovimientos(
        productoId: Int,
        tipo: String?,
        fechaInicio: Long,
        fechaFin: Long
    ): Flow<List<Movimiento>> {
        return movimientoDao.obtenerMovimientosDeProductoFiltrados(
            productoId,
            tipo,
            fechaInicio,
            fechaFin
        )
    }
}