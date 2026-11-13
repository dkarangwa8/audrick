package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.SwrDao
import com.example.data.local.toCacheEntity
import com.example.data.model.ActivityLog
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.DistributorSettings
import com.example.data.model.ExpenseRecord
import com.example.data.model.OrderLineItem
import com.example.data.model.OtpResetRecord
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ReplenishmentOrder
import com.example.data.model.SalesOrder
import com.example.data.model.ShipmentRecord
import com.example.data.model.StaffProfile
import com.example.data.model.StaffRole
import com.example.data.model.SwrSyncStatus
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.seed.BralirwaCatalogSeeder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

class DistributorRepository(
    private val db: FirebaseFirestore,
    private val swrDao: SwrDao? = null,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        swrDao = AppDatabase.getInstance(context.applicationContext).swrDao()
    )

    private val _swrStatus = MutableStateFlow(SwrSyncStatus())
    val swrStatus: StateFlow<SwrSyncStatus> = _swrStatus.asStateFlow()

    private val secureRandom = SecureRandom()

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must sign in with Google before accessing distributor data.")

    private fun distributorDoc(uid: String) = db.collection("distributors").document(uid)

    private fun markRevalidating() {
        _swrStatus.value = _swrStatus.value.copy(isRevalidating = true, errorMessage = null)
    }

    private fun markSynced(fromCache: Boolean = false) {
        _swrStatus.value = SwrSyncStatus(
            isRevalidating = false,
            fromCache = fromCache,
            lastSyncedEpochMs = System.currentTimeMillis(),
            errorMessage = null
        )
    }

    private fun markError(msg: String) {
        _swrStatus.value = _swrStatus.value.copy(
            isRevalidating = false,
            errorMessage = msg
        )
    }

    // --- SWR OBSERVERS (INSTANT LOCAL REPAINT + SILENT FIRESTORE REVALIDATION) ---

    fun observeSettings(userId: String = requireUserId()): Flow<DistributorSettings> = channelFlow {
        val docRef = distributorDoc(userId)
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeSettings(userId).collectLatest { cached ->
                    if (cached != null) {
                        _swrStatus.value = _swrStatus.value.copy(fromCache = true, isRevalidating = true)
                        send(cached.toDomain())
                    }
                }
            }
        }
        markRevalidating()
        try {
            docRef.snapshots().collect { snap ->
                val settings = snap.toObject(
                    DistributorSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: DistributorSettings(userId = userId)
                swrDao?.let { launch(Dispatchers.IO) { it.insertSettings(settings.toCacheEntity()) } }
                markSynced(fromCache = snap.metadata.isFromCache)
                send(settings)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.GET, docRef.path)
            markError(e.localizedMessage ?: "Settings sync error")
            throw e
        }
    }

    fun observeProducts(userId: String = requireUserId()): Flow<List<Product>> = channelFlow {
        val colRef = distributorDoc(userId).collection("products")
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeProducts(userId).collectLatest { cached ->
                    if (cached.isNotEmpty()) {
                        send(cached.map { it.toDomain() })
                    }
                }
            }
        }
        try {
            colRef.snapshots().collect { snap ->
                val list = snap.toObjects(
                    Product::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name }
                swrDao?.let { dao ->
                    launch(Dispatchers.IO) {
                        dao.replaceProducts(userId, list.map { it.toCacheEntity() })
                    }
                }
                markSynced(fromCache = snap.metadata.isFromCache)
                send(list)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    fun observeCustomers(userId: String = requireUserId()): Flow<List<Customer>> = channelFlow {
        val colRef = distributorDoc(userId).collection("customers")
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeCustomers(userId).collectLatest { cached ->
                    if (cached.isNotEmpty()) send(cached.map { it.toDomain() })
                }
            }
        }
        try {
            colRef.snapshots().collect { snap ->
                val list = snap.toObjects(
                    Customer::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.totalSpentRwf }
                swrDao?.let { dao ->
                    launch(Dispatchers.IO) { dao.replaceCustomers(userId, list.map { it.toCacheEntity() }) }
                }
                send(list)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    fun observeOrders(userId: String = requireUserId()): Flow<List<SalesOrder>> = channelFlow {
        val colRef = distributorDoc(userId).collection("orders")
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeOrders(userId).collectLatest { cached ->
                    if (cached.isNotEmpty()) send(cached.map { it.toDomain() })
                }
            }
        }
        try {
            colRef.snapshots().collect { snap ->
                val list = snap.toObjects(
                    SalesOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                swrDao?.let { dao ->
                    launch(Dispatchers.IO) { dao.replaceOrders(userId, list.map { it.toCacheEntity() }) }
                }
                send(list)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    fun observeReplenishments(userId: String = requireUserId()): Flow<List<ReplenishmentOrder>> = channelFlow {
        val colRef = distributorDoc(userId).collection("replenishments")
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeReplenishments(userId).collectLatest { cached ->
                    if (cached.isNotEmpty()) send(cached.map { it.toDomain() })
                }
            }
        }
        try {
            colRef.snapshots().collect { snap ->
                val list = snap.toObjects(
                    ReplenishmentOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                swrDao?.let { dao ->
                    launch(Dispatchers.IO) { dao.replaceReplenishments(userId, list.map { it.toCacheEntity() }) }
                }
                send(list)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    fun observeExpenses(userId: String = requireUserId()): Flow<List<ExpenseRecord>> = channelFlow {
        val colRef = distributorDoc(userId).collection("expenses")
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeExpenses(userId).collectLatest { cached ->
                    if (cached.isNotEmpty()) send(cached.map { it.toDomain() })
                }
            }
        }
        try {
            colRef.snapshots().collect { snap ->
                val list = snap.toObjects(
                    ExpenseRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                swrDao?.let { dao ->
                    launch(Dispatchers.IO) { dao.replaceExpenses(userId, list.map { it.toCacheEntity() }) }
                }
                send(list)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    fun observeShipments(userId: String = requireUserId()): Flow<List<ShipmentRecord>> = channelFlow {
        val colRef = distributorDoc(userId).collection("shipments")
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeShipments(userId).collectLatest { cached ->
                    if (cached.isNotEmpty()) send(cached.map { it.toDomain() })
                }
            }
        }
        try {
            colRef.snapshots().collect { snap ->
                val list = snap.toObjects(
                    ShipmentRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                swrDao?.let { dao ->
                    launch(Dispatchers.IO) { dao.replaceShipments(userId, list.map { it.toCacheEntity() }) }
                }
                send(list)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    fun observeActivities(userId: String = requireUserId()): Flow<List<ActivityLog>> = channelFlow {
        val colRef = distributorDoc(userId).collection("activities")
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeActivities(userId).collectLatest { cached ->
                    if (cached.isNotEmpty()) send(cached.map { it.toDomain() })
                }
            }
        }
        try {
            colRef.snapshots().collect { snap ->
                val list = snap.toObjects(
                    ActivityLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                swrDao?.let { dao ->
                    launch(Dispatchers.IO) { dao.replaceActivities(userId, list.map { it.toCacheEntity() }) }
                }
                send(list)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    fun observeStaff(userId: String = requireUserId()): Flow<List<StaffProfile>> = channelFlow {
        val colRef = distributorDoc(userId).collection("staff")
        val cacheJob = swrDao?.let { dao ->
            launch(Dispatchers.IO) {
                dao.observeStaff(userId).collectLatest { cached ->
                    if (cached.isNotEmpty()) send(cached.map { it.toDomain() })
                }
            }
        }
        try {
            colRef.snapshots().collect { snap ->
                val list = snap.toObjects(
                    StaffProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.fullName }
                swrDao?.let { dao ->
                    launch(Dispatchers.IO) { dao.replaceStaff(userId, list.map { it.toCacheEntity() }) }
                }
                send(list)
            }
        } catch (e: CancellationException) {
            cacheJob?.cancel()
            throw e
        } catch (e: Exception) {
            cacheJob?.cancel()
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    // --- INITIAL WORKSPACE SEEDING ---

    suspend fun ensureInitializedWorkspace(
        userDisplayName: String,
        userEmail: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val rootRef = distributorDoc(uid)
        val snap = try {
            rootRef.get().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, rootRef.path)
            throw e
        }
        if (snap.exists()) return@runCatching

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val batch = db.batch()

        val ownerName = userDisplayName.ifBlank { "Depot Admin" }.take(80)
        val ownerEmail = userEmail.ifBlank { "depot@kigaliprime.rw" }.take(120)

        batch.set(
            rootRef,
            mapOf(
                "userId" to uid,
                "businessName" to "Kigali Prime Bralirwa Depot",
                "address" to "KN 14 Ave, Nyarugenge, Kigali, Rwanda",
                "phone" to "+250 788 450 210",
                "email" to ownerEmail,
                "tin" to "102948571",
                "bralirwaEmail" to "orders@bralirwa.co.rw",
                "vatRate" to 18.0,
                "lowStockAutoEmail" to true,
                "recurringBillingEnabled" to true,
                "thermalPaperMm" to 80,
                "activeStaffRole" to StaffRole.ADMIN.code,
                "activeStaffName" to ownerName,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        BralirwaCatalogSeeder.products.forEach { sp ->
            val ref = rootRef.collection("products").document(sp.id)
            batch.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "skuCode" to sp.skuCode,
                    "name" to sp.name,
                    "category" to sp.category,
                    "subCategory" to sp.subCategory,
                    "unitLabel" to sp.unitLabel,
                    "costPriceRwf" to sp.costPriceRwf,
                    "sellingPriceRwf" to sp.sellingPriceRwf,
                    "stockQty" to sp.stockQty,
                    "reorderLevel" to sp.reorderLevel,
                    "batchReceivedEpochMs" to (now - sp.daysOld * dayMs),
                    "imageResKey" to sp.imageResKey,
                    "imageUri" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        BralirwaCatalogSeeder.customers.forEachIndexed { idx, sc ->
            val ref = rootRef.collection("customers").document(sc.id)
            val nextBilling = if (sc.billingCycle == "WEEKLY") now + (idx * dayMs) else now + (10 * dayMs)
            batch.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "name" to sc.name,
                    "phone" to sc.phone,
                    "email" to sc.email,
                    "tin" to sc.tin,
                    "zone" to sc.zone,
                    "totalSpentRwf" to sc.totalSpentRwf,
                    "creditBalanceRwf" to sc.creditBalanceRwf,
                    "billingCycle" to sc.billingCycle,
                    "nextBillingEpochMs" to nextBilling,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        BralirwaCatalogSeeder.expenses.forEach { se ->
            val ref = rootRef.collection("expenses").document(se.id)
            batch.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "category" to se.category,
                    "description" to se.description,
                    "amountRwf" to se.amountRwf,
                    "vehiclePlate" to se.vehiclePlate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        BralirwaCatalogSeeder.shipments.forEach { ss ->
            val ref = rootRef.collection("shipments").document(ss.id)
            batch.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "vehiclePlate" to ss.vehiclePlate,
                    "driverName" to ss.driverName,
                    "routeZone" to ss.routeZone,
                    "cratesLoaded" to ss.cratesLoaded,
                    "revenueRwf" to ss.revenueRwf,
                    "cogsRwf" to ss.cogsRwf,
                    "fuelAndTripCostRwf" to ss.fuelAndTripCostRwf,
                    "status" to ss.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        BralirwaCatalogSeeder.staff.forEachIndexed { idx, st ->
            val ref = rootRef.collection("staff").document(st.id)
            val staffEmail = if (idx == 0) ownerEmail else st.email
            val staffName = if (idx == 0) ownerName else st.fullName
            batch.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "fullName" to staffName,
                    "email" to staffEmail,
                    "role" to st.role,
                    "pinHash" to st.pinHash,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Seed 2 initial orders + 1 replenishment + activity timeline
        val seedItems = BralirwaCatalogSeeder.sampleOrderItems.map { it.toMap() }
        batch.set(
            rootRef.collection("orders").document("ord_seed_1"),
            mapOf(
                "userId" to uid,
                "invoiceNumber" to "INV-2026-1001",
                "customerId" to "cust_chez_lando",
                "customerName" to "Chez Lando Hotel",
                "staffName" to ownerName,
                "staffRole" to StaffRole.ADMIN.code,
                "items" to seedItems,
                "itemsSummary" to "25x Primus 72cl, 30x Coca-Cola 30cl RGB",
                "totalCrates" to 55,
                "subtotalRwf" to 546000.0,
                "vatRwf" to 98280.0,
                "totalRwf" to 644280.0,
                "paidRwf" to 644280.0,
                "balanceRwf" to 0.0,
                "paymentMethod" to PaymentMethod.MOMO.code,
                "status" to "PAID",
                "vehiclePlate" to "RAD 412 B",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.set(
            rootRef.collection("activities").document("act_seed_1"),
            mapOf(
                "userId" to uid,
                "type" to "STOCK_ALERT",
                "title" to "Low stock alert: 5 Bralirwa SKUs below reorder level",
                "detail" to "Mützig 65cl (14), Heineken 33cl (11), Fanta Orange (16), Vital'O (9), Legend Stout (18)",
                "amountRwf" to 0.0,
                "actorRole" to StaffRole.ADMIN.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.set(
            rootRef.collection("activities").document("act_seed_2"),
            mapOf(
                "userId" to uid,
                "type" to "SALE",
                "title" to "Invoice INV-2026-1001 • Chez Lando Hotel",
                "detail" to "55 crates via MTN MoMo • Paid in full",
                "amountRwf" to 644280.0,
                "actorRole" to StaffRole.ADMIN.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, rootRef.path)
            throw e
        }
    }

    // --- PRODUCT INVENTORY & CSV BULK IMPORT ---

    suspend fun saveProduct(product: Product): Result<String> = runCatching {
        val uid = requireUserId()
        val id = product.id.ifBlank { "sku_${UUID.randomUUID().toString().take(12).replace("-", "")}" }
        val docRef = distributorDoc(uid).collection("products").document(id)
        val existing = docRef.get().await()
        try {
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "skuCode" to product.skuCode.trim().take(40),
                        "name" to product.name.trim().take(120),
                        "category" to product.category,
                        "subCategory" to product.subCategory.trim().take(60),
                        "unitLabel" to product.unitLabel.trim().take(40),
                        "costPriceRwf" to product.costPriceRwf.coerceAtLeast(0.0),
                        "sellingPriceRwf" to product.sellingPriceRwf.coerceAtLeast(0.0),
                        "stockQty" to product.stockQty.coerceAtLeast(0),
                        "reorderLevel" to product.reorderLevel.coerceAtLeast(0),
                        "batchReceivedEpochMs" to product.batchReceivedEpochMs,
                        "imageResKey" to product.imageResKey.take(80),
                        "imageUri" to product.imageUri.take(600),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "skuCode" to product.skuCode.trim().take(40),
                        "name" to product.name.trim().take(120),
                        "category" to product.category,
                        "subCategory" to product.subCategory.trim().take(60),
                        "unitLabel" to product.unitLabel.trim().take(40),
                        "costPriceRwf" to product.costPriceRwf.coerceAtLeast(0.0),
                        "sellingPriceRwf" to product.sellingPriceRwf.coerceAtLeast(0.0),
                        "stockQty" to product.stockQty.coerceAtLeast(0),
                        "reorderLevel" to product.reorderLevel.coerceAtLeast(0),
                        "batchReceivedEpochMs" to product.batchReceivedEpochMs,
                        "imageResKey" to product.imageResKey.take(80),
                        "imageUri" to product.imageUri.take(600),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
        id
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = distributorDoc(uid).collection("products").document(productId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun bulkImportCsv(csvContent: String, actorRole: StaffRole): Result<Int> = runCatching {
        val uid = requireUserId()
        val lines = csvContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return@runCatching 0

        val dataLines = if (lines.first().lowercase().contains("sku") || lines.first().lowercase().contains("name")) {
            lines.drop(1)
        } else lines

        val batch = db.batch()
        var imported = 0
        val now = System.currentTimeMillis()
        for (line in dataLines.take(50)) {
            val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
            if (cols.size < 6) continue
            val sku = cols[0].ifBlank { "BRW-CSV-${imported + 1}" }.take(40)
            val name = cols[1].ifBlank { "Bralirwa Item" }.take(120)
            val cat = if (cols[2].uppercase().contains("SOFT")) "SOFT_DRINK" else "BEER"
            val subCat = cols[3].ifBlank { if (cat == "BEER") "Lager" else "Carbonated CSD" }.take(60)
            val unit = cols.getOrNull(4)?.ifBlank { "Crate x24" } ?: "Crate x24"
            val cost = cols.getOrNull(5)?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 8500.0
            val sell = cols.getOrNull(6)?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: (cost * 1.15)
            val stock = cols.getOrNull(7)?.toIntOrNull()?.coerceAtLeast(0) ?: 25
            val reorder = cols.getOrNull(8)?.toIntOrNull()?.coerceAtLeast(0) ?: 20

            val docId = "csv_${sku.replace(Regex("[^a-zA-Z0-9_-]"), "_")}".take(64)
            val ref = distributorDoc(uid).collection("products").document(docId)
            batch.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "skuCode" to sku,
                    "name" to name,
                    "category" to cat,
                    "subCategory" to subCat,
                    "unitLabel" to unit,
                    "costPriceRwf" to cost,
                    "sellingPriceRwf" to sell,
                    "stockQty" to stock,
                    "reorderLevel" to reorder,
                    "batchReceivedEpochMs" to now,
                    "imageResKey" to if (cat == "BEER") "beer_crate" else "soft_drinks",
                    "imageUri" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            imported++
        }

        if (imported > 0) {
            val actRef = distributorDoc(uid).collection("activities").document("act_${UUID.randomUUID().toString().take(10)}")
            batch.set(
                actRef,
                mapOf(
                    "userId" to uid,
                    "type" to "REPLENISHMENT",
                    "title" to "Bulk CSV imported $imported Bralirwa SKUs",
                    "detail" to "Inventory synced via CSV import",
                    "amountRwf" to 0.0,
                    "actorRole" to actorRole.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            try {
                batch.commit().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, distributorDoc(uid).collection("products").path)
                throw e
            }
        }
        imported
    }

    // --- POS CHECKOUT & ORDER MANAGEMENT ---

    suspend fun createSalesOrder(
        cartItems: List<CartItem>,
        customer: Customer?,
        paymentMethod: PaymentMethod,
        paidAmountRwf: Double,
        vatRatePercent: Double,
        staffName: String,
        staffRole: StaffRole,
        vehiclePlate: String = ""
    ): Result<SalesOrder> = runCatching {
        val uid = requireUserId()
        require(cartItems.isNotEmpty()) { "Cart is empty" }
        require(staffRole in listOf(StaffRole.ADMIN, StaffRole.MANAGER, StaffRole.CASHIER)) {
            "Role ${staffRole.label} is not authorized to process POS sales"
        }

        val orderId = "ord_${UUID.randomUUID().toString().take(12).replace("-", "")}"
        val invNum = "INV-${java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())}-${(1000..9999).random()}"

        val lineItems = cartItems.map { ci ->
            OrderLineItem(
                productId = ci.product.id,
                skuCode = ci.product.skuCode,
                name = ci.product.name,
                qty = ci.qty,
                rateRwf = ci.customRateRwf,
                costRateRwf = ci.product.costPriceRwf,
                lineTotalRwf = ci.lineTotalRwf
            )
        }
        val subtotal = lineItems.sumOf { it.lineTotalRwf }
        val vat = subtotal * (vatRatePercent / 100.0)
        val total = subtotal + vat
        val clampedPaid = paidAmountRwf.coerceIn(0.0, total)
        val balance = (total - clampedPaid).coerceAtLeast(0.0)
        val status = when {
            balance <= 1.0 -> "PAID"
            clampedPaid > 0.0 -> "PARTIAL"
            else -> "CREDIT"
        }
        val totalCrates = lineItems.sumOf { it.qty }
        val summary = lineItems.joinToString(", ") { "${it.qty}x ${it.name}" }.take(1500)
        val custName = customer?.name?.ifBlank { "Walk-in Customer" } ?: "Walk-in Customer"

        val batch = db.batch()
        val orderRef = distributorDoc(uid).collection("orders").document(orderId)
        batch.set(
            orderRef,
            mapOf(
                "userId" to uid,
                "invoiceNumber" to invNum,
                "customerId" to (customer?.id ?: ""),
                "customerName" to custName,
                "staffName" to staffName.ifBlank { "Cashier" }.take(80),
                "staffRole" to staffRole.code,
                "items" to lineItems.map { it.toMap() },
                "itemsSummary" to summary,
                "totalCrates" to totalCrates,
                "subtotalRwf" to subtotal,
                "vatRwf" to vat,
                "totalRwf" to total,
                "paidRwf" to clampedPaid,
                "balanceRwf" to balance,
                "paymentMethod" to paymentMethod.code,
                "status" to status,
                "vehiclePlate" to vehiclePlate.take(30),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val newlyAlerted = mutableListOf<String>()
        cartItems.forEach { ci ->
            val newQty = (ci.product.stockQty - ci.qty).coerceAtLeast(0)
            val prodRef = distributorDoc(uid).collection("products").document(ci.product.id)
            batch.update(
                prodRef,
                mapOf(
                    "stockQty" to newQty,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (newQty <= ci.product.reorderLevel) {
                newlyAlerted.add("${ci.product.name} ($newQty left)")
            }
        }

        if (customer != null && customer.id.isNotBlank()) {
            val custRef = distributorDoc(uid).collection("customers").document(customer.id)
            batch.update(
                custRef,
                mapOf(
                    "totalSpentRwf" to (customer.totalSpentRwf + total),
                    "creditBalanceRwf" to (customer.creditBalanceRwf + balance),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        val actId = "act_${UUID.randomUUID().toString().take(12).replace("-", "")}"
        batch.set(
            distributorDoc(uid).collection("activities").document(actId),
            mapOf(
                "userId" to uid,
                "type" to "SALE",
                "title" to "$invNum • $custName",
                "detail" to "$totalCrates crates via ${paymentMethod.label} • Status: $status",
                "amountRwf" to total,
                "actorRole" to staffRole.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (newlyAlerted.isNotEmpty()) {
            val alertActId = "act_${UUID.randomUUID().toString().take(12).replace("-", "")}"
            batch.set(
                distributorDoc(uid).collection("activities").document(alertActId),
                mapOf(
                    "userId" to uid,
                    "type" to "STOCK_ALERT",
                    "title" to "Stock alert triggered for ${newlyAlerted.size} SKU(s)",
                    "detail" to newlyAlerted.joinToString(", ").take(390),
                    "amountRwf" to 0.0,
                    "actorRole" to staffRole.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, orderRef.path)
            throw e
        }

        SalesOrder(
            id = orderId,
            userId = uid,
            invoiceNumber = invNum,
            customerId = customer?.id ?: "",
            customerName = custName,
            staffName = staffName,
            staffRole = staffRole.code,
            items = lineItems,
            itemsSummary = summary,
            totalCrates = totalCrates,
            subtotalRwf = subtotal,
            vatRwf = vat,
            totalRwf = total,
            paidRwf = clampedPaid,
            balanceRwf = balance,
            paymentMethod = paymentMethod.code,
            status = status,
            vehiclePlate = vehiclePlate
        )
    }

    // --- AUTOMATED BRALIRWA REPLENISHMENT ORDERS ---

    suspend fun createReplenishmentFromAlert(
        lowStockProducts: List<Product>,
        supplierEmail: String,
        actorRole: StaffRole,
        markAsEmailed: Boolean,
        triggeredByAlert: Boolean = true
    ): Result<ReplenishmentOrder> = runCatching {
        val uid = requireUserId()
        require(lowStockProducts.isNotEmpty()) { "No SKUs selected for replenishment" }
        require(actorRole in listOf(StaffRole.ADMIN, StaffRole.MANAGER, StaffRole.WAREHOUSE)) {
            "Role ${actorRole.label} cannot place Bralirwa replenishment orders"
        }

        val poId = "po_${UUID.randomUUID().toString().take(12).replace("-", "")}"
        val poNum = "BRW-PO-${(1000..9999).random()}"

        val lineItems = lowStockProducts.take(50).map { p ->
            val targetStock = (p.reorderLevel * 3).coerceAtLeast(50)
            val orderQty = (targetStock - p.stockQty).coerceAtLeast(20)
            OrderLineItem(
                productId = p.id,
                skuCode = p.skuCode,
                name = p.name,
                qty = orderQty,
                rateRwf = p.costPriceRwf,
                costRateRwf = p.costPriceRwf,
                lineTotalRwf = orderQty * p.costPriceRwf
            )
        }
        val totalCrates = lineItems.sumOf { it.qty }
        val totalCost = lineItems.sumOf { it.lineTotalRwf }
        val summary = lineItems.joinToString(", ") { "${it.qty}x ${it.name}" }.take(1500)
        val status = if (markAsEmailed) "EMAILED" else "DRAFT"

        val batch = db.batch()
        val poRef = distributorDoc(uid).collection("replenishments").document(poId)
        batch.set(
            poRef,
            mapOf(
                "userId" to uid,
                "poNumber" to poNum,
                "supplierName" to "BRALIRWA PLC — Kigali / Gisenyi Depot",
                "supplierEmail" to supplierEmail.ifBlank { "orders@bralirwa.co.rw" }.take(120),
                "items" to lineItems.map { it.toMap() },
                "itemsSummary" to summary,
                "totalCrates" to totalCrates,
                "totalCostRwf" to totalCost,
                "status" to status,
                "triggeredByAlert" to triggeredByAlert,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val actId = "act_${UUID.randomUUID().toString().take(12).replace("-", "")}"
        batch.set(
            distributorDoc(uid).collection("activities").document(actId),
            mapOf(
                "userId" to uid,
                "type" to if (markAsEmailed) "EMAIL" else "REPLENISHMENT",
                "title" to "$poNum • Bralirwa Stock Replenishment ($totalCrates crates)",
                "detail" to if (markAsEmailed) "Emailed to $supplierEmail: $summary" else "Draft PO created: $summary",
                "amountRwf" to totalCost,
                "actorRole" to actorRole.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, poRef.path)
            throw e
        }

        ReplenishmentOrder(
            id = poId,
            userId = uid,
            poNumber = poNum,
            supplierEmail = supplierEmail,
            items = lineItems,
            itemsSummary = summary,
            totalCrates = totalCrates,
            totalCostRwf = totalCost,
            status = status,
            triggeredByAlert = triggeredByAlert
        )
    }

    suspend fun markReplenishmentDelivered(
        order: ReplenishmentOrder,
        currentProducts: List<Product>,
        actorRole: StaffRole
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(order.status !in listOf("DELIVERED", "CANCELLED")) { "Order already finalized" }
        val batch = db.batch()
        val poRef = distributorDoc(uid).collection("replenishments").document(order.id)
        batch.update(
            poRef,
            mapOf(
                "status" to "DELIVERED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val now = System.currentTimeMillis()
        val productMap = currentProducts.associateBy { it.id }
        order.items.forEach { item ->
            val existingProd = productMap[item.productId]
            if (existingProd != null) {
                val prodRef = distributorDoc(uid).collection("products").document(existingProd.id)
                batch.update(
                    prodRef,
                    mapOf(
                        "stockQty" to (existingProd.stockQty + item.qty),
                        "batchReceivedEpochMs" to now,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        val actId = "act_${UUID.randomUUID().toString().take(12).replace("-", "")}"
        batch.set(
            distributorDoc(uid).collection("activities").document(actId),
            mapOf(
                "userId" to uid,
                "type" to "REPLENISHMENT",
                "title" to "${order.poNumber} Delivered • +${order.totalCrates} crates",
                "detail" to "Inventory restocked & batch aging reset",
                "amountRwf" to order.totalCostRwf,
                "actorRole" to actorRole.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, poRef.path)
            throw e
        }
    }

    // --- CUSTOMERS, EXPENSES, SHIPMENTS, SETTINGS ---

    suspend fun saveCustomer(customer: Customer): Result<Unit> = runCatching {
        val uid = requireUserId()
        val id = customer.id.ifBlank { "cust_${UUID.randomUUID().toString().take(12).replace("-", "")}" }
        val ref = distributorDoc(uid).collection("customers").document(id)
        val exists = ref.get().await().exists()
        try {
            if (!exists) {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "name" to customer.name.trim().take(120),
                        "phone" to customer.phone.trim().take(40),
                        "email" to customer.email.trim().take(120),
                        "tin" to customer.tin.trim().take(40),
                        "zone" to customer.zone.trim().take(80),
                        "totalSpentRwf" to customer.totalSpentRwf.coerceAtLeast(0.0),
                        "creditBalanceRwf" to customer.creditBalanceRwf.coerceAtLeast(0.0),
                        "billingCycle" to customer.billingCycle,
                        "nextBillingEpochMs" to customer.nextBillingEpochMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "name" to customer.name.trim().take(120),
                        "phone" to customer.phone.trim().take(40),
                        "email" to customer.email.trim().take(120),
                        "tin" to customer.tin.trim().take(40),
                        "zone" to customer.zone.trim().take(80),
                        "totalSpentRwf" to customer.totalSpentRwf.coerceAtLeast(0.0),
                        "creditBalanceRwf" to customer.creditBalanceRwf.coerceAtLeast(0.0),
                        "billingCycle" to customer.billingCycle,
                        "nextBillingEpochMs" to customer.nextBillingEpochMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }

    suspend fun addExpense(
        category: String,
        description: String,
        amountRwf: Double,
        vehiclePlate: String,
        actorRole: StaffRole
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(amountRwf > 0) { "Amount must be greater than 0" }
        val id = "exp_${UUID.randomUUID().toString().take(12).replace("-", "")}"
        val batch = db.batch()
        val ref = distributorDoc(uid).collection("expenses").document(id)
        batch.set(
            ref,
            mapOf(
                "userId" to uid,
                "category" to category,
                "description" to description.trim().take(250),
                "amountRwf" to amountRwf,
                "vehiclePlate" to vehiclePlate.trim().take(30),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val actRef = distributorDoc(uid).collection("activities").document("act_${UUID.randomUUID().toString().take(10)}")
        batch.set(
            actRef,
            mapOf(
                "userId" to uid,
                "type" to "EXPENSE",
                "title" to "Expense • $category",
                "detail" to description.trim().take(390),
                "amountRwf" to amountRwf,
                "actorRole" to actorRole.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun addShipment(
        vehiclePlate: String,
        driverName: String,
        routeZone: String,
        cratesLoaded: Int,
        revenueRwf: Double,
        cogsRwf: Double,
        fuelAndTripCostRwf: Double,
        actorRole: StaffRole
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val id = "ship_${UUID.randomUUID().toString().take(12).replace("-", "")}"
        val batch = db.batch()
        val ref = distributorDoc(uid).collection("shipments").document(id)
        batch.set(
            ref,
            mapOf(
                "userId" to uid,
                "vehiclePlate" to vehiclePlate.trim().take(30),
                "driverName" to driverName.trim().take(80),
                "routeZone" to routeZone.trim().take(100),
                "cratesLoaded" to cratesLoaded.coerceAtLeast(0),
                "revenueRwf" to revenueRwf.coerceAtLeast(0.0),
                "cogsRwf" to cogsRwf.coerceAtLeast(0.0),
                "fuelAndTripCostRwf" to fuelAndTripCostRwf.coerceAtLeast(0.0),
                "status" to "ON_ROUTE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val actRef = distributorDoc(uid).collection("activities").document("act_${UUID.randomUUID().toString().take(10)}")
        batch.set(
            actRef,
            mapOf(
                "userId" to uid,
                "type" to "SHIPMENT",
                "title" to "Truck $vehiclePlate Dispatched ($routeZone)",
                "detail" to "$driverName • $cratesLoaded crates loaded",
                "amountRwf" to revenueRwf,
                "actorRole" to actorRole.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    suspend fun completeShipment(shipmentId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = distributorDoc(uid).collection("shipments").document(shipmentId)
        try {
            ref.update(
                mapOf(
                    "status" to "COMPLETED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun updateSettings(settings: DistributorSettings): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = distributorDoc(uid)
        try {
            ref.update(
                mapOf(
                    "businessName" to settings.businessName.trim().take(120),
                    "address" to settings.address.trim().take(250),
                    "phone" to settings.phone.trim().take(40),
                    "email" to settings.email.trim().take(120),
                    "tin" to settings.tin.trim().take(40),
                    "bralirwaEmail" to settings.bralirwaEmail.trim().take(120),
                    "vatRate" to settings.vatRate.coerceIn(0.0, 100.0),
                    "lowStockAutoEmail" to settings.lowStockAutoEmail,
                    "recurringBillingEnabled" to settings.recurringBillingEnabled,
                    "thermalPaperMm" to if (settings.thermalPaperMm == 58) 58 else 80,
                    "activeStaffRole" to settings.activeStaffRole,
                    "activeStaffName" to settings.activeStaffName.trim().take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    // --- ROLE SWITCHING & 6-DIGIT OTP RESET FLOW (10-MIN EXPIRY) ---

    suspend fun switchStaffRole(
        staff: StaffProfile,
        enteredPin: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(staff.pinHash == enteredPin.trim()) { "Invalid staff PIN for ${staff.fullName}" }
        val batch = db.batch()
        batch.update(
            distributorDoc(uid),
            mapOf(
                "activeStaffRole" to staff.role,
                "activeStaffName" to staff.fullName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            distributorDoc(uid).collection("activities").document("act_${UUID.randomUUID().toString().take(10)}"),
            mapOf(
                "userId" to uid,
                "type" to "SECURITY",
                "title" to "Active Role Switched: ${staff.role}",
                "detail" to "${staff.fullName} signed into shift",
                "amountRwf" to 0.0,
                "actorRole" to staff.role,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, distributorDoc(uid).path)
            throw e
        }
    }

    suspend fun generateOtpResetCode(email: String): Result<OtpResetRecord> = runCatching {
        val uid = requireUserId()
        val trimmedEmail = email.trim().lowercase()
        require(trimmedEmail.contains("@")) { "Enter a valid staff email address" }

        val otpCode = String.format(java.util.Locale.US, "%06d", secureRandom.nextInt(1_000_000))
        val expiresAt = System.currentTimeMillis() + (10L * 60L * 1000L) // 10-minute expiry
        val otpId = "otp_${UUID.randomUUID().toString().take(12).replace("-", "")}"

        val ref = distributorDoc(uid).collection("otp_requests").document(otpId)
        try {
            ref.set(
                mapOf(
                    "userId" to uid,
                    "email" to trimmedEmail,
                    "otpCode" to otpCode,
                    "expiresAtMillis" to expiresAt,
                    "used" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }

        OtpResetRecord(
            id = otpId,
            userId = uid,
            email = trimmedEmail,
            otpCode = otpCode,
            expiresAtMillis = expiresAt,
            used = false
        )
    }

    suspend fun verifyOtpAndResetPin(
        email: String,
        otpCode: String,
        newPin: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val trimmedEmail = email.trim().lowercase()
        require(otpCode.trim().length == 6) { "Enter the 6-digit OTP code" }
        require(newPin.trim().length >= 4) { "New PIN/Password must be at least 4 characters" }

        val otpSnap = distributorDoc(uid).collection("otp_requests")
            .whereEqualTo("email", trimmedEmail)
            .whereEqualTo("used", false)
            .get()
            .await()

        val matching = otpSnap.toObjects(OtpResetRecord::class.java)
            .filter { it.otpCode == otpCode.trim() }
            .maxByOrNull { it.expiresAtMillis }
            ?: throw IllegalArgumentException("Invalid or already used OTP code")

        if (System.currentTimeMillis() > matching.expiresAtMillis) {
            throw IllegalStateException("OTP expired (10-min limit exceeded). Please request a new OTP.")
        }

        val staffSnap = distributorDoc(uid).collection("staff")
            .get()
            .await()
            .toObjects(StaffProfile::class.java)
        val targetStaff = staffSnap.firstOrNull { it.email.equals(trimmedEmail, ignoreCase = true) }
            ?: throw IllegalArgumentException("No staff account found for $trimmedEmail")

        val batch = db.batch()
        batch.update(
            distributorDoc(uid).collection("otp_requests").document(matching.id),
            mapOf("used" to true, "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.update(
            distributorDoc(uid).collection("staff").document(targetStaff.id),
            mapOf("pinHash" to newPin.trim().take(128), "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.set(
            distributorDoc(uid).collection("activities").document("act_${UUID.randomUUID().toString().take(10)}"),
            mapOf(
                "userId" to uid,
                "type" to "SECURITY",
                "title" to "Staff PIN Reset Verified via 6-Digit OTP",
                "detail" to "Credentials updated for ${targetStaff.fullName} ($trimmedEmail)",
                "amountRwf" to 0.0,
                "actorRole" to targetStaff.role,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, distributorDoc(uid).path)
            throw e
        }

        targetStaff.fullName
    }

    suspend fun logEmailDispatch(title: String, detail: String, actorRole: StaffRole): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = distributorDoc(uid).collection("activities")
            .document("act_${UUID.randomUUID().toString().take(12).replace("-", "")}")
        try {
            ref.set(
                mapOf(
                    "userId" to uid,
                    "type" to "EMAIL",
                    "title" to title.take(140),
                    "detail" to detail.take(400),
                    "amountRwf" to 0.0,
                    "actorRole" to actorRole.code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }
}
