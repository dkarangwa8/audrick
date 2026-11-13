package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.ExpenseRecord
import com.example.data.model.Product
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BralirwaCrimson
import com.example.ui.theme.CrimsonCritical
import com.example.ui.theme.EmeraldFresh
import com.example.ui.theme.LagerGold
import com.example.util.formatCompactRwf

@Composable
fun DailySalesLineChart(
    dailyValues: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val data = if (dailyValues.isEmpty()) listOf("Mon" to 420_000.0, "Tue" to 580_000.0, "Wed" to 510_000.0, "Thu" to 740_000.0, "Fri" to 890_000.0, "Sat" to 1_120_000.0, "Today" to 960_000.0) else dailyValues
    val maxVal = (data.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val lineColor = BralirwaCrimson
    val pointColor = LagerGold
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height
            val stepX = if (data.size > 1) w / (data.size - 1) else w

            // Horizontal dashed-like grid lines
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5f
                )
            }

            val points = data.mapIndexed { idx, pair ->
                val x = idx * stepX
                val ratio = (pair.second / maxVal).toFloat().coerceIn(0f, 1f)
                val y = h - (ratio * (h * 0.85f)) - (h * 0.06f)
                Offset(x, y)
            }

            if (points.size >= 2) {
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.34f),
                            LagerGold.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            points.forEach { pt ->
                drawCircle(color = Color.White, radius = 8f, center = pt)
                drawCircle(color = pointColor, radius = 5f, center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (day, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCompactRwf(value), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StockStatusVerticalBarChart(
    products: List<Product>,
    modifier: Modifier = Modifier
) {
    val subCats = products.groupBy { it.subCategory }.entries.take(5).toList()
    val maxCrates = (subCats.maxOfOrNull { entry -> entry.value.sumOf { it.stockQty } } ?: 1).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (subCats.isEmpty()) return@Canvas
            val barWidth = (size.width / subCats.size) * 0.46f
            val slot = size.width / subCats.size

            subCats.forEachIndexed { idx, (_, list) ->
                val total = list.sumOf { it.stockQty }
                val hasCritical = list.any { it.isCriticalStock }
                val hasLow = list.any { it.isLowStock }
                val barColor = when {
                    hasCritical -> CrimsonCritical
                    hasLow -> AmberAlert
                    else -> EmeraldFresh
                }
                val barHeight = (total.toFloat() / maxCrates) * (size.height * 0.88f)
                val x = idx * slot + (slot - barWidth) / 2f
                val y = size.height - barHeight

                drawRoundRect(
                    color = barColor.copy(alpha = 0.14f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.72f))),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.coerceAtLeast(10f)),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            subCats.forEach { (subCat, list) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = subCat.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${list.sumOf { it.stockQty }}c",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TopCustomersHorizontalBarChart(
    customers: List<Customer>,
    modifier: Modifier = Modifier
) {
    val top = customers.sortedByDescending { it.totalSpentRwf }.take(5)
    val maxSpend = (top.maxOfOrNull { it.totalSpentRwf } ?: 1.0).coerceAtLeast(1.0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        top.forEachIndexed { idx, c ->
            val ratio = (c.totalSpentRwf / maxSpend).toFloat().coerceIn(0.05f, 1f)
            val barColor = if (idx == 0) BralirwaCrimson else if (idx == 1) LagerGold else EmeraldFresh
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = c.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "RWF ${formatCompactRwf(c.totalSpentRwf)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseBreakdownDonutChart(
    expenses: List<ExpenseRecord>,
    modifier: Modifier = Modifier
) {
    val grouped = expenses.groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amountRwf } }
        .entries
        .sortedByDescending { it.value }
    val total = grouped.sumOf { it.value }.coerceAtLeast(1.0)
    val palette = listOf(BralirwaCrimson, LagerGold, EmeraldFresh, Color(0xFF0284C7), Color(0xFF7C3AED))

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(136.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(124.dp)) {
                val stroke = 24f
                var startAngle = -90f
                if (grouped.isEmpty()) {
                    drawArc(
                        color = Color.LightGray,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                } else {
                    grouped.forEachIndexed { idx, entry ->
                        val sweep = ((entry.value / total) * 360.0).toFloat()
                        drawArc(
                            color = palette[idx % palette.size],
                            startAngle = startAngle,
                            sweepAngle = (sweep - 3f).coerceAtLeast(2f),
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatCompactRwf(grouped.sumOf { it.value }),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            grouped.forEachIndexed { idx, entry ->
                val pct = ((entry.value / total) * 100.0).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(palette[idx % palette.size])
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = entry.key.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
