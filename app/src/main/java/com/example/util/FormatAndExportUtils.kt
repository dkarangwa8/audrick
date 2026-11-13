package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.Customer
import com.example.data.model.DistributorSettings
import com.example.data.model.Product
import com.example.data.model.ReplenishmentOrder
import com.example.data.model.SalesOrder
import com.example.data.model.ShipmentRecord
import com.example.data.model.StockAgingBucket
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRwf(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "RWF ${nf.format(amount)}"
}

fun formatCompactRwf(amount: Double): String {
    return when {
        kotlin.math.abs(amount) >= 1_000_000 -> String.format(Locale.US, "%.2fM", amount / 1_000_000.0)
        kotlin.math.abs(amount) >= 1_000 -> String.format(Locale.US, "%.1fK", amount / 1_000.0)
        else -> String.format(Locale.US, "%.0f", amount)
    }
}

fun formatDateTime(epochMs: Long): String {
    if (epochMs <= 0L) return "Just now"
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(epochMs))
}

fun dispatchEmailIntent(
    context: Context,
    recipient: String,
    subject: String,
    body: String
): Boolean {
    val mailUri = Uri.parse(
        "mailto:${Uri.encode(recipient)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
    )
    val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(Intent.createChooser(intent, "Send Email via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        true
    } catch (_: Exception) {
        false
    }
}

fun sharePlainTextOrCsv(
    context: Context,
    title: String,
    content: String,
    mimeType: String = "text/csv"
): Boolean {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        true
    } catch (_: Exception) {
        false
    }
}

fun buildBralirwaReplenishmentEmailBody(
    order: ReplenishmentOrder,
    settings: DistributorSettings
): String {
    val sb = StringBuilder()
    sb.appendLine("Dear BRALIRWA PLC Order Desk,")
    sb.appendLine()
    sb.appendLine("Please process our automated stock replenishment Purchase Order:")
    sb.appendLine("PO Number: ${order.poNumber}")
    sb.appendLine("Distributor: ${settings.businessName}")
    sb.appendLine("TIN: ${settings.tin} | Phone: ${settings.phone}")
    sb.appendLine("Delivery Address: ${settings.address}")
    sb.appendLine("Trigger: ${if (order.triggeredByAlert) "Low Stock Alert Auto-Replenishment" else "Manual Depot Order"}")
    sb.appendLine()
    sb.appendLine("ITEMS ORDERED:")
    order.items.forEachIndexed { idx, item ->
        sb.appendLine("${idx + 1}. ${item.skuCode} - ${item.name}: ${item.qty} crates @ ${formatRwf(item.rateRwf)} = ${formatRwf(item.lineTotalRwf)}")
    }
    sb.appendLine()
    sb.appendLine("TOTAL CRATES: ${order.totalCrates}")
    sb.appendLine("ESTIMATED TOTAL COST: ${formatRwf(order.totalCostRwf)}")
    sb.appendLine()
    sb.appendLine("Regards,")
    sb.appendLine("${settings.activeStaffName} (${settings.activeStaffRole})")
    sb.appendLine(settings.businessName)
    return sb.toString()
}

fun buildRecurringBillingEmailBody(
    customer: Customer,
    settings: DistributorSettings
): String {
    return buildString {
        appendLine("Dear ${customer.name},")
        appendLine()
        appendLine("This is your ${customer.billingCycle.lowercase()} distributor billing summary from ${settings.businessName}.")
        appendLine("Customer TIN: ${customer.tin.ifBlank { "N/A" }} | Zone: ${customer.zone}")
        appendLine("Outstanding Credit Balance: ${formatRwf(customer.creditBalanceRwf)}")
        appendLine("Lifetime Purchases: ${formatRwf(customer.totalSpentRwf)}")
        appendLine()
        appendLine("Please settle via MTN MoMo, Airtel Money, or Bank Transfer and quote your customer name.")
        appendLine("Contact: ${settings.phone} | ${settings.email}")
    }
}

fun buildProductsCsv(products: List<Product>): String {
    val sb = StringBuilder()
    sb.appendLine("sku_code,name,category,sub_category,unit,cost_rwf,selling_rwf,stock_qty,reorder_level,age_days")
    products.forEach { p ->
        val age = StockAgingBucket.daysOld(p.batchReceivedEpochMs)
        sb.appendLine("\"${p.skuCode}\",\"${p.name}\",\"${p.category}\",\"${p.subCategory}\",\"${p.unitLabel}\",${p.costPriceRwf},${p.sellingPriceRwf},${p.stockQty},${p.reorderLevel},$age")
    }
    return sb.toString()
}

