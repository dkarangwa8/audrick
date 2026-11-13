package com.example.data.seed

import com.example.data.model.OrderLineItem
import com.example.data.model.ProductCategory
import com.example.data.model.StaffRole

object BralirwaCatalogSeeder {
    data class SeedProduct(
        val id: String,
        val skuCode: String,
        val name: String,
        val category: String,
        val subCategory: String,
        val unitLabel: String,
        val costPriceRwf: Double,
        val sellingPriceRwf: Double,
        val stockQty: Int,
        val reorderLevel: Int,
        val daysOld: Int,
        val imageResKey: String
    )

    data class SeedCustomer(
        val id: String,
        val name: String,
        val phone: String,
        val email: String,
        val tin: String,
        val zone: String,
        val totalSpentRwf: Double,
        val creditBalanceRwf: Double,
        val billingCycle: String
    )

    data class SeedExpense(
        val id: String,
        val category: String,
        val description: String,
        val amountRwf: Double,
        val vehiclePlate: String
    )

    data class SeedShipment(
        val id: String,
        val vehiclePlate: String,
        val driverName: String,
        val routeZone: String,
        val cratesLoaded: Int,
        val revenueRwf: Double,
        val cogsRwf: Double,
        val fuelAndTripCostRwf: Double,
        val status: String
    )

    data class SeedStaff(
        val id: String,
        val fullName: String,
        val email: String,
        val role: String,
        val pinHash: String
    )

    // Authentic Bralirwa Rwanda Beer & Soft Drink portfolio
    val products = listOf(
        // BEERS (https://bralirwa.co.rw/beers)
        SeedProduct("sku_primus_72", "BRW-PRI-72", "Primus 72cl", ProductCategory.BEER.code, "Lager", "Crate x12", 9400.0, 10800.0, 145, 40, 6, "beer_crate"),
        SeedProduct("sku_primus_50", "BRW-PRI-50", "Primus 50cl", ProductCategory.BEER.code, "Lager", "Crate x20", 11200.0, 12800.0, 92, 35, 11, "beer_crate"),
        SeedProduct("sku_primus_citron", "BRW-PRC-50", "Primus Citron 50cl", ProductCategory.BEER.code, "Flavored Lager", "Crate x20", 11600.0, 13200.0, 22, 25, 24, "beer_crate"),
        SeedProduct("sku_mutzig_65", "BRW-MUT-65", "Mützig 65cl", ProductCategory.BEER.code, "Lager", "Crate x12", 11500.0, 13200.0, 14, 30, 8, "beer_crate"),
        SeedProduct("sku_mutzig_33", "BRW-MUT-33", "Mützig 33cl", ProductCategory.BEER.code, "Lager", "Crate x24", 13800.0, 15800.0, 68, 25, 19, "beer_crate"),
        SeedProduct("sku_amstel_33", "BRW-AMS-33", "Amstel Malt 33cl", ProductCategory.BEER.code, "Premium Lager", "Crate x24", 16200.0, 18500.0, 44, 20, 32, "beer_crate"),
        SeedProduct("sku_heineken_33", "BRW-HEI-33", "Heineken 33cl", ProductCategory.BEER.code, "Premium Lager", "Crate x24", 21500.0, 24500.0, 11, 18, 14, "beer_crate"),
        SeedProduct("sku_turbo_king", "BRW-TRB-72", "Turbo King 72cl", ProductCategory.BEER.code, "Dark Stout", "Crate x12", 10400.0, 12000.0, 56, 20, 52, "dark_malt"),
        SeedProduct("sku_legend_33", "BRW-LEG-33", "Legend Extra Stout 33cl", ProductCategory.BEER.code, "Dark Stout", "Crate x24", 14900.0, 17000.0, 18, 20, 68, "dark_malt"),
        SeedProduct("sku_huza_50", "BRW-HUZ-50", "Huza 50cl", ProductCategory.BEER.code, "Lager", "Crate x20", 8800.0, 10200.0, 74, 25, 96, "beer_crate"),

        // SOFT DRINKS (https://bralirwa.co.rw/softDrinks)
        SeedProduct("sku_coca_30", "BRW-COC-30", "Coca-Cola 30cl RGB", ProductCategory.SOFT_DRINK.code, "Carbonated CSD", "Crate x24", 7800.0, 9200.0, 160, 45, 5, "soft_drinks"),
        SeedProduct("sku_fanta_orange", "BRW-FAO-30", "Fanta Orange 30cl RGB", ProductCategory.SOFT_DRINK.code, "Carbonated CSD", "Crate x24", 7800.0, 9200.0, 16, 40, 12, "soft_drinks"),
        SeedProduct("sku_fanta_citron", "BRW-FAC-30", "Fanta Citron 30cl RGB", ProductCategory.SOFT_DRINK.code, "Carbonated CSD", "Crate x24", 7800.0, 9200.0, 84, 35, 18, "soft_drinks"),
        SeedProduct("sku_fanta_fiesta", "BRW-FAF-30", "Fanta Fiesta 30cl RGB", ProductCategory.SOFT_DRINK.code, "Carbonated CSD", "Crate x24", 7800.0, 9200.0, 52, 25, 29, "soft_drinks"),
        SeedProduct("sku_sprite_30", "BRW-SPR-30", "Sprite 30cl RGB", ProductCategory.SOFT_DRINK.code, "Carbonated CSD", "Crate x24", 7800.0, 9200.0, 64, 30, 9, "soft_drinks"),
        SeedProduct("sku_vitalo_30", "BRW-VIT-30", "Vital'O 30cl RGB", ProductCategory.SOFT_DRINK.code, "Carbonated CSD", "Crate x24", 7600.0, 9000.0, 9, 25, 22, "soft_drinks"),
        SeedProduct("sku_coca_pet_50", "BRW-COC-PET", "Coca-Cola PET 50cl", ProductCategory.SOFT_DRINK.code, "PET Pack", "Pack x12", 6200.0, 7400.0, 72, 25, 48, "soft_drinks"),
        SeedProduct("sku_chello_30", "BRW-CHL-30", "Chello Malt 30cl", ProductCategory.SOFT_DRINK.code, "Non-Alcoholic Malt", "Crate x24", 10200.0, 11800.0, 38, 15, 36, "dark_malt")
    )

