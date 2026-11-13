package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DistributorSettings
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ReplenishmentOrder
import com.example.data.model.StaffRole
import com.example.data.model.StockAgingBucket
import com.example.ui.components.ProductThumbnail
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.BralirwaCrimson
import com.example.ui.theme.CrimsonCritical
import com.example.ui.theme.CrimsonCriticalSoft
import com.example.ui.theme.EmeraldFresh
import com.example.ui.theme.EmeraldSoft
import com.example.ui.viewmodel.DistributorViewModel
import com.example.util.buildBralirwaReplenishmentEmailBody
import com.example.util.buildProductsCsv
import com.example.util.dispatchEmailIntent
import com.example.util.formatDateTime
import com.example.util.formatRwf
import com.example.util.sharePlainTextOrCsv

@Composable
fun InventoryScreen(
    viewModel: DistributorViewModel,
    settings: DistributorSettings,
    products: List<Product>,
    replenishments: List<ReplenishmentOrder>
) {
    val context = LocalContext.current
    val activeRole = StaffRole.fromCode(settings.activeStaffRole)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Inventory", "Drilldown", "Stock Aging", "Bralirwa PO")

    var searchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showAddSkuDialog by remember { mutableStateOf(false) }
    var showCsvImportModal by remember { mutableStateOf(false) }
    var photoTargetProduct by remember { mutableStateOf<Product?>(null) }

    // Zero-permission Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val target = photoTargetProduct
        if (uri != null && target != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Non-persistable URI is still usable for current session
            }
            viewModel.updateProductImageUri(target, uri.toString())
        }
        photoTargetProduct = null
    }

    // Zero-permission SAF CSV picker
    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                viewModel.importProductsCsv(content)
            }
        }
    }

    val lowStock = remember(products) { products.filter { it.isLowStock } }
    val filtered = remember(products, searchQuery) {
        products.filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.skuCode.contains(searchQuery, ignoreCase = true) ||
                it.subCategory.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showCsvImportModal = true },
                                modifier = Modifier.weight(1f).height(46.dp).testTag("inv_csv_import_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Bulk CSV", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    sharePlainTextOrCsv(context, "Bralirwa Inventory CSV", buildProductsCsv(products))
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Export", maxLines = 1)
                            }
                            Button(
                                onClick = { showAddSkuDialog = true },
                                modifier = Modifier.weight(1f).height(46.dp).testTag("inv_add_sku_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New SKU", maxLines = 1)
                            }
                        }
                    }

                    if (lowStock.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CrimsonCriticalSoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = CrimsonCritical)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${lowStock.size} SKUs below reorder alert", fontWeight = FontWeight.Bold, color = CrimsonCritical)
                                        Text("Tap Order to send automated PO email to Bralirwa", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7F1D1D))
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.orderReplenishmentAtBralirwa(context, lowStock, true)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson),
                                        modifier = Modifier.testTag("inv_auto_replenish_btn")
                                    ) {
                                        Text("Order")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                            placeholder = { Text("Filter SKU, beer, or soft drink...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(filtered, key = { it.id }) { prod ->
                        InventoryDataRow(
                            product = prod,
                            onPickPhoto = {
                                photoTargetProduct = prod
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onShareToDrive = {
                                if (prod.imageUri.isNotBlank()) {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(prod.imageUri))
                                        putExtra(Intent.EXTRA_SUBJECT, "${prod.skuCode} - ${prod.name}")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Upload / Save Image to Drive"))
                                } else {
                                    sharePlainTextOrCsv(
                                        context,
                                        "Upload SKU Spec to Drive: ${prod.name}",
                                        "SKU: ${prod.skuCode}\nName: ${prod.name}\nStock: ${prod.stockQty} ${prod.unitLabel}\nSelling: ${formatRwf(prod.sellingPriceRwf)}",
                                        "text/plain"
                                    )
                                }
                            },
                            onEdit = { editingProduct = prod }
                        )
                    }
                }
            }

            // TAB 1: SUB-CATEGORY DRILLDOWN
            1 -> {
                SubCategoryDrilldownView(products)
            }

            // TAB 2: STOCK AGING REPORT
            2 -> {
                StockAgingView(products)
            }

            // TAB 3: BRALIRWA REPLENISHMENT ORDERS
            3 -> {
                BralirwaReplenishmentTab(
                    viewModel = viewModel,
                    settings = settings,
                    products = products,
                    replenishments = replenishments,
                    activeRole = activeRole
                )
            }
        }
    }

    if (showAddSkuDialog || editingProduct != null) {
        ProductEditorDialog(
            initial = editingProduct,
            onDismiss = {
                showAddSkuDialog = false
                editingProduct = null
            },
            onSave = { saved ->
                viewModel.saveProduct(saved)
                showAddSkuDialog = false
                editingProduct = null
            }
        )
    }

    if (showCsvImportModal) {
        CsvBulkImportDialog(
            onDismiss = { showCsvImportModal = false },
            onPickCsvFile = { csvFileLauncher.launch(arrayOf("text/*", "application/csv")) },
            onImportText = { csvText ->
                viewModel.importProductsCsv(csvText)
                showCsvImportModal = false
            }
        )
    }
}

