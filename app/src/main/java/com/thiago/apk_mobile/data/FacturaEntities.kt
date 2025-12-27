package com.thiago.apk_mobile.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "facturas")
data class Factura(
    @PrimaryKey(autoGenerate = true)
    val facturaId: Long = 0,
    val nombreCliente: String,
    val cedulaCliente: String,
    val total: Double,
    val fecha: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "factura_articulos",
    primaryKeys = ["facturaId", "productoId"],
    indices = [Index(value = ["productoId"])], // <-- ÍNDICE AÑADIDO
    foreignKeys = [
        ForeignKey(
            entity = Factura::class,
            parentColumns = ["facturaId"],
            childColumns = ["facturaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["productoId"],
            childColumns = ["productoId"]
        )
    ]
)
data class FacturaArticulo(
    val facturaId: Long,
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double // Store price at the time of sale
)

data class FacturaConArticulos(
    @Embedded val factura: Factura,
    @Relation(
        parentColumn = "facturaId",
        entityColumn = "facturaId",
        entity = FacturaArticulo::class
    )
    val articulos: List<FacturaArticulo>
)
