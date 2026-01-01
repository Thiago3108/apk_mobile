package com.thiago.apk_mobile.presentation.facturas

import com.thiago.apk_mobile.data.model.Factura

/**
 * Clase de datos específica para la UI. No es parte de la base de datos.
 * Se construye en el ViewModel para facilitar la visualización en las pantallas.
 */
data class ArticuloVendidoDisplay(
    val productoNombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val total: Double
)

/**
 * Clase de datos específica para la UI. No es parte de la base de datos.
 * Representa una factura completa con sus artículos listos para ser mostrados.
 */
data class FacturaDisplay(
    val factura: Factura,
    val articulos: List<ArticuloVendidoDisplay>
)
