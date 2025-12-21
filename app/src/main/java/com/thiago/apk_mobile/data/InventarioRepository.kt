package com.thiago.apk_mobile.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class InventarioRepository(
    private val productoDao: ProductoDao,
    private val movimientoDao: MovimientoDao
) {
    val stockTotal: Flow<Int> = productoDao.obtenerStockTotal()
    val valorTotal: Flow<Double?> = productoDao.obtenerValorTotal()

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

    // NUEVA LÓGICA: PROCESAR RECEPCIÓN
    suspend fun procesarRecepcionPedido(detalles: List<DetallePedido>) {
        detalles.forEach { detalle ->
            val productoExistente = productoDao.obtenerProductoPorNombre(detalle.nombre)

            val productoId: Int = if (productoExistente != null) {
                productoExistente.productoId
            } else {
                val nuevoProducto = Producto(
                    nombre = detalle.nombre,
                    descripcion = "Registrado desde Pedidos",
                    precio = detalle.precioUnitario,
                    cantidadEnStock = 0,
                    ubicacion = "Almacén General"
                )
                productoDao.insertar(nuevoProducto).toInt()
            }

            val movimiento = Movimiento(
                productoId = productoId,
                tipo = "ENTRADA",
                cantidadAfectada = detalle.cantidadPedida,
                razon = "Recepción de pedido: ${detalle.nombre}",
                fecha = System.currentTimeMillis()
            )
            registrarMovimiento(movimiento)
        }
    }

    fun obtenerMovimientos(
        productoId: Int, tipo: String?, fechaInicio: Long, fechaFin: Long
    ): Flow<List<Movimiento>> {
        return movimientoDao.obtenerMovimientosDeProductoFiltrados(productoId, tipo, fechaInicio, fechaFin)
    }
}