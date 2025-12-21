// Archivo: InventarioDatabase.kt (MODIFICADO)
package com.thiago.apk_mobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Producto::class, Movimiento::class, Pedido::class, DetallePedido::class],
    version = 4, // <-- INCREMENTA A 4
    exportSchema = false
)
abstract class InventarioDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun movimientoDao(): MovimientoDao
    // Agregaremos el DAO de pedidos más adelante para ir paso a paso
    // Por ahora solo necesitamos que las tablas existan

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