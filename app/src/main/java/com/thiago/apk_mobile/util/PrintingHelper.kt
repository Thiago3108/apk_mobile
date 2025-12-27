package com.thiago.apk_mobile.util

import com.thiago.apk_mobile.presentation.facturas.FacturaDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintingHelper {

    fun generatePrintableFactura(template: String, factura: FacturaDisplay): String {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        var processedText = template
            .replace("{{fecha}}", dateFormatter.format(Date(factura.factura.fecha)))
            .replace("{{nombre_cliente}}", factura.factura.nombreCliente)
            .replace("{{cedula_cliente}}", factura.factura.cedulaCliente)
            .replace("{{total_final}}", String.format("%.2f", factura.factura.total))

        // --- Procesado de Artículos (sin Regex para máxima compatibilidad) ---
        val blockStart = "{{#each articulos}}"
        val blockEnd = "{{/each}}"

        val startIndex = processedText.indexOf(blockStart)
        val endIndex = processedText.indexOf(blockEnd)

        if (startIndex != -1 && endIndex != -1) {
            val articleTemplate = processedText.substring(startIndex + blockStart.length, endIndex).trim()
            val fullBlock = processedText.substring(startIndex, endIndex + blockEnd.length)

            val articlesContent = factura.articulos.joinToString(separator = "\n") { articulo ->
                articleTemplate
                    .replace("{{nombre}}", articulo.productoNombre.padEnd(14).take(14))
                    .replace("{{cantidad}}", articulo.cantidad.toString().padEnd(4))
                    .replace("{{valor_unitario}}", String.format("%.0f", articulo.precioUnitario).padEnd(9))
                    .replace("{{total}}", String.format("%.0f", articulo.total).padEnd(7))
            }
            processedText = processedText.replace(fullBlock, articlesContent)
        }

        return processedText
    }
}
