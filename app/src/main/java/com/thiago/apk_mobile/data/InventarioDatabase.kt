package com.thiago.apk_mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Producto::class, Movimiento::class, Pedido::class, DetallePedido::class, Factura::class, FacturaArticulo::class],
    version = 7, // Incrementar la versión por el cambio en el schema
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
                    // Permite que Room recree la base de datos si no encuentra una migración.
                    // ¡CUIDADO! Esto borrará los datos existentes.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