fun buildOrdersCsv(orders: List<SalesOrder>): String {
    val sb = StringBuilder()
    sb.appendLine("invoice_no,customer,staff,role,crates,subtotal_rwf,vat_rwf,total_rwf,paid_rwf,balance_rwf,method,status")
    orders.forEach { o ->
        sb.appendLine("\"${o.invoiceNumber}\",\"${o.customerName}\",\"${o.staffName}\",\"${o.staffRole}\",${o.totalCrates},${o.subtotalRwf},${o.vatRwf},${o.totalRwf},${o.paidRwf},${o.balanceRwf},\"${o.paymentMethod}\",\"${o.status}\"")
    }
    return sb.toString()
}

fun buildShipmentsCsv(shipments: List<ShipmentRecord>): String {
    val sb = StringBuilder()
    sb.appendLine("vehicle_plate,driver,zone,crates_loaded,revenue_rwf,cogs_rwf,trip_cost_rwf,net_profit_rwf,status")
    shipments.forEach { s ->
        sb.appendLine("\"${s.vehiclePlate}\",\"${s.driverName}\",\"${s.routeZone}\",${s.cratesLoaded},${s.revenueRwf},${s.cogsRwf},${s.fuelAndTripCostRwf},${s.netProfitRwf},\"${s.status}\"")
    }
    return sb.toString()
}

