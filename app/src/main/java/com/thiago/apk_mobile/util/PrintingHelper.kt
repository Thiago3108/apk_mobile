package com.thiago.apk_mobile.util

import com.thiago.apk_mobile.data.model.Recibo
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

    fun generatePrintableRecibo(template: String, recibo: Recibo): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaRegistro = Date(recibo.fechaRegistro)

        return template
            .replace("{{fecha}}", sdf.format(fechaRegistro))
            .replace("{{nombre_cliente}}", recibo.nombreCliente)
            .replace("{{cedula_cliente}}", recibo.cedulaCliente)
            .replace("{{telefono_cliente}}", recibo.telefonoCliente)
            .replace("{{equipo}}", recibo.referenciaCelular)
            .replace("{{procedimiento}}", recibo.procedimiento)
            .replace("{{valor}}", recibo.precio.toString())
            .replace("{{abono}}", recibo.abono.toString())
            .replace("{{clave}}", recibo.claveDispositivo ?: "___")
    }
}
