// ARCHIVO: com/thiago/apk_mobile/data/MovimientoDao.kt

package com.thiago.apk_mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(movimiento: Movimiento)

    /**
     * Obtiene los movimientos de un producto, permitiendo filtros opcionales por tipo y fechas.
     * @param tipo: Filtrar por "ENTRADA", "SALIDA", o null/"" para todos.
     * @param fechaInicio: Timestamp (milisegundos) mínimo para filtrar.
     * @param fechaFin: Timestamp (milisegundos) máximo para filtrar.
     */
    @Query("""
        SELECT * FROM movimientos 
        WHERE productoId = :productoId
          -- Filtro de Tipo: Si :tipo es NULL o vacío, se devuelve TRUE (se ignora el filtro)
          AND (:tipo IS NULL OR :tipo = '' OR tipo = :tipo)
          -- Filtro de Fechas: Siempre se aplicarán los límites de fecha
          AND (fecha >= :fechaInicio)
          AND (fecha <= :fechaFin)
        ORDER BY fecha DESC
    """)
    fun obtenerMovimientosDeProductoFiltrados(
        productoId: Int,
        tipo: String?,
        fechaInicio: Long,
        fechaFin: Long
    ): Flow<List<Movimiento>>

    // Conservamos la función suspendida existente
    @Query("SELECT * from movimientos WHERE movimientoId = :id")
    suspend fun obtenerDetalleMovimiento(id: Int): Movimiento
}