// ARCHIVO: com/thiago/apk_mobile/data/PedidoDao.kt (CORREGIDO)

package com.thiago.apk_mobile.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarDetalle(detalle: DetallePedido)

    @Query("SELECT * FROM detalle_pedido")
    fun obtenerTodosLosDetalles(): Flow<List<DetallePedido>>

    @Delete
    suspend fun eliminarDetalle(detalle: DetallePedido)

    @Query("DELETE FROM detalle_pedido WHERE detalleId = :id")
    suspend fun eliminarDetallePorId(id: Int)

    @Query("SELECT * FROM pedidos WHERE pedidoId = :id")
    suspend fun obtenerPedidoPorId(id: Int): Pedido?

    @Query("SELECT * FROM productos WHERE nombre = :nombre LIMIT 1")
    suspend fun obtenerProductoPorNombre(nombre: String): Producto?

    @Query("""
        SELECT * FROM pedidos 
        WHERE tipo = :tipo AND (:estado IS NULL OR estado = :estado) 
        ORDER BY fecha DESC
    """)
    fun obtenerPedidosPorTipoYEstado(tipo: String, estado: String?): Flow<List<Pedido>>
}