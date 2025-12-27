package com.thiago.apk_mobile.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.thiago.apk_mobile.presentation.facturas.ArticuloFactura
import kotlinx.coroutines.flow.Flow

class InventarioRepository(
    private val db: InventarioDatabase
) {
    private val productoDao = db.productoDao()
    private val movimientoDao = db.movimientoDao()
    private val detallePedidoDao = db.detallePedidoDao()
    private val facturaDao = db.facturaDao()

    val stockTotal: Flow<Int> = productoDao.obtenerStockTotal()
    val valorTotal: Flow<Double?> = productoDao.obtenerValorTotal()

    fun getProductos(): Flow<List<Producto>> {
        return productoDao.getProductos()
    }

    fun getFacturas(query: String, fechaInicio: Long, fechaFin: Long): Flow<List<FacturaConArticulos>> {
        return facturaDao.getFacturas(query, fechaInicio, fechaFin)
    }

    fun getFacturaConArticulosById(facturaId: Long): Flow<FacturaConArticulos?> {
        return facturaDao.getFacturaConArticulosById(facturaId)
    }

    suspend fun deleteFacturaById(facturaId: Long) {
        facturaDao.deleteFacturaById(facturaId)
    }

    fun obtenerSugerencias(query: String): Flow<List<Producto>> {
        return productoDao.buscarSugerencias(query)
    }

    fun obtenerProductosPaginados(query: String): Flow<PagingData<Producto>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { productoDao.getProductosPaginados(query) }
        ).flow
    }

    suspend fun insertarProducto(producto: Producto): Long = productoDao.insertar(producto)
    suspend fun actualizarProducto(producto: Producto) = productoDao.actualizar(producto)
    suspend fun eliminarProducto(producto: Producto) = productoDao.eliminar(producto)
    suspend fun obtenerProductoPorId(id: Int): Producto? = productoDao.obtenerProductoPorId(id)
    fun obtenerProductoPorIdAsFlow(id: Int): Flow<Producto?> = productoDao.obtenerProductoPorIdAsFlow(id)

    suspend fun registrarMovimiento(movimiento: Movimiento) {
        // This logic is now part of transactional operations like crearFactura or procesarRecepcionPedido
        // However, it can be kept for manual adjustments if needed.
        db.withTransaction {
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
    }

    suspend fun crearFactura(nombreCliente: String, cedulaCliente: String, articulos: List<ArticuloFactura>) {
        db.withTransaction {
            // 1. Crear y guardar la factura
            val total = articulos.sumOf { it.producto.precio * it.cantidad }
            val factura = Factura(nombreCliente = nombreCliente, cedulaCliente = cedulaCliente, total = total)
            val facturaId = facturaDao.insertFactura(factura)

            // 2. Crear y guardar los artículos de la factura
            val articulosDeFactura = articulos.map {
                FacturaArticulo(
                    facturaId = facturaId,
                    productoId = it.producto.productoId,
                    cantidad = it.cantidad,
                    precioUnitario = it.producto.precio
                )
            }
            facturaDao.insertArticulos(articulosDeFactura)

            // 3. Actualizar el stock de cada producto y registrar el movimiento
            for (articulo in articulos) {
                val producto = articulo.producto
                val nuevoStock = (producto.cantidadEnStock - articulo.cantidad).coerceAtLeast(0)
                productoDao.actualizar(producto.copy(cantidadEnStock = nuevoStock))

                movimientoDao.insertar(Movimiento(
                    productoId = producto.productoId,
                    tipo = "SALIDA",
                    cantidadAfectada = articulo.cantidad,
                    razon = "Venta Factura #$facturaId"
                ))
            }
        }
    }


    suspend fun procesarRecepcionPedido(detalles: List<DetallePedido>) {
         db.withTransaction {
            if (detalles.isEmpty()) return@withTransaction

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
                detallePedidoDao.borrar(detalle)
            }
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

    suspend fun limpiarTodosLosPedidos() {
        detallePedidoDao.borrarTodos()
    }
}
