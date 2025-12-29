package com.thiago.apk_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Receipt
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
import com.thiago.apk_mobile.presentation.facturas.FacturaDetailScreen
import com.thiago.apk_mobile.presentation.facturas.FacturaFormScreen
import com.thiago.apk_mobile.presentation.facturas.FacturasScreen
import com.thiago.apk_mobile.presentation.inventory.InventarioScreen
import com.thiago.apk_mobile.presentation.inventory.MovimientosScreen
import com.thiago.apk_mobile.presentation.InventarioViewModel
import com.thiago.apk_mobile.presentation.getInventarioViewModelFactory
import com.thiago.apk_mobile.presentation.pedidos.PedidosScreen
import com.thiago.apk_mobile.ui.recibos.CrearReciboScreen
import com.thiago.apk_mobile.ui.recibos.ReciboDetailScreen
import com.thiago.apk_mobile.ui.recibos.RecibosScreen
import com.thiago.apk_mobile.ui.theme.Apk_mobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

sealed class BottomBarScreen(val route: String, val label: String, val icon: ImageVector) {
    object Inventario : BottomBarScreen("inventario_section", "Inventario", Icons.Default.Inventory)
    object Pedidos : BottomBarScreen("pedidos_section", "Pedidos", Icons.Default.ListAlt)
    object Facturas : BottomBarScreen("facturas_section", "Facturas", Icons.Default.Description)
    object Recibos : BottomBarScreen("recibos_section", "Recibos", Icons.Default.Receipt)
}

object Destinations {
    const val DETAIL_ROUTE = "detail/{productoId}/{productoNombre}"
    const val FACTURA_FORM_ROUTE = "factura_form"
    const val FACTURA_DETAIL_ROUTE = "factura_detail/{facturaId}"
    const val FACTURA_FORM_WITH_ID_ROUTE = "factura_form?facturaId={facturaId}"
    const val CREAR_RECIBO_ROUTE = "crear_recibo"
    const val RECIBO_DETAIL_ROUTE = "recibo_detail/{reciboId}"
}


@Composable
fun InventoryApp(viewModel: InventarioViewModel) {
    val navController = rememberNavController()
    val screens = listOf(BottomBarScreen.Inventario, BottomBarScreen.Pedidos, BottomBarScreen.Facturas, BottomBarScreen.Recibos)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val shouldShowBottomBar = currentDestination?.route?.startsWith("factura_form") == false &&
                                      currentDestination?.route != Destinations.CREAR_RECIBO_ROUTE &&
                                      currentDestination?.route != Destinations.RECIBO_DETAIL_ROUTE

            if (shouldShowBottomBar) {
                NavigationBar {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            label = { Text(screen.label) },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomBarScreen.Inventario.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomBarScreen.Inventario.route) {
                InventarioScreen(
                    inventarioViewModel = viewModel,
                    onProductoClick = { productId, productName ->
                        navController.navigate("detail/$productId/$productName")
                    }
                )
            }

            composable(BottomBarScreen.Pedidos.route) {
                PedidosScreen()
            }

            composable(BottomBarScreen.Facturas.route) {
                FacturasScreen(
                    inventarioViewModel = viewModel,
                    onNavigateToForm = { navController.navigate(Destinations.FACTURA_FORM_WITH_ID_ROUTE.replace("{facturaId}", "0")) },
                    onNavigateToDetail = { facturaId -> navController.navigate("factura_detail/$facturaId") },
                    onNavigateToEdit = { facturaId -> navController.navigate("factura_form?facturaId=$facturaId")}
                )
            }
            
            composable(BottomBarScreen.Recibos.route){
                RecibosScreen(
                    onNavigateToCrearRecibo = { navController.navigate(Destinations.CREAR_RECIBO_ROUTE) },
                    onReciboClick = { reciboId -> navController.navigate("recibo_detail/$reciboId") }
                )
            }

            composable(Destinations.CREAR_RECIBO_ROUTE){
                CrearReciboScreen(onReciboCreado = { navController.popBackStack() })
            }

            composable(
                route = Destinations.RECIBO_DETAIL_ROUTE,
                arguments = listOf(navArgument("reciboId") { type = NavType.LongType })
            ) { backStackEntry ->
                val reciboId = backStackEntry.arguments?.getLong("reciboId")
                if (reciboId != null) {
                    ReciboDetailScreen(
                        reciboId = reciboId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Destinations.FACTURA_FORM_WITH_ID_ROUTE,
                arguments = listOf(navArgument("facturaId") { 
                    type = NavType.LongType
                    defaultValue = 0L
                })
            ) { backStackEntry ->
                val facturaId = backStackEntry.arguments?.getLong("facturaId")
                FacturaFormScreen(
                    facturaId = if(facturaId == 0L) null else facturaId,
                    inventarioViewModel = viewModel,
                    onSave = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Destinations.FACTURA_DETAIL_ROUTE,
                arguments = listOf(navArgument("facturaId") { type = NavType.LongType })
            ) { backStackEntry ->
                val facturaId = backStackEntry.arguments?.getLong("facturaId")
                if (facturaId != null) {
                    FacturaDetailScreen(
                        facturaId = facturaId,
                        inventarioViewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

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
