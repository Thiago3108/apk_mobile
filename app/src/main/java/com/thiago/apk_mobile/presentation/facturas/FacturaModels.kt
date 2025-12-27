package com.thiago.apk_mobile.presentation.facturas

import com.thiago.apk_mobile.data.Producto

data class ArticuloFactura(
    val producto: Producto,
    var cantidad: Int
)
