package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class StaffRole(
    val code: String,
    val label: String,
    val description: String
) {
    ADMIN("ADMIN", "Admin / Owner", "Full POS, Inventory, Reports, Staff & Settings"),
    MANAGER("MANAGER", "Depot Manager", "Dashboard, POS, Inventory, Replenishment & Reports"),
    CASHIER("CASHIER", "POS Cashier", "POS Checkout, Customers & Receipts"),
    WAREHOUSE("WAREHOUSE", "Warehouse Clerk", "Inventory, Aging, Replenishment & Fleet Loading");

    fun canAccessPos(): Boolean = this in listOf(ADMIN, MANAGER, CASHIER)
    fun canAccessInventory(): Boolean = this in listOf(ADMIN, MANAGER, WAREHOUSE)
    fun canManageReplenishment(): Boolean = this in listOf(ADMIN, MANAGER, WAREHOUSE)
    fun canViewFinancialReports(): Boolean = this in listOf(ADMIN, MANAGER)
    fun canAccessSettings(): Boolean = this == ADMIN
    fun canEditPrices(): Boolean = this in listOf(ADMIN, MANAGER)

    companion object {
        fun fromCode(code: String): StaffRole =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ADMIN
    }
}

enum class ProductCategory(val code: String, val label: String) {
    BEER("BEER", "Beers"),
    SOFT_DRINK("SOFT_DRINK", "Soft Drinks");

    companion object {
        fun fromCode(code: String): ProductCategory =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: BEER
    }
}

enum class PaymentMethod(val code: String, val label: String) {
    CASH("CASH", "Cash"),
    MOMO("MOMO", "MTN MoMo"),
    AIRTEL("AIRTEL", "Airtel Money"),
    BANK("BANK", "Bank Transfer"),
    CREDIT("CREDIT", "Credit");

    companion object {
        fun fromCode(code: String): PaymentMethod =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CASH
    }
}

enum class QuickPayMode(val label: String, val ratio: Double) {
    FULL("Full (100%)", 1.0),
    HALF("Half (50%)", 0.5),
    NONE("Credit (0%)", 0.0)
}

enum class StockAgingBucket(val label: String, val colorHex: Long) {
    FRESH("0-15d Fresh", 0xFF177245),
    NORMAL("16-45d Normal", 0xFFC88A1E),
    SLOW("46-90d Slow", 0xFFD97706),
    CRITICAL("90d+ Aged", 0xFFDC2626);

    companion object {
        fun fromBatchEpoch(batchReceivedEpochMs: Long, nowMs: Long = System.currentTimeMillis()): StockAgingBucket {
            val days = ((nowMs - batchReceivedEpochMs).coerceAtLeast(0L)) / (24L * 60L * 60L * 1000L)
            return when {
                days <= 15 -> FRESH
                days <= 45 -> NORMAL
                days <= 90 -> SLOW
                else -> CRITICAL
            }
        }

        fun daysOld(batchReceivedEpochMs: Long, nowMs: Long = System.currentTimeMillis()): Int {
            return (((nowMs - batchReceivedEpochMs).coerceAtLeast(0L)) / (24L * 60L * 60L * 1000L)).toInt()
        }
    }
}

