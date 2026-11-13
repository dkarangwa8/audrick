package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ActivityLog
import com.example.data.model.Customer
import com.example.data.model.DistributorSettings
import com.example.data.model.ExpenseRecord
import com.example.data.model.OrderLineItem
import com.example.data.model.Product
import com.example.data.model.ReplenishmentOrder
import com.example.data.model.SalesOrder
import com.example.data.model.ShipmentRecord
import com.example.data.model.StaffProfile
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "cached_products")
data class CachedProductEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val skuCode: String,
    val name: String,
    val category: String,
    val subCategory: String,
    val unitLabel: String,
    val costPriceRwf: Double,
    val sellingPriceRwf: Double,
    val stockQty: Int,
    val reorderLevel: Int,
    val batchReceivedEpochMs: Long,
    val imageResKey: String,
    val imageUri: String,
    val updatedEpochMs: Long
) {
    fun toDomain(): Product = Product(
        id = id,
        userId = userId,
        skuCode = skuCode,
        name = name,
        category = category,
        subCategory = subCategory,
        unitLabel = unitLabel,
        costPriceRwf = costPriceRwf,
        sellingPriceRwf = sellingPriceRwf,
        stockQty = stockQty,
        reorderLevel = reorderLevel,
        batchReceivedEpochMs = batchReceivedEpochMs,
        imageResKey = imageResKey,
        imageUri = imageUri,
        createdAt = Timestamp(updatedEpochMs / 1000, 0),
        updatedAt = Timestamp(updatedEpochMs / 1000, 0)
    )
}

fun Product.toCacheEntity(): CachedProductEntity = CachedProductEntity(
    id = id,
    userId = userId,
    skuCode = skuCode,
    name = name,
    category = category,
    subCategory = subCategory,
    unitLabel = unitLabel,
    costPriceRwf = costPriceRwf,
    sellingPriceRwf = sellingPriceRwf,
    stockQty = stockQty,
    reorderLevel = reorderLevel,
    batchReceivedEpochMs = batchReceivedEpochMs,
    imageResKey = imageResKey,
    imageUri = imageUri,
    updatedEpochMs = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
)

@Entity(tableName = "cached_customers")
data class CachedCustomerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val email: String,
    val tin: String,
    val zone: String,
    val totalSpentRwf: Double,
    val creditBalanceRwf: Double,
    val billingCycle: String,
    val nextBillingEpochMs: Long
) {
    fun toDomain(): Customer = Customer(
        id = id,
        userId = userId,
        name = name,
        phone = phone,
        email = email,
        tin = tin,
        zone = zone,
        totalSpentRwf = totalSpentRwf,
        creditBalanceRwf = creditBalanceRwf,
        billingCycle = billingCycle,
        nextBillingEpochMs = nextBillingEpochMs
    )
}

fun Customer.toCacheEntity(): CachedCustomerEntity = CachedCustomerEntity(
    id = id,
    userId = userId,
    name = name,
    phone = phone,
    email = email,
    tin = tin,
    zone = zone,
    totalSpentRwf = totalSpentRwf,
    creditBalanceRwf = creditBalanceRwf,
    billingCycle = billingCycle,
    nextBillingEpochMs = nextBillingEpochMs
)

@Entity(tableName = "cached_orders")
data class CachedOrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val invoiceNumber: String,
    val customerId: String,
    val customerName: String,
    val staffName: String,
    val staffRole: String,
    val itemsSerialized: String,
    val itemsSummary: String,
    val totalCrates: Int,
    val subtotalRwf: Double,
    val vatRwf: Double,
    val totalRwf: Double,
    val paidRwf: Double,
    val balanceRwf: Double,
    val paymentMethod: String,
    val status: String,
    val vehiclePlate: String,
    val createdEpochMs: Long
) {
    fun toDomain(): SalesOrder = SalesOrder(
        id = id,
        userId = userId,
        invoiceNumber = invoiceNumber,
        customerId = customerId,
        customerName = customerName,
        staffName = staffName,
        staffRole = staffRole,
        items = deserializeLineItems(itemsSerialized),
        itemsSummary = itemsSummary,
        totalCrates = totalCrates,
        subtotalRwf = subtotalRwf,
        vatRwf = vatRwf,
        totalRwf = totalRwf,
        paidRwf = paidRwf,
        balanceRwf = balanceRwf,
        paymentMethod = paymentMethod,
        status = status,
        vehiclePlate = vehiclePlate,
        createdAt = Timestamp(createdEpochMs / 1000, 0),
        updatedAt = Timestamp(createdEpochMs / 1000, 0)
    )
}

