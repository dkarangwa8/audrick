package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.model.StaffRole
import com.example.data.repository.DistributorRepository
import com.example.ui.components.DistributorSwalModal
import com.example.ui.components.SwrStatusPill
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BralirwaPosTheme
import com.example.ui.viewmodel.DistributorViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_POS = "pos"
const val ROUTE_INVENTORY = "inventory"
const val ROUTE_REPORTS = "reports"
const val ROUTE_SETTINGS = "settings"

data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BralirwaPosTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            DistributorWorkspace(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "Depot Admin",
                userEmail = user.email ?: "depot@kigaliprime.rw"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributorWorkspace(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String
) {
    val viewModel: DistributorViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val swrDao = AppDatabase.getInstance(app).swrDao()
                DistributorViewModel(
                    repository = DistributorRepository(db = db, swrDao = swrDao),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail
                )
            }
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val replenishments by viewModel.replenishments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val shipments by viewModel.shipments.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val staffList by viewModel.staff.collectAsStateWithLifecycle()
    val swrStatus by viewModel.swrStatus.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val quickPayMode by viewModel.quickPayMode.collectAsStateWithLifecycle()
    val vatEnabled by viewModel.vatEnabled.collectAsStateWithLifecycle()
    val lastCompletedOrder by viewModel.lastCompletedOrder.collectAsStateWithLifecycle()
    val swalModal by viewModel.swalModal.collectAsStateWithLifecycle()
    val latestOtp by viewModel.latestOtp.collectAsStateWithLifecycle()

    val activeRole = StaffRole.fromCode(settings.activeStaffRole)
    val lowStockCount = products.count { it.isLowStock }

    // Role-Aware Menu Filtering
    val visibleDestinations = remember(activeRole) {
        buildList {
            add(NavDestination(ROUTE_DASHBOARD, "Dashboard", Icons.Filled.Dashboard))
            if (activeRole.canAccessPos()) {
                add(NavDestination(ROUTE_POS, "POS", Icons.Filled.PointOfSale))
            }
            if (activeRole.canAccessInventory()) {
                add(NavDestination(ROUTE_INVENTORY, "Stock", Icons.Filled.Inventory2))
            }
            if (activeRole.canViewFinancialReports()) {
                add(NavDestination(ROUTE_REPORTS, "Reports", Icons.Filled.Analytics))
            }
            add(NavDestination(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings))
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail {
                    visibleDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                NavIconWithBadge(dest, lowStockCount, cartItems.sumOf { it.qty })
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = {
                            val title = visibleDestinations.firstOrNull { it.route == currentRoute }?.label ?: "Bralirwa POS"
                            Text(title, fontWeight = FontWeight.Bold)
                        },
                        actions = {
                            SwrStatusPill(status = swrStatus, modifier = Modifier.padding(end = 12.dp))
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            visibleDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(ROUTE_DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        NavIconWithBadge(dest, lowStockCount, cartItems.sumOf { it.qty })
                                    },
                                    label = { Text(dest.label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            viewModel = viewModel,
                            settings = settings,
                            products = products,
                            customers = customers,
                            orders = orders,
                            expenses = expenses,
                            activities = activities,
                            swrStatus = swrStatus,
                            onNavigatePos = {
                                if (activeRole.canAccessPos()) navController.navigate(ROUTE_POS)
                            },
                            onNavigateInventory = {
                                if (activeRole.canAccessInventory()) navController.navigate(ROUTE_INVENTORY)
                            },
                            onNavigateReports = {
                                if (activeRole.canViewFinancialReports()) navController.navigate(ROUTE_REPORTS)
                            }
                        )
                    }
                    composable(ROUTE_POS) {
                        PosScreen(
                            viewModel = viewModel,
                            settings = settings,
                            products = products,
                            customers = customers,
                            cartItems = cartItems,
                            selectedCustomer = selectedCustomer,
                            paymentMethod = paymentMethod,
                            quickPayMode = quickPayMode,
                            vatEnabled = vatEnabled,
                            lastCompletedOrder = lastCompletedOrder
                        )
                    }
                    composable(ROUTE_INVENTORY) {
                        InventoryScreen(
                            viewModel = viewModel,
                            settings = settings,
                            products = products,
                            replenishments = replenishments
                        )
                    }
                    composable(ROUTE_REPORTS) {
                        ReportsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            products = products,
                            customers = customers,
                            orders = orders,
                            expenses = expenses,
                            shipments = shipments
                        )
                    }
                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            staffList = staffList,
                            latestOtp = latestOtp,
                            onSignedOut = {}
                        )
                    }
                }
            }
        }
    }

    DistributorSwalModal(
        modal = swalModal,
        onDismiss = { viewModel.dismissModal() }
    )
}

@Composable
private fun NavIconWithBadge(
    dest: NavDestination,
    lowStockCount: Int,
    cartCount: Int
) {
    val badgeCount = when (dest.route) {
        ROUTE_INVENTORY -> lowStockCount
        ROUTE_POS -> cartCount
        else -> 0
    }
    if (badgeCount > 0) {
        BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
            Icon(dest.icon, contentDescription = dest.label)
        }
    } else {
        Icon(dest.icon, contentDescription = dest.label)
    }
}