@Composable
private fun InventoryDataRow(
    product: Product,
    onPickPhoto: () -> Unit,
    onShareToDrive: () -> Unit,
    onEdit: () -> Unit
) {
    val ratio = (product.stockQty.toFloat() / (product.reorderLevel * 3).coerceAtLeast(1)).coerceIn(0f, 1f)
    val barColor = when {
        product.isCriticalStock -> CrimsonCritical
        product.isLowStock -> AmberAlert
        else -> EmeraldFresh
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductThumbnail(product.imageResKey, product.imageUri, product.name, size = 56.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(product.skuCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${product.category} • ${product.subCategory} • ${product.unitLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Cost: ${formatRwf(product.costPriceRwf)}", style = MaterialTheme.typography.labelSmall)
                        Text("Sell: ${formatRwf(product.sellingPriceRwf)}", style = MaterialTheme.typography.labelSmall, color = BralirwaCrimson, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onPickPhoto) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Pick Product Photo", tint = BralirwaCrimson)
                }
                IconButton(onClick = onShareToDrive) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = "Upload to Drive", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { ratio },
                    color = barColor,
                    trackColor = barColor.copy(alpha = 0.16f),
                    modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(50))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${product.stockQty} / min ${product.reorderLevel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SubCategoryDrilldownView(products: List<Product>) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedSubCat by remember { mutableStateOf<String?>(null) }

    val subCategories = remember(products, selectedCategory) {
        products
            .filter { selectedCategory == "ALL" || it.category == selectedCategory }
            .groupBy { it.subCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ALL" to "All Categories", "BEER" to "Beers", "SOFT_DRINK" to "Soft Drinks")) { (code, label) ->
                    FilterChip(
                        selected = selectedCategory == code,
                        onClick = {
                            selectedCategory = code
                            selectedSubCat = null
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
        subCategories.forEach { (subCat, list) ->
            item(key = "sub_$subCat") {
                val expanded = selectedSubCat == subCat
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedSubCat = if (expanded) null else subCat
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(subCat, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${list.size} SKUs • ${list.sumOf { it.stockQty }} total crates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatRwf(list.sumOf { it.stockValueCostRwf }), style = MaterialTheme.typography.titleSmall, color = BralirwaCrimson, fontWeight = FontWeight.Bold)
                        }
                        if (expanded) {
                            Spacer(Modifier.height(10.dp))
                            list.forEach { p ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• ${p.name}", style = MaterialTheme.typography.bodyMedium)
                                    Text("${p.stockQty} crates", fontWeight = FontWeight.Bold, color = if (p.isLowStock) CrimsonCritical else EmeraldFresh)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockAgingView(products: List<Product>) {
    val buckets = remember(products) {
        products.groupBy { StockAgingBucket.fromBatchEpoch(it.batchReceivedEpochMs) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StockAgingBucket.entries.forEach { b ->
                    val count = buckets[b]?.sumOf { it.stockQty } ?: 0
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(b.colorHex).copy(alpha = 0.14f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(b.label.split(" ").first(), style = MaterialTheme.typography.labelSmall, color = Color(b.colorHex), fontWeight = FontWeight.Bold)
                            Text("$count c", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(products.sortedBy { it.batchReceivedEpochMs }, key = { "age_${it.id}" }) { p ->
            val days = StockAgingBucket.daysOld(p.batchReceivedEpochMs)
            val bucket = StockAgingBucket.fromBatchEpoch(p.batchReceivedEpochMs)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Color(bucket.colorHex)))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Bold)
                        Text("${p.stockQty} crates • Capital: ${formatRwf(p.stockValueCostRwf)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = Color(bucket.colorHex).copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                        Text("${days}d old", style = MaterialTheme.typography.labelSmall, color = Color(bucket.colorHex), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BralirwaReplenishmentTab(
    viewModel: DistributorViewModel,
    settings: DistributorSettings,
    products: List<Product>,
    replenishments: List<ReplenishmentOrder>,
    activeRole: StaffRole
) {
    val context = LocalContext.current
    val lowStock = remember(products) { products.filter { it.isLowStock } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Automated Bralirwa Stock Command", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BralirwaCrimson)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${lowStock.size} SKUs are below reorder threshold. Depot target: ${settings.bralirwaEmail}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.orderReplenishmentAtBralirwa(
                                context = context,
                                targetProducts = lowStock.ifEmpty { products.take(4) },
                                sendEmailNow = true
                            )
                        },
                        enabled = activeRole.canManageReplenishment(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("bralirwa_dispatch_po_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Command Replenishment at Bralirwa + Email")
                    }
                }
            }
        }

        items(replenishments, key = { it.id }) { po ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(po.poNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            color = if (po.status == "DELIVERED") EmeraldSoft else AmberAlertSoft,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = po.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (po.status == "DELIVERED") EmeraldFresh else AmberAlert,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(po.itemsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${po.totalCrates} crates • ${formatRwf(po.totalCostRwf)}", fontWeight = FontWeight.Bold, color = BralirwaCrimson)
                        Text(formatDateTime(po.createdAt?.toDate()?.time ?: 0L), style = MaterialTheme.typography.labelSmall)
                    }
                    if (po.status != "DELIVERED" && po.status != "CANCELLED") {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    dispatchEmailIntent(
                                        context,
                                        po.supplierEmail,
                                        "BRALIRWA Replenishment ${po.poNumber}",
                                        buildBralirwaReplenishmentEmailBody(po, settings)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Re-Email")
                            }
                            Button(
                                onClick = { viewModel.markPoDelivered(po) },
                                modifier = Modifier.weight(1f).testTag("mark_po_delivered_${po.poNumber}"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldFresh)
                            ) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Receive Stock")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var sku by remember { mutableStateOf(initial?.skuCode ?: "BRW-NEW-01") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: ProductCategory.BEER.code) }
    var subCategory by remember { mutableStateOf(initial?.subCategory ?: "Lager") }
    var unitLabel by remember { mutableStateOf(initial?.unitLabel ?: "Crate x24") }
    var cost by remember { mutableStateOf((initial?.costPriceRwf ?: 9000.0).toInt().toString()) }
    var sell by remember { mutableStateOf((initial?.sellingPriceRwf ?: 10500.0).toInt().toString()) }
    var stock by remember { mutableStateOf((initial?.stockQty ?: 40).toString()) }
    var reorder by remember { mutableStateOf((initial?.reorderLevel ?: 20).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Bralirwa SKU" else "Edit ${initial.name}", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU Code") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = category == "BEER", onClick = { category = "BEER" }, label = { Text("Beer") })
                        FilterChip(selected = category == "SOFT_DRINK", onClick = { category = "SOFT_DRINK" }, label = { Text("Soft Drink") })
                    }
                }
                item { OutlinedTextField(value = subCategory, onValueChange = { subCategory = it }, label = { Text("Sub-Category (Lager, CSD...)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = unitLabel, onValueChange = { unitLabel = it }, label = { Text("Unit (Crate x12 / x24)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = cost, onValueChange = { cost = it.filter { c -> c.isDigit() } }, label = { Text("Cost Price (RWF)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = sell, onValueChange = { sell = it.filter { c -> c.isDigit() } }, label = { Text("Selling Price (RWF)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() } }, label = { Text("Stock Crates") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = reorder, onValueChange = { reorder = it.filter { c -> c.isDigit() } }, label = { Text("Reorder Alert Threshold") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: Product()).copy(
                                skuCode = sku,
                                name = name,
                                category = category,
                                subCategory = subCategory,
                                unitLabel = unitLabel,
                                costPriceRwf = cost.toDoubleOrNull() ?: 0.0,
                                sellingPriceRwf = sell.toDoubleOrNull() ?: 0.0,
                                stockQty = stock.toIntOrNull() ?: 0,
                                reorderLevel = reorder.toIntOrNull() ?: 20,
                                imageResKey = if (category == "BEER") "beer_crate" else "soft_drinks"
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
            ) { Text("Save SKU") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CsvBulkImportDialog(
    onDismiss: () -> Unit,
    onPickCsvFile: () -> Unit,
    onImportText: (String) -> Unit
) {
    var csvText by remember {
        mutableStateOf(
            "sku,name,category,sub_category,unit,cost,selling,stock,reorder\nBRW-KIS-33,Mutzig Class 33cl,BEER,Premium Lager,Crate x24,14200,16400,45,20\nBRW-VTP-150,Vital'O PET 1.5L,SOFT_DRINK,PET Pack,Pack x6,5400,6500,60,25"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk CSV Import", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onPickCsvFile, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pick .CSV File from Device")
                }
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("Or Paste CSV Rows") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImportText(csvText) },
                colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
            ) { Text("Import Rows") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
