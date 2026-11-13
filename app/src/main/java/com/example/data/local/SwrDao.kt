package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SwrDao {
    // Products
    @Query("SELECT * FROM cached_products WHERE userId = :userId ORDER BY name ASC")
    fun observeProducts(userId: String): Flow<List<CachedProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(items: List<CachedProductEntity>)

    @Query("DELETE FROM cached_products WHERE userId = :userId")
    suspend fun clearProducts(userId: String)

    @Transaction
    suspend fun replaceProducts(userId: String, items: List<CachedProductEntity>) {
        clearProducts(userId)
        if (items.isNotEmpty()) insertProducts(items)
    }

    // Customers
    @Query("SELECT * FROM cached_customers WHERE userId = :userId ORDER BY totalSpentRwf DESC")
    fun observeCustomers(userId: String): Flow<List<CachedCustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(items: List<CachedCustomerEntity>)

    @Query("DELETE FROM cached_customers WHERE userId = :userId")
    suspend fun clearCustomers(userId: String)

    @Transaction
    suspend fun replaceCustomers(userId: String, items: List<CachedCustomerEntity>) {
        clearCustomers(userId)
        if (items.isNotEmpty()) insertCustomers(items)
    }

    // Orders
    @Query("SELECT * FROM cached_orders WHERE userId = :userId ORDER BY createdEpochMs DESC")
    fun observeOrders(userId: String): Flow<List<CachedOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(items: List<CachedOrderEntity>)

    @Query("DELETE FROM cached_orders WHERE userId = :userId")
    suspend fun clearOrders(userId: String)

    @Transaction
    suspend fun replaceOrders(userId: String, items: List<CachedOrderEntity>) {
        clearOrders(userId)
        if (items.isNotEmpty()) insertOrders(items)
    }

    // Replenishments
    @Query("SELECT * FROM cached_replenishments WHERE userId = :userId ORDER BY createdEpochMs DESC")
    fun observeReplenishments(userId: String): Flow<List<CachedReplenishmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplenishments(items: List<CachedReplenishmentEntity>)

    @Query("DELETE FROM cached_replenishments WHERE userId = :userId")
    suspend fun clearReplenishments(userId: String)

    @Transaction
    suspend fun replaceReplenishments(userId: String, items: List<CachedReplenishmentEntity>) {
        clearReplenishments(userId)
        if (items.isNotEmpty()) insertReplenishments(items)
    }

    // Expenses
    @Query("SELECT * FROM cached_expenses WHERE userId = :userId ORDER BY createdEpochMs DESC")
    fun observeExpenses(userId: String): Flow<List<CachedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(items: List<CachedExpenseEntity>)

    @Query("DELETE FROM cached_expenses WHERE userId = :userId")
    suspend fun clearExpenses(userId: String)

    @Transaction
    suspend fun replaceExpenses(userId: String, items: List<CachedExpenseEntity>) {
        clearExpenses(userId)
        if (items.isNotEmpty()) insertExpenses(items)
    }

    // Shipments
    @Query("SELECT * FROM cached_shipments WHERE userId = :userId ORDER BY createdEpochMs DESC")
    fun observeShipments(userId: String): Flow<List<CachedShipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipments(items: List<CachedShipmentEntity>)

    @Query("DELETE FROM cached_shipments WHERE userId = :userId")
    suspend fun clearShipments(userId: String)

    @Transaction
    suspend fun replaceShipments(userId: String, items: List<CachedShipmentEntity>) {
        clearShipments(userId)
        if (items.isNotEmpty()) insertShipments(items)
    }

    // Activities
    @Query("SELECT * FROM cached_activities WHERE userId = :userId ORDER BY createdEpochMs DESC LIMIT 60")
    fun observeActivities(userId: String): Flow<List<CachedActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(items: List<CachedActivityEntity>)

    @Query("DELETE FROM cached_activities WHERE userId = :userId")
    suspend fun clearActivities(userId: String)

    @Transaction
    suspend fun replaceActivities(userId: String, items: List<CachedActivityEntity>) {
        clearActivities(userId)
        if (items.isNotEmpty()) insertActivities(items)
    }

    // Settings
    @Query("SELECT * FROM cached_settings WHERE userId = :userId LIMIT 1")
    fun observeSettings(userId: String): Flow<CachedSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(item: CachedSettingsEntity)

    // Staff
    @Query("SELECT * FROM cached_staff WHERE userId = :userId ORDER BY fullName ASC")
    fun observeStaff(userId: String): Flow<List<CachedStaffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(items: List<CachedStaffEntity>)

    @Query("DELETE FROM cached_staff WHERE userId = :userId")
    suspend fun clearStaff(userId: String)

    @Transaction
    suspend fun replaceStaff(userId: String, items: List<CachedStaffEntity>) {
        clearStaff(userId)
        if (items.isNotEmpty()) insertStaff(items)
    }
}
