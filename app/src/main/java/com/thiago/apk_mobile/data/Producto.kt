package com.thiago.apk_mobile.data



import androidx.room.Entity

import androidx.room.Index

import androidx.room.PrimaryKey



/**

 * Entidad que representa la tabla 'productos' en la base de datos de Room.

 */

@Entity(tableName = "productos",

    indices = [Index(value = ["nombre"], unique = false)])

data class Producto(

// Clave primaria: ID único, generado automáticamente por la base de datos

    @PrimaryKey(autoGenerate = true)

    val productoId: Int = 0,



// Campos del Inventario

    val nombre: String,

    val descripcion: String,

    val precio: Double,

    val cantidadEnStock: Int,

    val ubicacion: String,



// Campo de auditoría

    val fechaCreacion: Long = System.currentTimeMillis()

)