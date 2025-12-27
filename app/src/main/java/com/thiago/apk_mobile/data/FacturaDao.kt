package com.thiago.apk_mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

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

    @Query("DELETE FROM facturas WHERE facturaId = :facturaId")
    suspend fun deleteFacturaById(facturaId: Long)
}
