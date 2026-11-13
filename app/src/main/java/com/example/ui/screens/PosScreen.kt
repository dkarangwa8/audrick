package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.DistributorSettings
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.QuickPayMode
import com.example.data.model.SalesOrder
import com.example.data.model.StaffRole
import com.example.ui.components.ProductThumbnail
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.BralirwaCrimson
import com.example.ui.theme.BralirwaCrimsonDark
import com.example.ui.theme.EmeraldFresh
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.LagerGold
import com.example.ui.viewmodel.DistributorViewModel
import com.example.util.formatRwf
import com.example.util.printBrandedA4Invoice
import com.example.util.printThermalReceipt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: DistributorViewModel,
    settings: DistributorSettings,
    products: List<Product>,
    customers: List<Customer>,
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    paymentMethod: PaymentMethod,
    quickPayMode: QuickPayMode,
    vatEnabled: Boolean,
    lastCompletedOrder: SalesOrder?
) {
    val context = LocalContext.current
    val activeRole = StaffRole.fromCode(settings.activeStaffRole)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var pickerProduct by remember { mutableStateOf<Product?>(null) }
    var showCustomerModal by remember { mutableStateOf(false) }
    var showMobileCartSheet by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.skuCode.contains(searchQuery, ignoreCase = true) ||
                p.subCategory.contains(searchQuery, ignoreCase = true)
            val matchesCat = when (selectedCategory) {
                "ALL" -> true
                "BEER" -> p.category == ProductCategory.BEER.code
                "SOFT_DRINK" -> p.category == ProductCategory.SOFT_DRINK.code
                else -> p.subCategory.equals(selectedCategory, ignoreCase = true)
            }
            matchesSearch && matchesCat
        }
    }

    val subtotal = cartItems.sumOf { it.lineTotalRwf }
    val vatAmt = if (vatEnabled) subtotal * (settings.vatRate / 100.0) else 0.0
    val grandTotal = subtotal + vatAmt

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1.35f).fillMaxHeight()) {
                    PosCatalogPane(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        products = filteredProducts,
                        onProductClick = { pickerProduct = it },
                        lastCompletedOrder = lastCompletedOrder,
                        onPrintA4 = { lastCompletedOrder?.let { printBrandedA4Invoice(context, it, settings) } },
                        onPrintThermal = { lastCompletedOrder?.let { printThermalReceipt(context, it, settings) } }
                    )
                }
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    PosCartCheckoutPane(
                        viewModel = viewModel,
                        settings = settings,
                        activeRole = activeRole,
                        cartItems = cartItems,
                        selectedCustomer = selectedCustomer,
                        paymentMethod = paymentMethod,
                        quickPayMode = quickPayMode,
                        vatEnabled = vatEnabled,
                        subtotal = subtotal,
                        vatAmt = vatAmt,
                        grandTotal = grandTotal,
                        onOpenCustomerPicker = { showCustomerModal = true }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    PosCatalogPane(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        products = filteredProducts,
                        onProductClick = { pickerProduct = it },
                        lastCompletedOrder = lastCompletedOrder,
                        onPrintA4 = { lastCompletedOrder?.let { printBrandedA4Invoice(context, it, settings) } },
                        onPrintThermal = { lastCompletedOrder?.let { printThermalReceipt(context, it, settings) } },
                        bottomPadding = 92.dp
                    )
                }

                // Floating POS Cart Bar on compact screens
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { showMobileCartSheet = true }
                        .testTag("pos_open_cart_bar"),
                    shape = RoundedCornerShape(20.dp),
                    color = BralirwaCrimson,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${cartItems.sumOf { it.qty }} Crates • ${selectedCustomer?.name ?: "Walk-in"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = formatRwf(grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(color = LagerGold, shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Checkout", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMobileCartSheet) {
        ModalBottomSheet(onDismissRequest = { showMobileCartSheet = false }) {
            PosCartCheckoutPane(
                viewModel = viewModel,
                settings = settings,
                activeRole = activeRole,
                cartItems = cartItems,
                selectedCustomer = selectedCustomer,
                paymentMethod = paymentMethod,
                quickPayMode = quickPayMode,
                vatEnabled = vatEnabled,
                subtotal = subtotal,
                vatAmt = vatAmt,
                grandTotal = grandTotal,
                onOpenCustomerPicker = { showCustomerModal = true },
                onCheckoutDone = { showMobileCartSheet = false }
            )
        }
    }

    pickerProduct?.let { prod ->
        QuantityPickerModal(
            product = prod,
            canEditPrice = activeRole.canEditPrices(),
            onDismiss = { pickerProduct = null },
            onConfirm = { qty, rate ->
                viewModel.addToCart(prod, qty, rate)
                pickerProduct = null
            }
        )
    }

    if (showCustomerModal) {
        CustomerPickerDialog(
            customers = customers,
            selected = selectedCustomer,
            onSelect = {
                viewModel.selectCustomer(it)
                showCustomerModal = false
            },
            onCreateQuickCustomer = { name, phone, zone ->
                viewModel.saveCustomer(Customer(name = name, phone = phone, zone = zone))
            },
            onDismiss = { showCustomerModal = false }
        )
    }
}

@Composable
private fun PosCatalogPane(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    lastCompletedOrder: SalesOrder?,
    onPrintA4: () -> Unit,
    onPrintThermal: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 12.dp
) {
    val chips = listOf("ALL" to "All", "BEER" to "Beers", "SOFT_DRINK" to "Soft Drinks", "Lager" to "Lager", "Dark Stout" to "Stout", "Carbonated CSD" to "CSD")

    Column(modifier = Modifier.fillMaxSize()) {
        if (lastCompletedOrder != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                color = EmeraldSoft
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Last Invoice: ${lastCompletedOrder.invoiceNumber}", fontWeight = FontWeight.Bold, color = EmeraldFresh, style = MaterialTheme.typography.titleSmall)
                        Text(formatRwf(lastCompletedOrder.totalRwf), style = MaterialTheme.typography.bodySmall, color = EmeraldFresh)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = onPrintThermal, modifier = Modifier.height(40.dp).testTag("pos_print_thermal_btn")) {
                            Icon(Icons.Filled.Receipt, contentDescription = "Print Thermal", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Thermal")
                        }
                        Button(
                            onClick = onPrintA4,
                            modifier = Modifier.height(40.dp).testTag("pos_print_a4_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldFresh)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = "Print A4", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("A4")
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            placeholder = { Text("Search Primus, Mützig, Coca-Cola, Fanta...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("pos_search_input")
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips) { (key, label) ->
                FilterChip(
                    selected = selectedCategory == key,
                    onClick = { onSelectCategory(key) },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products, key = { it.id }) { prod ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductClick(prod) }
                        .testTag("pos_product_${prod.skuCode}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductThumbnail(prod.imageResKey, prod.imageUri, prod.name, size = 60.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${prod.subCategory} • ${prod.unitLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = if (prod.isLowStock) AmberAlertSoft else EmeraldSoft,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (prod.isLowStock) "${prod.stockQty} left • LOW" else "${prod.stockQty} crates",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (prod.isLowStock) AmberAlert else EmeraldFresh,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatRwf(prod.sellingPriceRwf), style = MaterialTheme.typography.titleSmall, color = BralirwaCrimson, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Filled.Add, contentDescription = "Add ${prod.name}", tint = BralirwaCrimson, modifier = Modifier.padding(6.dp).size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosCartCheckoutPane(
    viewModel: DistributorViewModel,
    settings: DistributorSettings,
    activeRole: StaffRole,
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    paymentMethod: PaymentMethod,
    quickPayMode: QuickPayMode,
    vatEnabled: Boolean,
    subtotal: Double,
    vatAmt: Double,
    grandTotal: Double,
    onOpenCustomerPicker: () -> Unit,
    onCheckoutDone: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Customer Card (Gradient when selected!)
        item {
            val selectedBg = Brush.horizontalGradient(listOf(BralirwaCrimson, BralirwaCrimsonDark, LagerGold))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .then(
                        if (selectedCustomer != null) Modifier.background(selectedBg)
                        else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    .clickable(onClick = onOpenCustomerPicker)
                    .padding(14.dp)
                    .testTag("pos_customer_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Customer",
                        tint = if (selectedCustomer != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedCustomer?.name ?: "Select Customer (Optional)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCustomer != null) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedCustomer != null) {
                                "${selectedCustomer.zone} • Credit: ${formatRwf(selectedCustomer.creditBalanceRwf)}"
                            } else "Tap to attach HORECA / Retail buyer",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedCustomer != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedCustomer != null) {
                        TextButton(onClick = { viewModel.selectCustomer(null) }) {
                            Text("Clear", color = Color.White)
                        }
                    }
                }
            }
        }

        // Cart Line Items with inline Qty + Rate edit
        if (cartItems.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Cart is empty. Tap any Bralirwa beer or soft drink to add crates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(cartItems, key = { it.product.id }) { ci ->
                CartLineRow(
                    item = ci,
                    canEditRate = activeRole.canEditPrices(),
                    onQtyChange = { viewModel.updateCartItemQty(ci.product.id, it) },
                    onRateChange = { viewModel.updateCartItemRate(ci.product.id, it) }
                )
            }
        }

        // Quick Pay Buttons (Full / Half / None)
        item {
            Text("Quick Pay Split", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                QuickPayMode.entries.forEach { mode ->
                    val selected = quickPayMode == mode
                    OutlinedButton(
                        onClick = { viewModel.selectQuickPayMode(mode) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) BralirwaCrimson else Color.Transparent,
                            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(mode.label.split(" ").first(), maxLines = 1)
                    }
                }
            }
        }

        // Payment Method Tiles with Icons
        item {
            Text("Payment Method", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PaymentMethod.entries) { method ->
                    val selected = paymentMethod == method
                    val icon: ImageVector = when (method) {
                        PaymentMethod.CASH -> Icons.Filled.Payments
                        PaymentMethod.MOMO -> Icons.Filled.PhoneAndroid
                        PaymentMethod.AIRTEL -> Icons.Filled.PhoneAndroid
                        PaymentMethod.BANK -> Icons.Filled.AccountBalance
                        PaymentMethod.CREDIT -> Icons.Filled.CreditCard
                    }
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.selectPaymentMethod(method) }
                            .testTag("pay_method_${method.code}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, contentDescription = method.label, tint = if (selected) BralirwaCrimson else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(method.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Totals + VAT toggle + Complete Sale CTA
        item {
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Include VAT (${settings.vatRate}%)", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = vatEnabled, onCheckedChange = { viewModel.toggleVat(it) })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatRwf(subtotal), fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VAT", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatRwf(vatAmt), fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pay Now (${quickPayMode.label})", color = EmeraldFresh, fontWeight = FontWeight.SemiBold)
                Text(formatRwf(grandTotal * quickPayMode.ratio), color = EmeraldFresh, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("GRAND TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(formatRwf(grandTotal), style = MaterialTheme.typography.titleLarge, color = BralirwaCrimson, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    viewModel.checkoutPos()
                    onCheckoutDone()
                },
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pos_complete_sale_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
            ) {
                Text("Complete Sale & Generate Invoice", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartLineRow(
    item: CartItem,
    canEditRate: Boolean,
    onQtyChange: (Int) -> Unit,
    onRateChange: (Double) -> Unit
) {
    var rateText by remember(item.product.id, item.customRateRwf) { mutableStateOf(item.customRateRwf.toInt().toString()) }
    var qtyText by remember(item.product.id, item.qty) { mutableStateOf(item.qty.toString()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(item.product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(formatRwf(item.lineTotalRwf), style = MaterialTheme.typography.titleSmall, color = BralirwaCrimson, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onQtyChange(0) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove item", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onQtyChange(item.qty - 1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease qty")
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        qtyText = digits
                        digits.toIntOrNull()?.let { if (it > 0) onQtyChange(it) }
                    },
                    label = { Text("Qty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(76.dp)
                )
                IconButton(onClick = { onQtyChange(item.qty + 1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase qty")
                }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        rateText = digits
                        digits.toDoubleOrNull()?.let(onRateChange)
                    },
                    enabled = canEditRate,
                    label = { Text("Rate") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuantityPickerModal(
    product: Product,
    canEditPrice: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double) -> Unit
) {
    var qty by remember { mutableStateOf("5") }
    var rate by remember { mutableStateOf(product.sellingPriceRwf.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${product.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${product.unitLabel} • Available: ${product.stockQty} crates", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 5, 10, 25, 50).forEach { preset ->
                        Surface(
                            modifier = Modifier.clickable { qty = preset.toString() },
                            shape = RoundedCornerShape(10.dp),
                            color = if (qty == preset.toString()) BralirwaCrimson else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "+$preset",
                                color = if (qty == preset.toString()) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it.filter { c -> c.isDigit() } },
                    label = { Text("Crate Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("qty_modal_input")
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it.filter { c -> c.isDigit() } },
                    enabled = canEditPrice,
                    label = { Text("Unit Price (RWF)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qty.toIntOrNull() ?: 1
                    val r = rate.toDoubleOrNull() ?: product.sellingPriceRwf
                    onConfirm(q.coerceAtLeast(1), r)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson),
                modifier = Modifier.testTag("qty_modal_confirm_btn")
            ) { Text("Add to Cart") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CustomerPickerDialog(
    customers: List<Customer>,
    selected: Customer?,
    onSelect: (Customer) -> Unit,
    onCreateQuickCustomer: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var addingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newZone by remember { mutableStateOf("Kigali CBD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (addingNew) "New Distributor Customer" else "Select Customer", fontWeight = FontWeight.Bold) },
        text = {
            if (addingNew) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Customer / Bar / Hotel Name") })
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Phone") })
                    OutlinedTextField(value = newZone, onValueChange = { newZone = it }, label = { Text("Route / Zone") })
                }
            } else {
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customers, key = { it.id }) { cust ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(cust) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected?.id == cust.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(cust.name, fontWeight = FontWeight.Bold)
                                Text("${cust.zone} • Spent: ${formatRwf(cust.totalSpentRwf)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (addingNew) {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onCreateQuickCustomer(newName, newPhone, newZone)
                        addingNew = false
                    }
                }) { Text("Save Customer") }
            } else {
                TextButton(onClick = { addingNew = true }) { Text("+ Add New Customer") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
