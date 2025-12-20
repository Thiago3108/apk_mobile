// Archivo: com/thiago/apk_mobile/data/Movimiento.kt



package com.thiago.apk_mobile.data



import androidx.room.Entity

import androidx.room.PrimaryKey



/**

 * Entidad que registra cada transacción de stock (entrada o salida).

 */

@Entity(tableName = "movimientos")

data class Movimiento(

// Clave primaria para el registro del movimiento

    @PrimaryKey(autoGenerate = true)

    val movimientoId: Int = 0,



// Clave foránea: ID del producto afectado (vincula al Producto.productoId)

    val productoId: Int,



// Detalles del movimiento

    val tipo: String, // Usaremos "ENTRADA" o "SALIDA"

    val cantidadAfectada: Int, // Cuántas unidades se movieron

    val razon: String, // "Compra", "Venta", "Ajuste por pérdida", etc.



// Marca de tiempo del movimiento

    val fecha: Long = System.currentTimeMillis()

)