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

    // Dentro de InventarioRepository.kt agrega esta función:
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

    // NUEVA LÓGICA: PROCESAR RECEPCIÓN

    suspend fun procesarRecepcionPedido(detalles: List<DetallePedido>) {
        detalles.forEach { detalle ->
            // 1. Buscar si el producto ya existe por nombre
            val productoExistente = productoDao.obtenerProductoPorNombre(detalle.nombre)

            if (productoExistente != null) {
                // 2. Si existe, actualizar stock y precio
                val nuevoStock = productoExistente.cantidadEnStock + detalle.cantidadPedida
                productoDao.actualizar(productoExistente.copy(
                    cantidadEnStock = nuevoStock,
                    precio = detalle.precioUnitario
                ))

                // REGISTRO DE MOVIMIENTO (Nombres corregidos según tu error)
                movimientoDao.insertar(Movimiento(
                    productoId = productoExistente.productoId,
                    tipo = "ENTRADA",
                    cantidadAfectada = detalle.cantidadPedida, // Antes decía 'cantidad'
                    razon = "Recepción de Pedido"             // Antes decía 'motivo'
                ))
            } else {
                // 3. Si no existe, crear producto nuevo
                // NOTA: He añadido descripción y ubicación vacíos para cumplir con los parámetros que te faltaban
                val nuevoId = productoDao.insertar(Producto(
                    nombre = detalle.nombre,
                    cantidadEnStock = detalle.cantidadPedida,
                    precio = detalle.precioUnitario,
                    descripcion = "",       // Parámetro requerido según el error
                    ubicacion = ""          // Parámetro requerido según el error
                    // Si tu modelo no usa 'categoria', asegúrate de borrarla de aquí
                ))

                movimientoDao.insertar(Movimiento(
                    productoId = nuevoId.toInt(),
                    tipo = "ENTRADA",
                    cantidadAfectada = detalle.cantidadPedida, // Antes decía 'cantidad'
                    razon = "Producto Nuevo desde Pedido"      // Antes decía 'motivo'
                ))
            }
        }
    }

    fun obtenerMovimientos(
        productoId: Int, tipo: String?, fechaInicio: Long, fechaFin: Long
    ): Flow<List<Movimiento>> {
        return movimientoDao.obtenerMovimientosDeProductoFiltrados(productoId, tipo, fechaInicio, fechaFin)
    }
}