    val customers = listOf(
        SeedCustomer("cust_chez_lando", "Chez Lando Hotel", "+250 788 302 110", "procurement@chezlando.rw", "100428192", "Remera", 3420000.0, 185000.0, "WEEKLY"),
        SeedCustomer("cust_serena", "Kigali Serena Lounge", "+250 788 184 500", "fb@serenakigali.rw", "100119283", "Nyarugenge", 5180000.0, 0.0, "MONTHLY"),
        SeedCustomer("cust_simba", "Simba Supermarket Kimironko", "+250 788 612 900", "orders@simbasupermarket.rw", "101847261", "Kimironko", 4250000.0, 320000.0, "WEEKLY"),
        SeedCustomer("cust_mille_collines", "Hôtel des Mille Collines", "+250 788 220 330", "purchasing@millecollines.rw", "100028471", "Kiyovu", 2890000.0, 0.0, "MONTHLY"),
        SeedCustomer("cust_remera_depot", "Nyabugogo Mini-Depot", "+250 783 911 402", "nyabugogo.drinks@gmail.com", "103948112", "Nyabugogo", 1960000.0, 145000.0, "NONE")
    )

    val expenses = listOf(
        SeedExpense("exp_fuel_1", "FUEL_FLEET", "Diesel refill RAD 412 B — Kigali North loop", 145000.0, "RAD 412 B"),
        SeedExpense("exp_rent_1", "WAREHOUSE_RENT", "Nyarugenge main warehouse monthly lease", 480000.0, ""),
        SeedExpense("exp_payroll_1", "PAYROLL", "Loading bay & driver weekly allowances", 320000.0, ""),
        SeedExpense("exp_crate_1", "CRATE_DEPOSIT", "Empty RGB crate pallet breakage reserve", 95000.0, ""),
        SeedExpense("exp_util_1", "UTILITIES", "Cold room electricity & depot water", 112000.0, "")
    )

    val shipments = listOf(
        SeedShipment("ship_rad412", "RAD 412 B", "Emmanuel Habimana", "Remera - Kimironko", 180, 2140000.0, 1795000.0, 92000.0, "COMPLETED"),
        SeedShipment("ship_rae809", "RAE 809 C", "Patrick Mugisha", "Nyarugenge - Kiyovu", 145, 1890000.0, 1580000.0, 68000.0, "ON_ROUTE"),
        SeedShipment("ship_raf215", "RAF 215 K", "Jean Bosco Niyonzima", "Kicukiro - Bugesera", 110, 1340000.0, 1135000.0, 105000.0, "LOADING")
    )

    val staff = listOf(
        SeedStaff("staff_admin", "Diane Uwimana", "admin@kigaliprime.rw", StaffRole.ADMIN.code, "1234"),
        SeedStaff("staff_manager", "Eric Nshimiyimana", "manager@kigaliprime.rw", StaffRole.MANAGER.code, "2222"),
        SeedStaff("staff_cashier", "Chantal Mukamana", "cashier@kigaliprime.rw", StaffRole.CASHIER.code, "3333"),
        SeedStaff("staff_warehouse", "Innocent Bizimana", "warehouse@kigaliprime.rw", StaffRole.WAREHOUSE.code, "4444")
    )

    val sampleOrderItems = listOf(
        OrderLineItem("sku_primus_72", "BRW-PRI-72", "Primus 72cl", 25, 10800.0, 9400.0, 270000.0),
        OrderLineItem("sku_coca_30", "BRW-COC-30", "Coca-Cola 30cl RGB", 30, 9200.0, 7800.0, 276000.0)
    )
}
