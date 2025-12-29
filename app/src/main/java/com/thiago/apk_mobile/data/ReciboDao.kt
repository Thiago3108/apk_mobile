package com.thiago.apk_mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.thiago.apk_mobile.data.model.Recibo
import kotlinx.coroutines.flow.Flow

@Dao
interface ReciboDao {

    @Insert
    suspend fun insert(recibo: Recibo)

    @Update
    suspend fun update(recibo: Recibo)

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
        ORDER BY fechaEntregaEstimada ASC
        """)
    fun getRecibosFiltrados(query: String, startDate: Long, endDate: Long): Flow<List<Recibo>>


    @Query("SELECT * FROM recibos WHERE id = :reciboId")
    fun getReciboById(reciboId: Long): Flow<Recibo>

}