@IgnoreExtraProperties
data class DistributorSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessName: String = "Kigali Prime Bralirwa Depot",
    val address: String = "KN 14 Ave, Nyarugenge, Kigali, Rwanda",
    val phone: String = "+250 788 450 210",
    val email: String = "depot@kigaliprime.rw",
    val tin: String = "102948571",
    val bralirwaEmail: String = "orders@bralirwa.co.rw",
    val vatRate: Double = 18.0,
    val lowStockAutoEmail: Boolean = true,
    val recurringBillingEnabled: Boolean = true,
    val thermalPaperMm: Int = 80,
    val activeStaffRole: String = StaffRole.ADMIN.code,
    val activeStaffName: String = "Depot Admin",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val skuCode: String = "",
    val name: String = "",
    val category: String = ProductCategory.BEER.code,
    val subCategory: String = "Lager",
    val unitLabel: String = "Crate x12",
    val costPriceRwf: Double = 0.0,
    val sellingPriceRwf: Double = 0.0,
    val stockQty: Int = 0,
    val reorderLevel: Int = 20,
    val batchReceivedEpochMs: Long = System.currentTimeMillis(),
    val imageResKey: String = "beer_crate",
    val imageUri: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isLowStock: Boolean get() = stockQty <= reorderLevel
    val isCriticalStock: Boolean get() = stockQty <= (reorderLevel / 2).coerceAtLeast(5)
    val stockValueCostRwf: Double get() = stockQty * costPriceRwf
    val marginPercent: Double
        get() = if (sellingPriceRwf > 0) ((sellingPriceRwf - costPriceRwf) / sellingPriceRwf) * 100.0 else 0.0
}

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val tin: String = "",
    val zone: String = "Kigali CBD",
    val totalSpentRwf: Double = 0.0,
    val creditBalanceRwf: Double = 0.0,
    val billingCycle: String = "NONE",
    val nextBillingEpochMs: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val skuCode: String = "",
    val name: String = "",
    val qty: Int = 0,
    val rateRwf: Double = 0.0,
    val costRateRwf: Double = 0.0,
    val lineTotalRwf: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "skuCode" to skuCode,
        "name" to name,
        "qty" to qty,
        "rateRwf" to rateRwf,
        "costRateRwf" to costRateRwf,
        "lineTotalRwf" to lineTotalRwf
    )
}

@IgnoreExtraProperties
data class SalesOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val customerId: String = "",
    val customerName: String = "Walk-in Customer",
    val staffName: String = "",
    val staffRole: String = StaffRole.ADMIN.code,
    val items: List<OrderLineItem> = emptyList(),
    val itemsSummary: String = "",
    val totalCrates: Int = 0,
    val subtotalRwf: Double = 0.0,
    val vatRwf: Double = 0.0,
    val totalRwf: Double = 0.0,
    val paidRwf: Double = 0.0,
    val balanceRwf: Double = 0.0,
    val paymentMethod: String = PaymentMethod.CASH.code,
    val status: String = "PAID",
    val vehiclePlate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReplenishmentOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val poNumber: String = "",
    val supplierName: String = "BRALIRWA PLC — Kigali / Gisenyi Depot",
    val supplierEmail: String = "orders@bralirwa.co.rw",
    val items: List<OrderLineItem> = emptyList(),
    val itemsSummary: String = "",
    val totalCrates: Int = 0,
    val totalCostRwf: Double = 0.0,
    val status: String = "DRAFT",
    val triggeredByAlert: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpenseRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "FUEL_FLEET",
    val description: String = "",
    val amountRwf: Double = 0.0,
    val vehiclePlate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShipmentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vehiclePlate: String = "",
    val driverName: String = "",
    val routeZone: String = "",
    val cratesLoaded: Int = 0,
    val revenueRwf: Double = 0.0,
    val cogsRwf: Double = 0.0,
    val fuelAndTripCostRwf: Double = 0.0,
    val status: String = "ON_ROUTE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val netProfitRwf: Double get() = revenueRwf - cogsRwf - fuelAndTripCostRwf
}

@IgnoreExtraProperties
data class StaffProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = StaffRole.CASHIER.code,
    val pinHash: String = "1234",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OtpResetRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val otpCode: String = "",
    val expiresAtMillis: Long = 0L,
    val used: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "SALE",
    val title: String = "",
    val detail: String = "",
    val amountRwf: Double = 0.0,
    val actorRole: String = StaffRole.ADMIN.code,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val product: Product,
    val qty: Int,
    val customRateRwf: Double = product.sellingPriceRwf
) {
    val lineTotalRwf: Double get() = qty * customRateRwf
}

data class SwrSyncStatus(
    val isRevalidating: Boolean = false,
    val fromCache: Boolean = false,
    val lastSyncedEpochMs: Long = 0L,
    val errorMessage: String? = null
)