fun SalesOrder.toCacheEntity(): CachedOrderEntity = CachedOrderEntity(
    id = id,
    userId = userId,
    invoiceNumber = invoiceNumber,
    customerId = customerId,
    customerName = customerName,
    staffName = staffName,
    staffRole = staffRole,
    itemsSerialized = serializeLineItems(items),
    itemsSummary = itemsSummary,
    totalCrates = totalCrates,
    subtotalRwf = subtotalRwf,
    vatRwf = vatRwf,
    totalRwf = totalRwf,
    paidRwf = paidRwf,
    balanceRwf = balanceRwf,
    paymentMethod = paymentMethod,
    status = status,
    vehiclePlate = vehiclePlate,
    createdEpochMs = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

@Entity(tableName = "cached_replenishments")
data class CachedReplenishmentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val poNumber: String,
    val supplierName: String,
    val supplierEmail: String,
    val itemsSerialized: String,
    val itemsSummary: String,
    val totalCrates: Int,
    val totalCostRwf: Double,
    val status: String,
    val triggeredByAlert: Boolean,
    val createdEpochMs: Long
) {
    fun toDomain(): ReplenishmentOrder = ReplenishmentOrder(
        id = id,
        userId = userId,
        poNumber = poNumber,
        supplierName = supplierName,
        supplierEmail = supplierEmail,
        items = deserializeLineItems(itemsSerialized),
        itemsSummary = itemsSummary,
        totalCrates = totalCrates,
        totalCostRwf = totalCostRwf,
        status = status,
        triggeredByAlert = triggeredByAlert,
        createdAt = Timestamp(createdEpochMs / 1000, 0),
        updatedAt = Timestamp(createdEpochMs / 1000, 0)
    )
}

fun ReplenishmentOrder.toCacheEntity(): CachedReplenishmentEntity = CachedReplenishmentEntity(
    id = id,
    userId = userId,
    poNumber = poNumber,
    supplierName = supplierName,
    supplierEmail = supplierEmail,
    itemsSerialized = serializeLineItems(items),
    itemsSummary = itemsSummary,
    totalCrates = totalCrates,
    totalCostRwf = totalCostRwf,
    status = status,
    triggeredByAlert = triggeredByAlert,
    createdEpochMs = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

@Entity(tableName = "cached_expenses")
data class CachedExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val description: String,
    val amountRwf: Double,
    val vehiclePlate: String,
    val createdEpochMs: Long
) {
    fun toDomain(): ExpenseRecord = ExpenseRecord(
        id = id,
        userId = userId,
        category = category,
        description = description,
        amountRwf = amountRwf,
        vehiclePlate = vehiclePlate,
        createdAt = Timestamp(createdEpochMs / 1000, 0),
        updatedAt = Timestamp(createdEpochMs / 1000, 0)
    )
}

fun ExpenseRecord.toCacheEntity(): CachedExpenseEntity = CachedExpenseEntity(
    id = id,
    userId = userId,
    category = category,
    description = description,
    amountRwf = amountRwf,
    vehiclePlate = vehiclePlate,
    createdEpochMs = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

@Entity(tableName = "cached_shipments")
data class CachedShipmentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val vehiclePlate: String,
    val driverName: String,
    val routeZone: String,
    val cratesLoaded: Int,
    val revenueRwf: Double,
    val cogsRwf: Double,
    val fuelAndTripCostRwf: Double,
    val status: String,
    val createdEpochMs: Long
) {
    fun toDomain(): ShipmentRecord = ShipmentRecord(
        id = id,
        userId = userId,
        vehiclePlate = vehiclePlate,
        driverName = driverName,
        routeZone = routeZone,
        cratesLoaded = cratesLoaded,
        revenueRwf = revenueRwf,
        cogsRwf = cogsRwf,
        fuelAndTripCostRwf = fuelAndTripCostRwf,
        status = status,
        createdAt = Timestamp(createdEpochMs / 1000, 0),
        updatedAt = Timestamp(createdEpochMs / 1000, 0)
    )
}

