package com.thiago.apk_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.thiago.apk_mobile.presentation.inventory.InventarioScreen
import com.thiago.apk_mobile.presentation.inventory.MovimientosScreen
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory
import com.thiago.apk_mobile.presentation.pedidos.PedidosScreen
import com.thiago.apk_mobile.ui.theme.Apk_mobileTheme

// 1. Definición de las secciones principales para la barra inferior
sealed class BottomBarScreen(val route: String, val label: String, val icon: ImageVector) {
    object Inventario : BottomBarScreen("inventario_section", "Inventario", Icons.Default.Inventory)
    object Pedidos : BottomBarScreen("pedidos_section", "Pedidos", Icons.Default.ListAlt)
}

// Rutas internas (las que ya tenías)
object Destinations {
    const val INVENTARIO_HOME = "inventario_home"
    const val PEDIDOS_HOME = "pedidos_home"
    const val DETAIL_ROUTE = "detail/{productoId}/{productoNombre}"
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
    val screens = listOf(BottomBarScreen.Inventario, BottomBarScreen.Pedidos)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        label = { Text(screen.label) },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Evita acumular muchas instancias de la misma pantalla
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomBarScreen.Inventario.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // SECCIÓN: INVENTARIO (Contenedor)
            composable(BottomBarScreen.Inventario.route) {
                InventarioScreen(
                    inventarioViewModel = viewModel,
                    onProductoClick = { productId, productName ->
                        navController.navigate("detail/$productId/$productName")
                    }
                )
            }

            // SECCIÓN: PEDIDOS (Contenedor)
            composable(BottomBarScreen.Pedidos.route) {
                PedidosScreen()
            }

            // RUTA DE DETALLE (Fuera de la barra pero accesible desde Inventario)
            composable(
                route = Destinations.DETAIL_ROUTE,
                arguments = listOf(
                    navArgument("productoId") { type = NavType.IntType },
                    navArgument("productoNombre") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getInt("productoId")
                val productName = backStackEntry.arguments?.getString("productoNombre")
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
}