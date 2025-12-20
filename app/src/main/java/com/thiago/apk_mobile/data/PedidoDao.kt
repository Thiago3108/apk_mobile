// ARCHIVO: com/thiago/apk_mobile/data/PedidoDao.kt (CORREGIDO)

package com.thiago.apk_mobile.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(pedido: Pedido): Long

    @Update
    suspend fun actualizar(pedido: Pedido)

    @Delete
    suspend fun eliminar(pedido: Pedido)

    @Query("SELECT * FROM pedidos WHERE pedidoId = :id")
    suspend fun obtenerPedidoPorId(id: Int): Pedido?

    @Query("""
        SELECT * FROM pedidos 
        WHERE tipo = :tipo AND (:estado IS NULL OR estado = :estado) 
        ORDER BY fecha DESC
    """)
    fun obtenerPedidosPorTipoYEstado(tipo: String, estado: String?): Flow<List<Pedido>>
}