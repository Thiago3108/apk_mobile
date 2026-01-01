package com.thiago.apk_mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.thiago.apk_mobile.data.model.Factura
import com.thiago.apk_mobile.data.model.FacturaArticulo
import com.thiago.apk_mobile.data.model.FacturaConArticulos
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

@Dao
interface FacturaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFactura(factura: Factura): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticulos(articulos: List<FacturaArticulo>)

    @Transaction
    @Query("SELECT * FROM facturas WHERE nombreCliente LIKE '%' || :query || '%' AND fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC")
    fun getFacturas(query: String, fechaInicio: Long, fechaFin: Long): Flow<List<FacturaConArticulos>>

    @Transaction
    @Query("SELECT * FROM facturas WHERE facturaId = :facturaId")
    fun getFacturaConArticulosById(facturaId: Long): Flow<FacturaConArticulos?>
    
    @Transaction
    @Query("SELECT * FROM facturas WHERE fecha >= :startOfDay")
    fun getFacturasForToday(startOfDay: Long = getStartOfDay()): Flow<List<FacturaConArticulos>>

    @Query("SELECT * FROM facturas WHERE facturaId = :facturaId")
    suspend fun getFacturaById(facturaId: Long): Factura?

    @Query("DELETE FROM facturas WHERE facturaId = :facturaId")
    suspend fun deleteFacturaById(facturaId: Long)
}

private fun getStartOfDay(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}