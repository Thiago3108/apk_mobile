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
    fun getProductosPaginados(query: String): PagingSource<Int, Producto>

    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getProductos(): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE productoId = :id")
    suspend fun obtenerProductoPorId(id: Int): Producto?

    @Query("SELECT * FROM productos WHERE productoId = :id")
    fun obtenerProductoPorIdAsFlow(id: Int): Flow<Producto?>

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

    // NUEVA FUNCIÓN PARA EL PASO 4
    @Query("SELECT * FROM productos WHERE nombre = :nombre LIMIT 1")
    suspend fun obtenerProductoPorNombre(nombre: String): Producto?

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :query || '%' LIMIT 5")
    fun buscarSugerencias(query: String): Flow<List<Producto>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: Producto): Long
}