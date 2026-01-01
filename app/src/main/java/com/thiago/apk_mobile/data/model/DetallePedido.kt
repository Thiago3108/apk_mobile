package com.thiago.apk_mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detalle_pedido")
data class DetallePedido(
    @PrimaryKey(autoGenerate = true)
    val detalleId: Int = 0,
    val nombre: String,
    val cantidadPedida: Int,
    val precioUnitario: Double,
    val estaMarcado: Boolean = false, // Para el checklist del Paso 2
    val productoIdReferencia: Int? = null // Si ya existe en inventario, guardamos su ID
)