fun ShipmentRecord.toCacheEntity(): CachedShipmentEntity = CachedShipmentEntity(
    id = id,
    userId = userId,
    vehiclePlate = vehiclePlate,
    driverName = driverName,
    routeZone = routeZone,
    cratesLoaded = cratesLoaded,
    revenueRwf = revenueRwf,
    cogsRwf = cogsRwf,
    fuelAndTripCostRwf = fuelAndTripCostRwf,
    status = status,
    createdEpochMs = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

@Entity(tableName = "cached_activities")
data class CachedActivityEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val detail: String,
    val amountRwf: Double,
    val actorRole: String,
    val createdEpochMs: Long
) {
    fun toDomain(): ActivityLog = ActivityLog(
        id = id,
        userId = userId,
        type = type,
        title = title,
        detail = detail,
        amountRwf = amountRwf,
        actorRole = actorRole,
        createdAt = Timestamp(createdEpochMs / 1000, 0),
        updatedAt = Timestamp(createdEpochMs / 1000, 0)
    )
}

fun ActivityLog.toCacheEntity(): CachedActivityEntity = CachedActivityEntity(
    id = id,
    userId = userId,
    type = type,
    title = title,
    detail = detail,
    amountRwf = amountRwf,
    actorRole = actorRole,
    createdEpochMs = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

@Entity(tableName = "cached_settings")
data class CachedSettingsEntity(
    @PrimaryKey val userId: String,
    val businessName: String,
    val address: String,
    val phone: String,
    val email: String,
    val tin: String,
    val bralirwaEmail: String,
    val vatRate: Double,
    val lowStockAutoEmail: Boolean,
    val recurringBillingEnabled: Boolean,
    val thermalPaperMm: Int,
    val activeStaffRole: String,
    val activeStaffName: String
) {
    fun toDomain(): DistributorSettings = DistributorSettings(
        id = userId,
        userId = userId,
        businessName = businessName,
        address = address,
        phone = phone,
        email = email,
        tin = tin,
        bralirwaEmail = bralirwaEmail,
        vatRate = vatRate,
        lowStockAutoEmail = lowStockAutoEmail,
        recurringBillingEnabled = recurringBillingEnabled,
        thermalPaperMm = thermalPaperMm,
        activeStaffRole = activeStaffRole,
        activeStaffName = activeStaffName
    )
}

fun DistributorSettings.toCacheEntity(): CachedSettingsEntity = CachedSettingsEntity(
    userId = userId,
    businessName = businessName,
    address = address,
    phone = phone,
    email = email,
    tin = tin,
    bralirwaEmail = bralirwaEmail,
    vatRate = vatRate,
    lowStockAutoEmail = lowStockAutoEmail,
    recurringBillingEnabled = recurringBillingEnabled,
    thermalPaperMm = thermalPaperMm,
    activeStaffRole = activeStaffRole,
    activeStaffName = activeStaffName
)

@Entity(tableName = "cached_staff")
data class CachedStaffEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val role: String,
    val pinHash: String,
    val active: Boolean
) {
    fun toDomain(): StaffProfile = StaffProfile(
        id = id,
        userId = userId,
        fullName = fullName,
        email = email,
        role = role,
        pinHash = pinHash,
        active = active
    )
}

fun StaffProfile.toCacheEntity(): CachedStaffEntity = CachedStaffEntity(
    id = id,
    userId = userId,
    fullName = fullName,
    email = email,
    role = role,
    pinHash = pinHash,
    active = active
)

private fun serializeLineItems(items: List<OrderLineItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        array.put(JSONObject().apply {
            put("productId", item.productId)
            put("skuCode", item.skuCode)
            put("name", item.name)
            put("qty", item.qty)
            put("rateRwf", item.rateRwf)
            put("costRateRwf", item.costRateRwf)
            put("lineTotalRwf", item.lineTotalRwf)
        })
    }
    return array.toString()
}

private fun deserializeLineItems(raw: String): List<OrderLineItem> {
    if (raw.isBlank()) return emptyList()
    return try {
        val array = JSONArray(raw)
        List(array.length()) { idx ->
            val obj = array.getJSONObject(idx)
            OrderLineItem(
                productId = obj.optString("productId"),
                skuCode = obj.optString("skuCode"),
                name = obj.optString("name"),
                qty = obj.optInt("qty"),
                rateRwf = obj.optDouble("rateRwf"),
                costRateRwf = obj.optDouble("costRateRwf"),
                lineTotalRwf = obj.optDouble("lineTotalRwf")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
