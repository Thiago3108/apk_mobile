package com.thiago.apk_mobile.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.thiago.apk_mobile.data.model.Recibo
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

@Dao
interface ReciboDao {

    @Insert
    suspend fun insert(recibo: Recibo)

    @Update
    suspend fun update(recibo: Recibo)

    @Delete
    suspend fun delete(recibo: Recibo)

    @Query("SELECT * FROM recibos ORDER BY fechaEntregaEstimada ASC")
    fun getAllRecibos(): Flow<List<Recibo>>

    @Query("""
        SELECT * FROM recibos 
        WHERE (:query = '' OR nombreCliente LIKE '%' || :query || '%') 
        AND (
            -- Si no se selecciona un rango de fechas, se muestran todos los recibos
            (:startDate = 0 AND :endDate = 9223372036854775807) 
            OR 
            -- Si se selecciona, se filtra por la fecha de entrega real
            (fechaDeEntregaReal BETWEEN :startDate AND :endDate)
        )
        AND (:estado IS NULL OR :estado = '' OR estado = :estado)
        ORDER BY fechaEntregaEstimada ASC
        """)
    fun getRecibosFiltrados(query: String, startDate: Long, endDate: Long, estado: String?): Flow<List<Recibo>>


    @Query("SELECT * FROM recibos WHERE id = :reciboId")
    fun getReciboById(reciboId: Long): Flow<Recibo>

    @Query("SELECT COUNT(*) FROM recibos WHERE estado = 'SIN_ARREGLAR'")
    fun getSinArreglarCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recibos WHERE estado = 'ARREGLADO'")
    fun getArregladoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recibos WHERE fechaArreglado >= :startOfDay AND estado = 'ARREGLADO'")
    fun getReparacionesTerminadasHoy(startOfDay: Long = getStartOfDay()): Flow<Int>

    @Query("SELECT COUNT(*) FROM recibos WHERE fechaDeEntregaReal >= :startOfDay AND estado = 'ENTREGADO'")
    fun getEquiposEntregadosHoy(startOfDay: Long = getStartOfDay()): Flow<Int>

}

private fun getStartOfDay(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}