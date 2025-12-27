package com.thiago.apk_mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Producto::class, Movimiento::class, Pedido::class, DetallePedido::class, Factura::class, FacturaArticulo::class],
    version = 5,
    exportSchema = false
)
abstract class InventarioDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun detallePedidoDao(): DetallePedidoDao
    abstract fun facturaDao(): FacturaDao

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
                    // Con esto, al subir la versión, la base de datos se recreará.
                    // ¡CUIDADO! Esto borra todos los datos existentes. Es útil para desarrollo.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
