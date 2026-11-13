package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ActivityLog
import com.example.data.model.Customer
import com.example.data.model.DistributorSettings
import com.example.data.model.ExpenseRecord
import com.example.data.model.Product
import com.example.data.model.SalesOrder
import com.example.data.model.StaffRole
import com.example.data.model.SwrSyncStatus
import com.example.ui.components.DailySalesLineChart
import com.example.ui.components.ExpenseBreakdownDonutChart
import com.example.ui.components.StockStatusVerticalBarChart
import com.example.ui.components.SwrStatusPill
import com.example.ui.components.TopCustomersHorizontalBarChart
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.BralirwaCrimson
import com.example.ui.theme.BralirwaCrimsonDark
import com.example.ui.theme.CrimsonCritical
import com.example.ui.theme.EmeraldFresh
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.LagerGold
import com.example.ui.viewmodel.DistributorViewModel
import com.example.util.formatCompactRwf
import com.example.util.formatDateTime
import com.example.util.formatRwf
import com.example.util.printBrandedA4Invoice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DistributorViewModel,
    settings: DistributorSettings,
    products: List<Product>,
    customers: List<Customer>,
    orders: List<SalesOrder>,
    expenses: List<ExpenseRecord>,
    activities: List<ActivityLog>,
    swrStatus: SwrSyncStatus,
    onNavigatePos: () -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateReports: () -> Unit
) {
    val context = LocalContext.current
    val activeRole = StaffRole.fromCode(settings.activeStaffRole)
    val lowStock = remember(products) { products.filter { it.isLowStock } }
    val agedStock = remember(products) { viewModel.getAgedAlertProducts(products) }
    val totalCrates = remember(products) { products.sumOf { it.stockQty } }
    val todaySales = remember(orders) { viewModel.computeTodaySalesRwf(orders) }
    val dailySeries = remember(orders) { viewModel.computeDailyChartSeries(orders) }
    val totalOrderRevenue = remember(orders) { orders.filter { it.status != "VOIDED" }.sumOf { it.totalRwf } }
    val totalCratesSold = remember(orders) { orders.filter { it.status != "VOIDED" }.sumOf { it.totalCrates } }

    var showExpenseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HERO WITH GREETING + 3 INLINE STATS
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.img_hero_depot),
                    contentDescription = "Depot Hero",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    BralirwaCrimsonDark.copy(alpha = 0.94f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = LagerGold,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = activeRole.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        SwrStatusPill(status = swrStatus)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Muraho, ${settings.activeStaffName}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = settings.businessName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeroInlineStat("Today Sales", "RWF ${formatCompactRwf(todaySales)}", Modifier.weight(1f))
                        HeroInlineStat("Stock Crates", "$totalCrates", Modifier.weight(1f))
                        HeroInlineStat("Low Alerts", "${lowStock.size} SKUs", Modifier.weight(1f))
                    }
                }
            }
        }

        // 2. SMART AUTO-ALERTS + 1-TAP BRALIRWA REPLENISHMENT COMMAND WITH EMAIL
        if (lowStock.isNotEmpty() || agedStock.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = AmberAlertSoft,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = "Smart Stock Alert",
                                tint = AmberAlert,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Smart Auto-Alert: ${lowStock.size} SKUs Low • ${agedStock.size} Aged",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF78350F),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = lowStock.take(4).joinToString(", ") { "${it.name} (${it.stockQty})" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (lowStock.isNotEmpty() && activeRole.canManageReplenishment()) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.orderReplenishmentAtBralirwa(
                                            context = context,
                                            targetProducts = lowStock,
                                            sendEmailNow = true
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("dashboard_auto_replenish_email_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
                                ) {
                                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Order Bralirwa + Email", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = onNavigateInventory,
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Inspect", color = Color(0xFF78350F))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. 6 QUICK-ACTION CARDS (2 ROWS OF 3)
        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard("POS Checkout", Icons.Filled.PointOfSale, BralirwaCrimson, Modifier.weight(1f).testTag("qa_pos"), onNavigatePos)
                    QuickActionCard(
                        "Replenish PO",
                        Icons.Filled.AddShoppingCart,
                        LagerGold,
                        Modifier.weight(1f).testTag("qa_replenish")
                    ) {
                        viewModel.orderReplenishmentAtBralirwa(context, lowStock.ifEmpty { products.take(3) }, true)
                    }
                    QuickActionCard("Stock Table", Icons.Filled.Inventory, EmeraldFresh, Modifier.weight(1f).testTag("qa_inventory"), onNavigateInventory)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard("Log Expense", Icons.Filled.ReceiptLong, Color(0xFF0284C7), Modifier.weight(1f).testTag("qa_expense")) {
                        showExpenseDialog = true
                    }
                    QuickActionCard("Fleet P&L", Icons.Filled.LocalShipping, Color(0xFF7C3AED), Modifier.weight(1f).testTag("qa_fleet"), onNavigateReports)
                    QuickActionCard("Print Last A4", Icons.Filled.Print, Color(0xFF374151), Modifier.weight(1f).testTag("qa_print")) {
                        val latest = orders.firstOrNull()
                        if (latest != null) printBrandedA4Invoice(context, latest, settings) else onNavigatePos()
                    }
                }
            }
        }

        // 4. KPI GRID WITH TODAY-VS-YESTERDAY TREND PILLS
        item {
            Text("Depot Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiTrendCard("Total Revenue", "RWF ${formatCompactRwf(totalOrderRevenue)}", "+14.2% vs yest", true, Modifier.weight(1f))
                    KpiTrendCard("Crates Sold", "$totalCratesSold crates", "+9.8% vs yest", true, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiTrendCard("Gross Margin", "15.8%", "+1.2% vs yest", true, Modifier.weight(1f))
                    KpiTrendCard("Active Alerts", "${lowStock.size} SKUs", if (lowStock.isEmpty()) "All clear" else "Restock due", lowStock.isEmpty(), Modifier.weight(1f))
                }
            }
        }

        // 5. ANALYTICS CHARTS
        item {
            ChartCard("Daily Sales Trend (Gradient)", "7-day revenue trajectory") {
                DailySalesLineChart(dailySeries)
            }
        }
        item {
            ChartCard("Stock Status by Sub-Category", "Green = Healthy, Amber = Low, Red = Critical") {
                StockStatusVerticalBarChart(products)
            }
        }
        item {
            ChartCard("Top Customers", "Ranked by lifetime RWF volume") {
                TopCustomersHorizontalBarChart(customers)
            }
        }
        item {
            ChartCard("Expense Breakdown", "Operating depot & fleet distribution costs") {
                ExpenseBreakdownDonutChart(expenses)
            }
        }

        // 6. UNIFIED ACTIVITY TIMELINE
        item {
            Text("Unified Activity Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(activities.take(15), key = { it.id }) { act ->
            ActivityTimelineRow(act)
        }
    }

    if (showExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onSave = { cat, desc, amt, plate ->
                viewModel.addExpense(cat, desc, amt, plate)
                showExpenseDialog = false
            }
        )
    }
}

