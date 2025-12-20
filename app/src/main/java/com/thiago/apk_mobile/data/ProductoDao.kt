package com.thiago.apk_mobile.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :query || '%' ORDER BY nombre ASC")
    fun obtenerTodosProductos(query: String): PagingSource<Int, Producto>

    @Query("SELECT * FROM productos WHERE productoId = :id")
    suspend fun obtenerProductoPorId(id: Int): Producto?

    @Query("SELECT * FROM productos WHERE productoId = :id")
    fun obtenerProductoPorIdAsFlow(id: Int): Flow<Producto?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: Producto): Long

    @Update
    suspend fun actualizar(producto: Producto)

    @Delete
    suspend fun eliminar(producto: Producto)

    @Query("UPDATE productos SET cantidadEnStock = :nuevoStock WHERE productoId = :id")
    suspend fun actualizarStock(id: Int, nuevoStock: Int)

    // --- CONSULTAS PARA MÉTRICAS ---
    @Query("SELECT IFNULL(SUM(cantidadEnStock), 0) FROM productos")
    fun obtenerStockTotal(): Flow<Int>

    @Query("SELECT IFNULL(SUM(cantidadEnStock * precio), 0.0) FROM productos")
    fun obtenerValorTotal(): Flow<Double>
}