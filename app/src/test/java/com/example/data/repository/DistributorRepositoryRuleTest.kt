package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.StaffRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DistributorRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureInitializedWorkspace_and_observeProducts_succeedsForOwner() = runBlocking {
        val uid = signInTestUser("owner_${UUID.randomUUID()}@kigaliprime.rw")
        val repo = DistributorRepository(db = firestore, swrDao = null, auth = auth)

        val seedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureInitializedWorkspace("Diane Admin", "admin@kigaliprime.rw")
        }
        assertTrue("Workspace seed should succeed: ${seedResult.exceptionOrNull()}", seedResult.isSuccess)

        val products = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { it.isNotEmpty() }
        }
        assertTrue("Seeded catalog should include Primus & Coca-Cola", products.size >= 15)
    }

    @Test
    fun createSalesOrder_and_replenishment_flow_validatesSchemaAndStock() = runBlocking {
        val uid = signInTestUser("pos_${UUID.randomUUID()}@kigaliprime.rw")
        val repo = DistributorRepository(db = firestore, swrDao = null, auth = auth)

        val prod = Product(
            id = "sku_${UUID.randomUUID().toString().take(8)}",
            userId = uid,
            skuCode = "BRW-TST-01",
            name = "Primus 72cl",
            category = ProductCategory.BEER.code,
            subCategory = "Lager",
            unitLabel = "Crate x12",
            costPriceRwf = 9400.0,
            sellingPriceRwf = 10800.0,
            stockQty = 15,
            reorderLevel = 25,
            imageResKey = "beer_crate"
        )
        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveProduct(prod) }
        assertTrue(saveRes.isSuccess)

        val orderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createSalesOrder(
                cartItems = listOf(CartItem(prod, qty = 3)),
                customer = null,
                paymentMethod = PaymentMethod.MOMO,
                paidAmountRwf = 38232.0,
                vatRatePercent = 18.0,
                staffName = "Chantal",
                staffRole = StaffRole.CASHIER
            )
        }
        assertTrue("POS order should succeed: ${orderRes.exceptionOrNull()}", orderRes.isSuccess)

        val poRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createReplenishmentFromAlert(
                lowStockProducts = listOf(prod),
                supplierEmail = "orders@bralirwa.co.rw",
                actorRole = StaffRole.MANAGER,
                markAsEmailed = true
            )
        }
        assertTrue("Replenishment order should succeed: ${poRes.exceptionOrNull()}", poRes.isSuccess)
    }

    @Test
    fun otpResetFlow_generatesAndVerifiesSixDigitCode() = runBlocking {
        val uid = signInTestUser("otp_${UUID.randomUUID()}@kigaliprime.rw")
        val repo = DistributorRepository(db = firestore, swrDao = null, auth = auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureInitializedWorkspace("Diane Admin", "admin@kigaliprime.rw")
        }

        val otpRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.generateOtpResetCode("admin@kigaliprime.rw")
        }
        assertTrue("OTP creation should succeed: ${otpRes.exceptionOrNull()}", otpRes.isSuccess)
        val otp = otpRes.getOrThrow()
        assertEquals(6, otp.otpCode.length)

        val verifyRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.verifyOtpAndResetPin("admin@kigaliprime.rw", otp.otpCode, "9988")
        }
        assertTrue("OTP verification should succeed: ${verifyRes.exceptionOrNull()}", verifyRes.isSuccess)
        assertTrue(uid.isNotBlank())
    }

    @Test
    fun crossUserRead_rejectedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@kigaliprime.rw")
        val aliceRepo = DistributorRepository(db = firestore, swrDao = null, auth = auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureInitializedWorkspace("Alice", "alice@kigaliprime.rw")
        }

        signInTestUser("bob_${UUID.randomUUID()}@kigaliprime.rw")
        val bobRepo = DistributorRepository(db = firestore, swrDao = null, auth = auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeProducts(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull("Cross-user access should fail", caught)
        val fsError = caught?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    @Test
    fun unauthenticatedObserve_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = DistributorRepository(db = firestore, swrDao = null, auth = auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeProducts("some_owner_uid").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull("Unauthenticated query should fail", caught)
        val fsError = caught?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
