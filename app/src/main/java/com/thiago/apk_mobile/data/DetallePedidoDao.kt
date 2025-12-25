package com.thiago.apk_mobile.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetallePedidoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(detalle: DetallePedido)

    @Query("SELECT * FROM detalle_pedido ORDER BY detalleId ASC")
    fun obtenerTodos(): Flow<List<DetallePedido>>

    @Query("DELETE FROM detalle_pedido")
    suspend fun borrarTodos()

    @Query("SELECT * FROM detalle_pedido")
    suspend fun obtenerTodosSuspend(): List<DetallePedido>

    @Delete
    suspend fun borrar(detalle: DetallePedido)
}
