package com.thiago.apk_mobile.di

import android.content.Context
import com.thiago.apk_mobile.data.DetallePedidoDao
import com.thiago.apk_mobile.data.FacturaDao
import com.thiago.apk_mobile.data.InventarioDatabase
import com.thiago.apk_mobile.data.MovimientoDao
import com.thiago.apk_mobile.data.PedidoDao
import com.thiago.apk_mobile.data.ProductoDao
import com.thiago.apk_mobile.data.ReciboDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): InventarioDatabase {
        return InventarioDatabase.getDatabase(appContext)
    }

    @Provides
    fun provideReciboDao(database: InventarioDatabase): ReciboDao {
        return database.reciboDao()
    }

    @Provides
    fun provideProductoDao(database: InventarioDatabase): ProductoDao {
        return database.productoDao()
    }

    @Provides
    fun provideMovimientoDao(database: InventarioDatabase): MovimientoDao {
        return database.movimientoDao()
    }

    @Provides
    fun provideDetallePedidoDao(database: InventarioDatabase): DetallePedidoDao {
        return database.detallePedidoDao()
    }

    @Provides
    fun provideFacturaDao(database: InventarioDatabase): FacturaDao {
        return database.facturaDao()
    }

    @Provides
    fun providePedidoDao(database: InventarioDatabase): PedidoDao {
        return database.pedidoDao()
    }
}
