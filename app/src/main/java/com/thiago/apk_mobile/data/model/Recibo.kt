package com.thiago.apk_mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recibos")
data class Recibo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombreCliente: String,
    val telefonoCliente: String,
    val cedulaCliente: String,
    val referenciaCelular: String,
    val procedimiento: String,
    val claveDispositivo: String?,
    val precio: Double,
    val abono: Double,
    val fechaRegistro: Long,
    val fechaEntregaEstimada: Long,
    val estado: String = "SIN_ARREGLAR", // Valores: SIN_ARREGLAR, ARREGLADO, ENTREGADO
    val fechaDeEntregaReal: Long? = null,
    val fechaArreglado: Long? = null
)
