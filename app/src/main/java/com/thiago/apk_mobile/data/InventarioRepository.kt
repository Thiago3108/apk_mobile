package com.thiago.apk_mobile.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.thiago.apk_mobile.data.model.DetallePedido
import com.thiago.apk_mobile.data.model.Factura
import com.thiago.apk_mobile.data.model.FacturaArticulo
import com.thiago.apk_mobile.data.model.FacturaConArticulos
import com.thiago.apk_mobile.data.model.Movimiento
import com.thiago.apk_mobile.data.model.Producto
import com.thiago.apk_mobile.presentation.facturas.ArticuloFactura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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
        val facturaOriginal = getFacturaConArticulosById(facturaId).first()
        db.withTransaction {
            // Restaurar stock antes de borrar
            facturaOriginal?.articulos?.forEach { articuloOriginal ->
                val producto = obtenerProductoPorId(articuloOriginal.productoId)
                if (producto != null) {
                    actualizarProducto(producto.copy(cantidadEnStock = producto.cantidadEnStock + articuloOriginal.cantidad))
                }
            }
            facturaDao.deleteFacturaById(facturaId)
        }
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
            val total = articulos.sumOf { it.producto.precioVenta * it.cantidad }
            val factura = Factura(nombreCliente = nombreCliente, cedulaCliente = cedulaCliente, total = total)
            val facturaId = facturaDao.insertFactura(factura)

            val articulosDeFactura = articulos.map { 
                FacturaArticulo(
                    facturaId = facturaId,
                    productoId = it.producto.productoId,
                    cantidad = it.cantidad,
                    precioUnitario = it.producto.precioVenta // Usar precio de venta
                )
            }
            facturaDao.insertArticulos(articulosDeFactura)

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

    suspend fun updateFactura(facturaId: Long, nombreCliente: String, cedulaCliente: String, nuevosArticulos: List<ArticuloFactura>) {
        db.withTransaction {
            val facturaOriginal = getFacturaConArticulosById(facturaId).first() ?: return@withTransaction

            // 1. Restaurar stock de los productos originales
            facturaOriginal.articulos.forEach { articuloOriginal ->
                val producto = obtenerProductoPorId(articuloOriginal.productoId)!!
                actualizarProducto(producto.copy(cantidadEnStock = producto.cantidadEnStock + articuloOriginal.cantidad))
            }

            // 2. Borrar los artículos antiguos y la factura (se recreará con el mismo ID)
            facturaDao.deleteFacturaById(facturaOriginal.factura.facturaId)

            // 3. Crear la factura y los artículos nuevos con la info actualizada
            val totalNuevo = nuevosArticulos.sumOf { it.producto.precioVenta * it.cantidad }
            val facturaNueva = Factura(
                facturaId = facturaId, // <-- Usamos el mismo ID
                nombreCliente = nombreCliente,
                cedulaCliente = cedulaCliente,
                total = totalNuevo,
                fecha = facturaOriginal.factura.fecha // Mantenemos la fecha original
            )
            facturaDao.insertFactura(facturaNueva) // Usamos REPLACE, así que actualiza o inserta

            val articulosNuevosParaDb = nuevosArticulos.map {
                FacturaArticulo(facturaId, it.producto.productoId, it.cantidad, it.producto.precioVenta)
            }
            facturaDao.insertArticulos(articulosNuevosParaDb)

            // 4. Descontar el stock de los nuevos productos
            nuevosArticulos.forEach { articuloNuevo ->
                val producto = obtenerProductoPorId(articuloNuevo.producto.productoId)!!
                actualizarProducto(producto.copy(cantidadEnStock = producto.cantidadEnStock - articuloNuevo.cantidad))
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
                        precio = detalle.precioUnitario // Actualizamos precio de compra
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
                        precioVenta = detalle.precioUnitario, // Precio de venta inicial
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
