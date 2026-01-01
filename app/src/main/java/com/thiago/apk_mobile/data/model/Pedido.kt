package com.thiago.apk_mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pedidos")
data class Pedido(
    @PrimaryKey(autoGenerate = true)
    val pedidoId: Int = 0,
    val tipo: String, // "VENTA" o "REQUERIMIENTO"
    val proveedor: String,
    val total: Double,
    val estado: String = "PENDIENTE_COMPRA", // "PENDIENTE_COMPRA", "STOCK_CONFIRMADO"
    val fecha: Long = System.currentTimeMillis()
)
