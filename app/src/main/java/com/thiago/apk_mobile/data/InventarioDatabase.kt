package com.thiago.apk_mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.thiago.apk_mobile.data.model.DetallePedido
import com.thiago.apk_mobile.data.model.Factura
import com.thiago.apk_mobile.data.model.FacturaArticulo
import com.thiago.apk_mobile.data.model.Movimiento
import com.thiago.apk_mobile.data.model.Pedido
import com.thiago.apk_mobile.data.model.Producto
import com.thiago.apk_mobile.data.model.Recibo

@Database(
    entities = [Producto::class, Movimiento::class, Pedido::class, DetallePedido::class, Factura::class, FacturaArticulo::class, Recibo::class],
    version = 13, 
    exportSchema = false
)
abstract class InventarioDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun detallePedidoDao(): DetallePedidoDao
    abstract fun facturaDao(): FacturaDao
    abstract fun reciboDao(): ReciboDao
    abstract fun pedidoDao(): PedidoDao

    companion object {
        @Volatile
        private var INSTANCE: InventarioDatabase? = null

        fun getDatabase(context: Context): InventarioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventarioDatabase::class.java,
                    "inventario_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
