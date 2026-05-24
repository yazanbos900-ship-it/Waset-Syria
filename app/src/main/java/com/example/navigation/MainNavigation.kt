package com.example.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductDetailsScreen
import com.example.ui.screens.VendorStoreScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Categories : Screen("categories", "Categories", Icons.Default.List)
    object Cart : Screen("cart", "Cart", Icons.Default.ShoppingCart)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    val navigationItems = listOf(
        Screen.Home,
        Screen.Categories,
        Screen.Cart,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Show bottom bar only on main screens
            val showBottomBar = navigationItems.any { it.route == currentRoute }
            
            if (showBottomBar) {
                NavigationBar {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
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
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "onboarding",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("onboarding") {
                com.example.ui.screens.OnboardingScreen(
                    onFinishOnboarding = {
                        navController.navigate("welcome") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("welcome") {
                com.example.ui.screens.WelcomeScreen(
                    onNavigateToLogin = {
                        navController.navigate("login")
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }
            composable("login") {
                com.example.ui.screens.LoginScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("welcome") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }
            composable("register") {
                com.example.ui.screens.RegisterScreen(
                    onNavigateToLogin = {
                        navController.navigateUp()
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToProduct = { productId ->
                        navController.navigate("product/$productId")
                    },
                    onNavigateToVendor = { vendorId ->
                        navController.navigate("vendor/$vendorId")
                    }
                )
            }
            composable(Screen.Categories.route) {
                com.example.ui.screens.PlaceholderScreen(title = "Categories", message = "Browse all categories")
            }
            composable(Screen.Cart.route) {
                com.example.ui.screens.PlaceholderScreen(title = "My Cart", message = "Your multi-vendor cart is empty")
            }
            composable(Screen.Profile.route) {
                com.example.ui.screens.PlaceholderScreen(title = "Profile", message = "Manage your account")
            }
            
            composable("product/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                ProductDetailsScreen(
                    productId = productId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToVendor = { vendorId ->
                        navController.navigate("vendor/$vendorId")
                    }
                )
            }
            
            composable("vendor/{vendorId}") { backStackEntry ->
                val vendorId = backStackEntry.arguments?.getString("vendorId") ?: return@composable
                VendorStoreScreen(
                    vendorId = vendorId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToProduct = { productId ->
                        navController.navigate("product/$productId")
                    }
                )
            }
        }
    }
}
