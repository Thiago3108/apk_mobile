package com.thiago.apk_mobile.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa la tabla 'productos' en la base de datos de Room.
 */
@Entity(
    tableName = "productos",
    indices = [Index(value = ["nombre"], unique = false)]
)
data class Producto(
    @PrimaryKey(autoGenerate = true)
    val productoId: Int = 0,

    // Campos del Inventario
    val nombre: String,
    val descripcion: String,
    val precio: Double, // Precio de compra
    val precioVenta: Double, // Precio de venta
    val cantidadEnStock: Int,
    val ubicacion: String,

    // Campo de auditoría
    val fechaCreacion: Long = System.currentTimeMillis()
)
