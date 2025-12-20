// Archivo: InventarioDatabase.kt (MODIFICADO)



package com.thiago.apk_mobile.data



import android.content.Context

import androidx.room.Database

import androidx.room.Room

import androidx.room.RoomDatabase



@Database(

    entities = [Producto::class, Movimiento::class],

    version = 3, // <-- VERSIÓN INCREMENTADA

    exportSchema = false

)

abstract class InventarioDatabase : RoomDatabase() {



    abstract fun productoDao(): ProductoDao

    abstract fun movimientoDao(): MovimientoDao



    companion object {

        @Volatile

        private var INSTANCE: InventarioDatabase? = null



        fun getDatabase(context: Context): InventarioDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(

                    context.applicationContext,

                    InventarioDatabase::class.java,

                    "inventario_db" // Nombre del archivo de la base de datos

                )

                    .fallbackToDestructiveMigration()

                    .build()

                INSTANCE = instance

                return instance

            }

        }

    }

}