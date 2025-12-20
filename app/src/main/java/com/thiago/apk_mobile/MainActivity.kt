package com.thiago.apk_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thiago.apk_mobile.presentation.inventory.InventarioScreen
import com.thiago.apk_mobile.presentation.inventory.MovimientosScreen
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory
import com.thiago.apk_mobile.ui.theme.Apk_mobileTheme

// Definición de Rutas
object Destinations {
    const val HOME_ROUTE = "home"
    const val DETAIL_ROUTE = "detail/{productoId}/{productoNombre}"
    const val PRODUCTO_ID_KEY = "productoId"
    const val PRODUCTO_NOMBRE_KEY = "productoNombre"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Apk_mobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Inicializamos el ViewModel con el Factory
                    val viewModel: InventarioViewModel = viewModel(
                        factory = getInventarioViewModelFactory(applicationContext)
                    )
                    InventoryApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun InventoryApp(viewModel: InventarioViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME_ROUTE
    ) {
        // 1. Pantalla Principal
        composable(Destinations.HOME_ROUTE) {
            InventarioScreen(
                inventarioViewModel = viewModel,
                onProductoClick = { productId, productName ->
                    navController.navigate("detail/$productId/$productName")
                }
            )
        }

        // 2. Pantalla de Detalle de Producto (Movimientos)
        composable(
            Destinations.DETAIL_ROUTE,
            arguments = listOf(
                navArgument(Destinations.PRODUCTO_ID_KEY) { type = NavType.IntType },
                navArgument(Destinations.PRODUCTO_NOMBRE_KEY) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt(Destinations.PRODUCTO_ID_KEY)
            val productName = backStackEntry.arguments?.getString(Destinations.PRODUCTO_NOMBRE_KEY)
            if (productId != null && productName != null) {
                MovimientosScreen(
                    productoId = productId,
                    productoNombre = productName,
                    onBackClick = { navController.popBackStack() },
                    inventarioViewModel = viewModel
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Apk_mobileTheme {
        // Para la preview, necesitarías un ViewModel de prueba o un estado simulado.
    }
}