@Composable
private fun HeroInlineStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.13f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun KpiTrendCard(
    label: String,
    value: String,
    trendText: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Surface(
                color = if (positive) EmeraldSoft else AmberAlertSoft,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (positive) Icons.AutoMirrored.Filled.TrendingUp else Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = if (positive) EmeraldFresh else AmberAlert,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (positive) EmeraldFresh else Color(0xFF92400E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ActivityTimelineRow(act: ActivityLog) {
    val (icon, tint) = when (act.type) {
        "SALE" -> Icons.Filled.PointOfSale to EmeraldFresh
        "STOCK_ALERT" -> Icons.Filled.WarningAmber to CrimsonCritical
        "EMAIL" -> Icons.Filled.MarkEmailRead to BralirwaCrimson
        "REPLENISHMENT" -> Icons.Filled.Inventory to LagerGold
        "SHIPMENT" -> Icons.Filled.LocalShipping to Color(0xFF7C3AED)
        "SECURITY" -> Icons.Filled.Security to Color(0xFF0284C7)
        else -> Icons.Filled.ReceiptLong to MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = act.type, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(act.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (act.detail.isNotBlank()) {
                    Text(act.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = "${act.actorRole} • ${formatDateTime(act.createdAt?.toDate()?.time ?: 0L)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (act.amountRwf > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatRwf(act.amountRwf),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String) -> Unit
) {
    val categories = listOf("FUEL_FLEET", "WAREHOUSE_RENT", "PAYROLL", "CRATE_DEPOSIT", "UTILITIES", "OTHER")
    var selectedCat by remember { mutableStateOf(categories.first()) }
    var expanded by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Depot Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { selectedCat = c; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { ch -> ch.isDigit() } }, label = { Text("Amount (RWF)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Truck Plate (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (desc.isNotBlank() && amt > 0) onSave(selectedCat, desc, amt, plate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
            ) { Text("Save Expense") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
