package com.thiago.apk_mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thiago.apk_mobile.data.model.Pedido
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pedido: Pedido)

    @Query("SELECT * FROM pedidos ORDER BY fecha DESC")
    fun getAllPedidos(): Flow<List<Pedido>>

    @Query("SELECT * FROM pedidos WHERE pedidoId = :id")
    suspend fun getPedidoById(id: Int): Pedido?

    @Query("""
        SELECT * FROM pedidos
        WHERE tipo = :tipo AND (:estado IS NULL OR estado = :estado)
        ORDER BY fecha DESC
    """)
    fun getPedidosPorTipoYEstado(tipo: String, estado: String?): Flow<List<Pedido>>

    @Query("SELECT * FROM pedidos ORDER BY fecha DESC LIMIT :limit")
    fun getUltimosPedidos(limit: Int): Flow<List<Pedido>>
}
