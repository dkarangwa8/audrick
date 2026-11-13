const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_depot_1";
const BOB_UID = "bob_depot_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`distributors/${ALICE_UID}/products`).get());
  await assertFails(
    unauthDb.collection(`distributors/${ALICE_UID}/products`).doc("p1").set({
      userId: ALICE_UID,
      name: "Primus",
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice products", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection(`distributors/${ALICE_UID}/products`).doc("primus72").set({
      userId: ALICE_UID,
      skuCode: "BRW-PRI-72",
      name: "Primus 72cl",
      category: "BEER",
      subCategory: "Lager",
      unitLabel: "Crate x12",
      costPriceRwf: 9200,
      sellingPriceRwf: 10500,
      stockQty: 120,
      reorderLevel: 30,
      batchReceivedEpochMs: 1700000000000,
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`distributors/${ALICE_UID}/products`).get());
  await assertFails(bobDb.collection(`distributors/${ALICE_UID}/products`).doc("primus72").get());
  await assertFails(
    bobDb.collection(`distributors/${ALICE_UID}/products`).doc("primus72").update({ stockQty: 0 })
  );
});

test("Owner can create and update valid product, rejects ghost fields and negative stock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection(`distributors/${ALICE_UID}/products`).doc("mutzig65");
  const ts = new Date(Date.now() - 5000);

  // Ghost field rejected
  await assertFails(
    ref.set({
      userId: ALICE_UID,
      skuCode: "BRW-MUT-65",
      name: "Mutzig 65cl",
      category: "BEER",
      subCategory: "Lager",
      unitLabel: "Crate x12",
      costPriceRwf: 11200,
      sellingPriceRwf: 12800,
      stockQty: 50,
      reorderLevel: 20,
      batchReceivedEpochMs: 1700000000000,
      ghostAdminFlag: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid create succeeds
  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      skuCode: "BRW-MUT-65",
      name: "Mutzig 65cl",
      category: "BEER",
      subCategory: "Lager",
      unitLabel: "Crate x12",
      costPriceRwf: 11200,
      sellingPriceRwf: 12800,
      stockQty: 50,
      reorderLevel: 20,
      batchReceivedEpochMs: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Negative stock rejected on update
  await assertFails(ref.update({ stockQty: -5, updatedAt: new Date(Date.now() - 1000) }));

  // Valid stock update succeeds
  await assertSucceeds(ref.update({ stockQty: 42, updatedAt: new Date(Date.now() - 1000) }));
});

test("SalesOrder terminal VOIDED state cannot be updated further", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.collection(`distributors/${ALICE_UID}/orders`).doc("ord1");
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    orderRef.set({
      userId: ALICE_UID,
      invoiceNumber: "INV-2026-001",
      customerName: "Chez Lando",
      staffName: "Jean",
      staffRole: "CASHIER",
      items: [{ productId: "p1", name: "Primus 72cl", qty: 5, rateRwf: 10500, lineTotalRwf: 52500 }],
      itemsSummary: "5x Primus 72cl",
      totalCrates: 5,
      subtotalRwf: 52500,
      vatRwf: 9450,
      totalRwf: 61950,
      paidRwf: 30000,
      balanceRwf: 31950,
      paymentMethod: "MOMO",
      status: "PARTIAL",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Void order
  await assertSucceeds(orderRef.update({ status: "VOIDED", updatedAt: new Date(Date.now() - 1000) }));

  // Subsequent update on VOIDED fails
  await assertFails(orderRef.update({ status: "PAID", paidRwf: 61950, balanceRwf: 0, updatedAt: new Date(Date.now() - 500) }));
});

test("Replenishment terminal DELIVERED state lock & OTP single-use lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const poRef = aliceDb.collection(`distributors/${ALICE_UID}/replenishments`).doc("po1");
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    poRef.set({
      userId: ALICE_UID,
      poNumber: "BRW-PO-101",
      supplierName: "Bralirwa Depot Kigali",
      supplierEmail: "orders@bralirwa.co.rw",
      items: [{ productId: "p1", name: "Primus 72cl", qty: 50, rateRwf: 9200, lineTotalRwf: 460000 }],
      itemsSummary: "50x Primus 72cl",
      totalCrates: 50,
      totalCostRwf: 460000,
      status: "EMAILED",
      triggeredByAlert: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(poRef.update({ status: "DELIVERED", updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(poRef.update({ status: "CANCELLED", updatedAt: new Date(Date.now() - 500) }));

  const otpRef = aliceDb.collection(`distributors/${ALICE_UID}/otp_requests`).doc("otp1");
  await assertSucceeds(
    otpRef.set({
      userId: ALICE_UID,
      email: "staff@distributor.rw",
      otpCode: "482910",
      expiresAtMillis: Date.now() + 600000,
      used: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(otpRef.update({ used: true, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(otpRef.update({ used: false, updatedAt: new Date(Date.now() - 500) }));
});