fun printBrandedA4Invoice(
    context: Context,
    order: SalesOrder,
    settings: DistributorSettings
) {
    val dateStr = formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())
    val rowsHtml = order.items.joinToString("") { item ->
        """
        <tr>
          <td>${escapeHtml(item.skuCode)}</td>
          <td>${escapeHtml(item.name)}</td>
          <td style="text-align:right">${item.qty}</td>
          <td style="text-align:right">${formatRwf(item.rateRwf)}</td>
          <td style="text-align:right"><strong>${formatRwf(item.lineTotalRwf)}</strong></td>
        </tr>
        """.trimIndent()
    }

    val html = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8" />
      <style>
        body { font-family: sans-serif; color: #1C1917; padding: 28px; font-size: 12px; }
        .header { display: flex; justify-content: space-between; border-bottom: 3px solid #9E1B1B; padding-bottom: 14px; margin-bottom: 18px; }
        .brand { color: #9E1B1B; font-size: 20px; font-weight: bold; }
        .badge { background: #9E1B1B; color: #fff; padding: 4px 10px; border-radius: 4px; font-weight: bold; display: inline-block; }
        table { width: 100%; border-collapse: collapse; margin-top: 16px; }
        th { background: #F3EFEA; text-align: left; padding: 8px; border-bottom: 2px solid #D6CEC4; }
        td { padding: 8px; border-bottom: 1px solid #EAE4DC; }
        .totals { margin-top: 18px; width: 45%; margin-left: auto; }
        .totals td { border: none; padding: 4px 8px; }
        .grand { font-size: 15px; font-weight: bold; color: #9E1B1B; border-top: 2px solid #9E1B1B !important; }
        .footer { margin-top: 32px; border-top: 1px dashed #D6CEC4; padding-top: 12px; font-size: 11px; color: #57534E; }
      </style>
    </head>
    <body>
      <div class="header">
        <div>
          <div class="brand">${escapeHtml(settings.businessName)}</div>
          <div>Official BRALIRWA Authorized Distributor</div>
          <div>${escapeHtml(settings.address)}</div>
          <div>Tel: ${escapeHtml(settings.phone)} | Email: ${escapeHtml(settings.email)}</div>
          <div>TIN: ${escapeHtml(settings.tin)}</div>
        </div>
        <div style="text-align:right">
          <div class="badge">TAX INVOICE (A4)</div>
          <p><strong>${escapeHtml(order.invoiceNumber)}</strong><br/>Date: $dateStr<br/>Status: ${escapeHtml(order.status)}</p>
        </div>
      </div>

      <p><strong>Bill To:</strong> ${escapeHtml(order.customerName)}<br/>
      <strong>Served By:</strong> ${escapeHtml(order.staffName)} (${escapeHtml(order.staffRole)}) | <strong>Payment:</strong> ${escapeHtml(order.paymentMethod)}</p>

      <table>
        <thead>
          <tr><th>SKU</th><th>Product</th><th style="text-align:right">Qty</th><th style="text-align:right">Unit Rate</th><th style="text-align:right">Line Total</th></tr>
        </thead>
        <tbody>$rowsHtml</tbody>
      </table>

      <table class="totals">
        <tr><td>Total Crates:</td><td style="text-align:right">${order.totalCrates}</td></tr>
        <tr><td>Subtotal:</td><td style="text-align:right">${formatRwf(order.subtotalRwf)}</td></tr>
        <tr><td>VAT (${settings.vatRate}%):</td><td style="text-align:right">${formatRwf(order.vatRwf)}</td></tr>
        <tr class="grand"><td>TOTAL:</td><td style="text-align:right">${formatRwf(order.totalRwf)}</td></tr>
        <tr><td>Paid:</td><td style="text-align:right">${formatRwf(order.paidRwf)}</td></tr>
        <tr><td>Balance Due:</td><td style="text-align:right">${formatRwf(order.balanceRwf)}</td></tr>
      </table>

      <div class="footer">
        Thank you for partnering with ${escapeHtml(settings.businessName)}. Empty RGB crates remain distributor property unless deposit settled.
      </div>
    </body>
    </html>
    """.trimIndent()

    launchWebPrint(context, "Invoice_${order.invoiceNumber}", html, PrintAttributes.MediaSize.ISO_A4)
}

fun printThermalReceipt(
    context: Context,
    order: SalesOrder,
    settings: DistributorSettings
) {
    val widthPx = if (settings.thermalPaperMm == 58) "210px" else "290px"
    val dateStr = formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())
    val itemsHtml = order.items.joinToString("") { item ->
        """
        <div style="margin-bottom:4px">
          <div><strong>${escapeHtml(item.name)}</strong></div>
          <div style="display:flex;justify-content:space-between">
            <span>${item.qty} x ${formatRwf(item.rateRwf)}</span>
            <span>${formatRwf(item.lineTotalRwf)}</span>
          </div>
        </div>
        """.trimIndent()
    }

    val html = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8" />
      <style>
        body { font-family: monospace; width: $widthPx; margin: 0 auto; font-size: 11px; color: #000; }
        .center { text-align: center; }
        .hr { border-top: 1px dashed #000; margin: 6px 0; }
        .row { display: flex; justify-content: space-between; }
      </style>
    </head>
    <body>
      <div class="center">
        <strong>${escapeHtml(settings.businessName)}</strong><br/>
        BRALIRWA DISTRIBUTOR<br/>
        ${escapeHtml(settings.address)}<br/>
        Tel: ${escapeHtml(settings.phone)}<br/>
        TIN: ${escapeHtml(settings.tin)}
      </div>
      <div class="hr"></div>
      <div>Inv: ${escapeHtml(order.invoiceNumber)}</div>
      <div>Date: $dateStr</div>
      <div>Cust: ${escapeHtml(order.customerName)}</div>
      <div>Cashier: ${escapeHtml(order.staffName)}</div>
      <div class="hr"></div>
      $itemsHtml
      <div class="hr"></div>
      <div class="row"><span>Subtotal:</span><span>${formatRwf(order.subtotalRwf)}</span></div>
      <div class="row"><span>VAT (${settings.vatRate}%):</span><span>${formatRwf(order.vatRwf)}</span></div>
      <div class="row"><strong>TOTAL:</strong><strong>${formatRwf(order.totalRwf)}</strong></div>
      <div class="row"><span>Paid (${escapeHtml(order.paymentMethod)}):</span><span>${formatRwf(order.paidRwf)}</span></div>
      <div class="row"><span>Balance:</span><span>${formatRwf(order.balanceRwf)}</span></div>
      <div class="hr"></div>
      <div class="center">MURAKOZE • THANK YOU!</div>
    </body>
    </html>
    """.trimIndent()

    launchWebPrint(context, "Receipt_${order.invoiceNumber}", html, PrintAttributes.MediaSize.ISO_A5)
}

fun printBrandedReportA4(
    context: Context,
    reportTitle: String,
    rowsHtml: String,
    settings: DistributorSettings
) {
    val html = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8" />
      <style>
        body { font-family: sans-serif; color: #1C1917; padding: 24px; font-size: 12px; }
        h2 { color: #9E1B1B; margin-bottom: 4px; }
        table { width: 100%; border-collapse: collapse; margin-top: 14px; }
        th { background: #F3EFEA; text-align: left; padding: 8px; border-bottom: 2px solid #9E1B1B; }
        td { padding: 7px 8px; border-bottom: 1px solid #EAE4DC; }
      </style>
    </head>
    <body>
      <h2>${escapeHtml(settings.businessName)} — ${escapeHtml(reportTitle)}</h2>
      <div>${escapeHtml(settings.address)} | Tel: ${escapeHtml(settings.phone)} | Generated: ${formatDateTime(System.currentTimeMillis())}</div>
      $rowsHtml
    </body>
    </html>
    """.trimIndent()
    launchWebPrint(context, "Report_${reportTitle.replace(" ", "_")}", html, PrintAttributes.MediaSize.ISO_A4)
}

private fun launchWebPrint(
    context: Context,
    jobName: String,
    html: String,
    mediaSize: PrintAttributes.MediaSize
) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val adapter = view.createPrintDocumentAdapter(jobName)
            val attrs = PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .build()
            printManager.print(jobName, adapter, attrs)
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
