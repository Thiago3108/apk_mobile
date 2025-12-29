package com.thiago.apk_mobile.utils

import com.thiago.apk_mobile.data.model.Recibo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun createReciboPrintText(recibo: Recibo): String {
    val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val fechaRegistro = Date(recibo.fechaRegistro)

    return """
    CELUMAX SOCORRO
    Servicio Técnico Especializado
    Carrera 15 # 11 - 46
    Tels:    3205218943  3013815335
    Darío Martínez
    Nit: 91109755-3

    Fecha:   ${sdfDate.format(fechaRegistro)} ${sdfTime.format(fechaRegistro)}
    Nombre:   ${recibo.nombreCliente}
    CC.:  ${recibo.cedulaCliente}
    Tel:   ${recibo.telefonoCliente}
    Equipo:  ${recibo.referenciaCelular}
    Procedimiento: ${recibo.procedimiento}
    Valor: ${recibo.precio}
    Abono:  ${recibo.abono}

        *   *   *
        *   *   *
        *   *   *      Clave: ${recibo.claveDispositivo ?: "___"}

    Se recibe de buena fe el equipo, presumiendo su buen proceder y con previa verificación de su imei en la página web www.imeicolombia.com.co al día de hoy: para efectos de responsabilidad y exoneración de las garantías, se tendrá en cuenta lo estipulado en los artículos 932, 958, 1061, 1062, 1063 de el código de comercio y los artículos 12, 15, 16, 22 de la ley 1480 estatuto del consumidor.
    Nota: Artículos o piezas usadas no tienen garantía, repuesto como LCD Display. Flex y otros deberán ser revisados por el cliente previamente antes de salir del establecimiento, pues están exentos de garantía, no se responde por Sim Card o Tarjetas SD dejadas dentro del equipo, pasados 30 días no nos hacemos responsables por equipos dejados en reparación y el equipo será considerado en abandono. Así como está estipulado en el capitulo ll Artículo 18 de la ley 1450 el bodegaje tendrá un costo de $100M/C la hora. 

    Técnico: Darío Martínez
    """
}

