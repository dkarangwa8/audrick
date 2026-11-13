package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.DistributorSettings
import com.example.data.model.ExpenseRecord
import com.example.data.model.Product
import com.example.data.model.SalesOrder
import com.example.data.model.ShipmentRecord
import com.example.data.model.StockAgingBucket
import com.example.ui.components.DailySalesLineChart
import com.example.ui.components.ExpenseBreakdownDonutChart
import com.example.ui.components.TopCustomersHorizontalBarChart
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.BralirwaCrimson
import com.example.ui.theme.CrimsonCritical
import com.example.ui.theme.EmeraldFresh
import com.example.ui.theme.EmeraldSoft
import com.example.ui.viewmodel.DistributorViewModel
import com.example.util.buildOrdersCsv
import com.example.util.buildProductsCsv
import com.example.util.buildShipmentsCsv
import com.example.util.formatRwf
import com.example.util.printBrandedA4Invoice
import com.example.util.printBrandedReportA4
import com.example.util.printThermalReceipt
import com.example.util.sharePlainTextOrCsv

@Composable
fun ReportsScreen(
    viewModel: DistributorViewModel,
    settings: DistributorSettings,
    products: List<Product>,
    customers: List<Customer>,
    orders: List<SalesOrder>,
    expenses: List<ExpenseRecord>,
    shipments: List<ShipmentRecord>
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val reportTabs = listOf("Profit & Loss", "Sales Summary", "Stock Aging", "Customers", "Car / Fleet P&L")
    var showAddShipment by remember { mutableStateOf(false) }

    val validOrders = remember(orders) { orders.filter { it.status != "VOIDED" } }
    val revenue = remember(validOrders) { validOrders.sumOf { it.subtotalRwf } }
    val cogs = remember(validOrders) {
        validOrders.sumOf { o -> o.items.sumOf { it.costRateRwf * it.qty } }
    }
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amountRwf } }
    val grossProfit = revenue - cogs
    val netProfit = grossProfit - totalExpenses

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            reportTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        // Export & Print Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val csv = when (selectedTab) {
                        0, 1 -> buildOrdersCsv(orders)
                        2 -> buildProductsCsv(products)
                        4 -> buildShipmentsCsv(shipments)
                        else -> customers.joinToString("\n") { "${it.name},${it.zone},${it.totalSpentRwf},${it.creditBalanceRwf},${it.billingCycle}" }
                    }
                    sharePlainTextOrCsv(context, "Bralirwa_${reportTabs[selectedTab]}.csv", csv)
                },
                modifier = Modifier.weight(1f).height(44.dp).testTag("report_export_csv_btn")
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export CSV")
            }
            Button(
                onClick = {
                    val rows = when (selectedTab) {
                        0 -> "<table><tr><th>Metric</th><th>Amount</th></tr><tr><td>Revenue</td><td>${formatRwf(revenue)}</td></tr><tr><td>COGS</td><td>${formatRwf(cogs)}</td></tr><tr><td>Gross Profit</td><td>${formatRwf(grossProfit)}</td></tr><tr><td>Operating Expenses</td><td>${formatRwf(totalExpenses)}</td></tr><tr><td><strong>Net Profit</strong></td><td><strong>${formatRwf(netProfit)}</strong></td></tr></table>"
                        4 -> "<table><tr><th>Plate</th><th>Driver</th><th>Zone</th><th>Revenue</th><th>Trip Cost</th><th>Net P&L</th></tr>" +
                            shipments.joinToString("") { "<tr><td>${it.vehiclePlate}</td><td>${it.driverName}</td><td>${it.routeZone}</td><td>${formatRwf(it.revenueRwf)}</td><td>${formatRwf(it.fuelAndTripCostRwf)}</td><td>${formatRwf(it.netProfitRwf)}</td></tr>" } + "</table>"
                        else -> "<table><tr><th>Invoice</th><th>Customer</th><th>Crates</th><th>Total</th><th>Status</th></tr>" +
                            validOrders.joinToString("") { "<tr><td>${it.invoiceNumber}</td><td>${it.customerName}</td><td>${it.totalCrates}</td><td>${formatRwf(it.totalRwf)}</td><td>${it.status}</td></tr>" } + "</table>"
                    }
                    printBrandedReportA4(context, reportTabs[selectedTab], rows, settings)
                },
                modifier = Modifier.weight(1f).height(44.dp).testTag("report_print_a4_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Print A4 PDF")
            }
        }

        when (selectedTab) {
            // 0. PROFIT & LOSS
            0 -> {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Statement of Profit & Loss", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                PnlRow("Sales Revenue (Excl. VAT)", revenue, MaterialTheme.colorScheme.onSurface)
                                PnlRow("Cost of Goods Sold (Bralirwa)", -cogs, MaterialTheme.colorScheme.onSurfaceVariant)
                                PnlRow("Gross Profit", grossProfit, EmeraldFresh)
                                PnlRow("Operating Expenses", -totalExpenses, AmberAlert)
                                Surface(
                                    color = if (netProfit >= 0) EmeraldSoft else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("NET PROFIT", fontWeight = FontWeight.Bold)
                                        Text(formatRwf(netProfit), fontWeight = FontWeight.Bold, color = if (netProfit >= 0) EmeraldFresh else CrimsonCritical)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Expense Mix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                ExpenseBreakdownDonutChart(expenses)
                            }
                        }
                    }
                }
            }

            // 1. SALES SUMMARY
            1 -> {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("7-Day Sales Curve", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                DailySalesLineChart(viewModel.computeDailyChartSeries(orders))
                            }
                        }
                    }
                    items(orders, key = { it.id }) { ord ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${ord.invoiceNumber} • ${ord.customerName}", fontWeight = FontWeight.Bold)
                                    Text(formatRwf(ord.totalRwf), color = BralirwaCrimson, fontWeight = FontWeight.Bold)
                                }
                                Text("${ord.itemsSummary} • ${ord.paymentMethod} (${ord.status})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { printThermalReceipt(context, ord, settings) }) {
                                        Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Thermal")
                                    }
                                    Button(
                                        onClick = { printBrandedA4Invoice(context, ord, settings) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
                                    ) {
                                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Branded A4")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. STOCK AGING
            2 -> {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(products.sortedBy { it.batchReceivedEpochMs }, key = { "rep_age_${it.id}" }) { p ->
                        val days = StockAgingBucket.daysOld(p.batchReceivedEpochMs)
                        val bucket = StockAgingBucket.fromBatchEpoch(p.batchReceivedEpochMs)
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                    Text("${p.stockQty} crates • ${formatRwf(p.stockValueCostRwf)}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${bucket.label} (${days}d)", color = Color(bucket.colorHex), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // 3. CUSTOMER ANALYSIS & RECURRING BILLING CYCLES
            3 -> {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Top Customer Volume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                TopCustomersHorizontalBarChart(customers)
                            }
                        }
                    }
                    items(customers, key = { it.id }) { cust ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cust.name, fontWeight = FontWeight.Bold)
                                    Text("${cust.zone} • Billing: ${cust.billingCycle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Credit Due: ${formatRwf(cust.creditBalanceRwf)}", color = if (cust.creditBalanceRwf > 0) AmberAlert else EmeraldFresh, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                }
                                if (cust.billingCycle != "NONE" || cust.creditBalanceRwf > 0) {
                                    Button(
                                        onClick = { viewModel.sendRecurringBillingEmail(context, cust) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson),
                                        modifier = Modifier.testTag("bill_email_${cust.id}")
                                    ) {
                                        Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Bill Email")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. CAR / SHIPMENT P&L
            4 -> {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Button(
                            onClick = { showAddShipment = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_shipment_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Dispatch New Delivery Truck")
                        }
                    }
                    items(shipments, key = { it.id }) { ship ->
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = BralirwaCrimson)
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text("${ship.vehiclePlate} • ${ship.routeZone}", fontWeight = FontWeight.Bold)
                                            Text("${ship.driverName} • ${ship.cratesLoaded} crates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Surface(color = if (ship.status == "COMPLETED") EmeraldSoft else AmberAlertSoft, shape = RoundedCornerShape(50)) {
                                        Text(ship.status, style = MaterialTheme.typography.labelSmall, color = if (ship.status == "COMPLETED") EmeraldFresh else AmberAlert, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Rev: ${formatRwf(ship.revenueRwf)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Fuel/Trip: ${formatRwf(ship.fuelAndTripCostRwf)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Net: ${formatRwf(ship.netProfitRwf)}", fontWeight = FontWeight.Bold, color = if (ship.netProfitRwf >= 0) EmeraldFresh else CrimsonCritical)
                                }
                                if (ship.status != "COMPLETED") {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = { viewModel.completeShipment(ship.id) }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Mark Trip Completed")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddShipment) {
        AddShipmentDialog(
            onDismiss = { showAddShipment = false },
            onSave = { plate, driver, zone, crates, rev, cogsAmt, fuel ->
                viewModel.addShipment(plate, driver, zone, crates, rev, cogsAmt, fuel)
                showAddShipment = false
            }
        )
    }
}

@Composable
private fun PnlRow(label: String, amount: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(formatRwf(amount), fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun AddShipmentDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Double, Double, Double) -> Unit
) {
    var plate by remember { mutableStateOf("RAG 104 D") }
    var driver by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("Gisenyi - Musanze") }
    var crates by remember { mutableStateOf("120") }
    var rev by remember { mutableStateOf("1450000") }
    var cogs by remember { mutableStateOf("1210000") }
    var fuel by remember { mutableStateOf("85000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispatch Car / Shipment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Vehicle Plate") })
                OutlinedTextField(value = driver, onValueChange = { driver = it }, label = { Text("Driver Name") })
                OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("Route / Zone") })
                OutlinedTextField(value = crates, onValueChange = { crates = it.filter { c -> c.isDigit() } }, label = { Text("Crates Loaded") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = rev, onValueChange = { rev = it.filter { c -> c.isDigit() } }, label = { Text("Expected Revenue (RWF)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = cogs, onValueChange = { cogs = it.filter { c -> c.isDigit() } }, label = { Text("COGS (RWF)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = fuel, onValueChange = { fuel = it.filter { c -> c.isDigit() } }, label = { Text("Fuel & Trip Cost (RWF)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plate.isNotBlank() && driver.isNotBlank()) {
                        onSave(
                            plate,
                            driver,
                            zone,
                            crates.toIntOrNull() ?: 0,
                            rev.toDoubleOrNull() ?: 0.0,
                            cogs.toDoubleOrNull() ?: 0.0,
                            fuel.toDoubleOrNull() ?: 0.0
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
            ) { Text("Dispatch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
