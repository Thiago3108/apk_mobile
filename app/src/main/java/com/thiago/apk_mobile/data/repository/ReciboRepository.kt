package com.thiago.apk_mobile.data.repository

import com.thiago.apk_mobile.data.ReciboDao
import com.thiago.apk_mobile.data.model.Recibo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReciboRepository @Inject constructor(private val reciboDao: ReciboDao) {

    fun getRecibosFiltrados(query: String, startDate: Long, endDate: Long, estado: String?): Flow<List<Recibo>> {
        return reciboDao.getRecibosFiltrados(query, startDate, endDate, estado)
    }

    suspend fun insertar(recibo: Recibo) {
        reciboDao.insert(recibo)
    }

    suspend fun actualizar(recibo: Recibo) {
        reciboDao.update(recibo)
    }

    suspend fun eliminar(recibo: Recibo) {
        reciboDao.delete(recibo)
    }

    fun getRecibo(id: Long): Flow<Recibo> {
        return reciboDao.getReciboById(id)
    }
}
