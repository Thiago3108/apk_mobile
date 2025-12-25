package com.thiago.apk_mobile.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class InventarioRepository(
    private val productoDao: ProductoDao,
    private val movimientoDao: MovimientoDao,
    private val detallePedidoDao: DetallePedidoDao
) {
    val stockTotal: Flow<Int> = productoDao.obtenerStockTotal()
    val valorTotal: Flow<Double?> = productoDao.obtenerValorTotal()

    fun obtenerSugerencias(query: String): Flow<List<Producto>> {
        return productoDao.buscarSugerencias(query)
    }

    fun obtenerProductosPaginados(query: String): Flow<PagingData<Producto>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { productoDao.obtenerTodosProductos(query) }
        ).flow
    }

    suspend fun insertarProducto(producto: Producto): Long = productoDao.insertar(producto)
    suspend fun actualizarProducto(producto: Producto) = productoDao.actualizar(producto)
    suspend fun eliminarProducto(producto: Producto) = productoDao.eliminar(producto)
    suspend fun obtenerProductoPorId(id: Int): Producto? = productoDao.obtenerProductoPorId(id)
    fun obtenerProductoPorIdAsFlow(id: Int): Flow<Producto?> = productoDao.obtenerProductoPorIdAsFlow(id)

    suspend fun registrarMovimiento(movimiento: Movimiento) {
        movimientoDao.insertar(movimiento)
        val producto = productoDao.obtenerProductoPorId(movimiento.productoId)
        producto?.let { p ->
            val nuevoStock = if (movimiento.tipo == "ENTRADA") {
                p.cantidadEnStock + movimiento.cantidadAfectada
            } else {
                (p.cantidadEnStock - movimiento.cantidadAfectada).coerceAtLeast(0)
            }
            productoDao.actualizar(p.copy(cantidadEnStock = nuevoStock))
        }
    }

    // CORREGIDO: Acepta la lista de detalles y los borra después de procesarlos
    suspend fun procesarRecepcionPedido(detalles: List<DetallePedido>) {
        if (detalles.isEmpty()) return

        detalles.forEach { detalle ->
            val productoExistente = productoDao.obtenerProductoPorNombre(detalle.nombre)

            if (productoExistente != null) {
                val nuevoStock = productoExistente.cantidadEnStock + detalle.cantidadPedida
                productoDao.actualizar(productoExistente.copy(
                    cantidadEnStock = nuevoStock,
                    precio = detalle.precioUnitario
                ))

                movimientoDao.insertar(Movimiento(
                    productoId = productoExistente.productoId,
                    tipo = "ENTRADA",
                    cantidadAfectada = detalle.cantidadPedida,
                    razon = "Recepción de Pedido"
                ))
            } else {
                val nuevoId = productoDao.insertar(Producto(
                    nombre = detalle.nombre,
                    cantidadEnStock = detalle.cantidadPedida,
                    precio = detalle.precioUnitario,
                    descripcion = "",
                    ubicacion = ""
                ))

                movimientoDao.insertar(Movimiento(
                    productoId = nuevoId.toInt(),
                    tipo = "ENTRADA",
                    cantidadAfectada = detalle.cantidadPedida,
                    razon = "Producto Nuevo desde Pedido"
                ))
            }
            // Borramos el detalle individual una vez procesado
            detallePedidoDao.borrar(detalle)
        }
    }

    fun obtenerMovimientos(
        productoId: Int, tipo: String?, fechaInicio: Long, fechaFin: Long
    ): Flow<List<Movimiento>> {
        return movimientoDao.obtenerMovimientosDeProductoFiltrados(productoId, tipo, fechaInicio, fechaFin)
    }

    // --- Métodos para Detalles de Pedido ---
    fun obtenerDetallesPedido(): Flow<List<DetallePedido>> = detallePedidoDao.obtenerTodos()

    suspend fun insertarDetallePedido(detalle: DetallePedido) {
        detallePedidoDao.insertar(detalle)
    }

    suspend fun borrarDetallePedido(detalle: DetallePedido) {
        detallePedidoDao.borrar(detalle)
    }

    // AÑADIDO: La función que faltaba
    suspend fun limpiarTodosLosPedidos() {
        detallePedidoDao.borrarTodos()
    }
}
