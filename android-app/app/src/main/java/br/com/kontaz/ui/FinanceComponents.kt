package br.com.kontaz.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.kontaz.data.Dashboard
import br.com.kontaz.data.Transaction
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

object KontazColors {
    val Background = Color(0xFFF7F9FC)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFE8EDF3)
    val Text = Color(0xFF101828)
    val Muted = Color(0xFF667085)
    val Green = Color(0xFF00BF63)
    val GreenSoft = Color(0xFFE1F6EB)
    val Orange = Color(0xFFFF6B00)
    val OrangeSoft = Color(0xFFFFEFE4)
    val Blue = Color(0xFF18A8E0)
    val BlueSoft = Color(0xFFE1F4FC)
    val Red = Color(0xFFE5484D)
}

@Composable
fun FinanceScaffold(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onAddTransaction: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = KontazColors.Background,
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
                onAddTransaction = onAddTransaction
            )
        },
        content = content
    )
}

@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onAddTransaction: () -> Unit
) {
    Surface(
        color = KontazColors.Surface,
        shadowElevation = 14.dp,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().border(1.dp, KontazColors.Border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavigationItem(
                label = "Início",
                selected = selectedTab == "dashboard",
                icon = { tint -> Icon(Icons.Outlined.Home, contentDescription = null, tint = tint) },
                onClick = { onSelectTab("dashboard") }
            )
            NavigationItem(
                label = "Metas",
                selected = selectedTab == "goals",
                icon = { tint -> Icon(Icons.Outlined.Flag, contentDescription = null, tint = tint) },
                onClick = { onSelectTab("goals") }
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    color = KontazColors.Green,
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(50.dp).offset(y = (-4).dp).clickable(onClick = onAddTransaction)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Add, contentDescription = "Adicionar transação", tint = Color.White)
                    }
                }
            }
            NavigationItem(
                label = "Transações",
                selected = selectedTab == "history",
                icon = { tint -> Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = tint) },
                onClick = { onSelectTab("history") }
            )
            NavigationItem(
                label = "Calculadora",
                selected = selectedTab == "calculators",
                icon = { tint -> Icon(Icons.Outlined.Calculate, contentDescription = null, tint = tint) },
                onClick = { onSelectTab("calculators") }
            )
        }
    }
}

@Composable
private fun RowScope.NavigationItem(
    label: String,
    selected: Boolean,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit
) {
    val tint = if (selected) KontazColors.Green else KontazColors.Muted
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        icon(tint)
        Text(
            label,
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Box(
            Modifier.size(4.dp).clip(CircleShape)
                .background(if (selected) KontazColors.Green else Color.Transparent)
        )
    }
}

@Composable
fun HomeDashboard(
    displayName: String,
    month: YearMonth,
    dashboard: Dashboard?,
    transactions: List<Transaction>,
    loading: Boolean,
    message: String?,
    onMonthChange: (YearMonth) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeHeader(displayName = displayName, onLogout = onLogout, onOpenProfile = onOpenProfile)
        }
        item {
            MonthSelector(month = month, onMonthChange = onMonthChange)
        }
        if (message != null) {
            item { InlineMessage(message = message, onRetry = onRefresh) }
        }
        if (loading && dashboard == null) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = KontazColors.Green) } }
        }
        dashboard?.let { data ->
            item {
                BalanceCard(balance = data.balance, month = month)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Resumo do mês", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = KontazColors.Text)
                    MonthlySummary(data)
                }
            }
            item { SpendingChart(month = month, transactions = transactions) }
            item { OverviewCard(data) }
            item { CategoryBreakdown(transactions) }
            item { InvestmentCategoriesCard(transactions) }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transações recentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = KontazColors.Text)
                TextButton(onClick = onRefresh) { Text("Atualizar", color = KontazColors.Muted) }
            }
        }
        if (transactions.isEmpty() && !loading && message == null) {
            item { EmptyState("Nenhuma transação neste mês", "Seus lançamentos aparecerão aqui.") }
        } else {
            items(transactions.take(5), key = { it.id }) { transaction ->
                TransactionCard(transaction = transaction, onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

@Composable
fun TransactionsHistory(
    month: YearMonth,
    transactions: List<Transaction>,
    loading: Boolean,
    message: String?,
    selectedType: String?,
    onTypeChange: (String?) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().background(KontazColors.Background).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Transações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KontazColors.Text)
            Text("Consulte seus lançamentos", style = MaterialTheme.typography.bodyMedium, color = KontazColors.Muted)
            MonthSelector(month = month, onMonthChange = onMonthChange)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null to "Todas", "income" to "Receitas", "expense" to "Despesas", "investment" to "Investimentos").forEach { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onTypeChange(type) },
                        label = { Text(label) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                message?.let { Text(it, modifier = Modifier.weight(1f), color = KontazColors.Red, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = onRefresh) { Text("Atualizar", color = KontazColors.Green) }
            }
        }
        if (loading && transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KontazColors.Green)
            }
        } else if (transactions.isEmpty()) {
            EmptyState(
                "Nenhuma transação encontrada",
                if (selectedType == null) "Não há lançamentos em ${month.toDisplayName()}." else "Tente outro tipo ou período."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionCard(transaction = transaction, onEdit = onEdit, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(displayName: String, onLogout: () -> Unit, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Olá,", style = MaterialTheme.typography.bodyMedium, color = KontazColors.Muted)
            Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KontazColors.Text)
        }
        HeaderAction(Icons.Outlined.PersonOutline, "Perfil", onClick = onOpenProfile)
        Spacer(Modifier.width(10.dp))
        HeaderAction(Icons.AutoMirrored.Outlined.Logout, "Sair", onClick = onLogout)
    }
}

@Composable
private fun HeaderAction(
    image: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        color = KontazColors.Surface,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, KontazColors.Border),
        modifier = Modifier.size(44.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(image, contentDescription = description, tint = KontazColors.Text, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
fun MonthSelector(month: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Surface(
        color = KontazColors.Surface,
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KontazColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.height(54.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mês anterior", tint = KontazColors.Muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = KontazColors.Green, modifier = Modifier.size(19.dp))
                Text(month.toDisplayName(), color = KontazColors.Text, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Próximo mês", tint = KontazColors.Muted)
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Double, month: YearMonth) {
    val negative = balance < 0
    val accent = if (negative) KontazColors.Orange else KontazColors.Green
    FinanceCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (negative) Color(0xFFFFC9A5) else Color(0xFFBCEBD1),
        contentPadding = 20.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundIcon(
                    icon = if (negative) Icons.Outlined.ArrowDownward else Icons.Outlined.Savings,
                    tint = accent,
                    background = if (negative) KontazColors.OrangeSoft else KontazColors.GreenSoft,
                    size = 42.dp
                )
                Column {
                    Text("Saldo restante", style = MaterialTheme.typography.titleSmall, color = KontazColors.Text, fontWeight = FontWeight.SemiBold)
                    Text(month.remainingDaysLabel(), style = MaterialTheme.typography.bodySmall, color = KontazColors.Muted)
                }
            }
            Text(
                formatCurrency(balance, withSign = true),
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            if (negative) {
                Surface(color = KontazColors.OrangeSoft, shape = RoundedCornerShape(18.dp)) {
                    Text(
                        "⚠ Suas despesas ultrapassaram suas receitas. Revise seus gastos.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = KontazColors.Orange
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlySummary(data: Dashboard) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SummaryMetric("Receitas", data.income, KontazColors.Green, KontazColors.GreenSoft, Icons.Outlined.ArrowUpward)
        SummaryMetric("Despesas", data.expenses, KontazColors.Orange, KontazColors.OrangeSoft, Icons.Outlined.ArrowDownward)
        SummaryMetric("Investimentos", data.investments, KontazColors.Blue, KontazColors.BlueSoft, Icons.Outlined.Savings)
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    amount: Double,
    tint: Color,
    background: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier.width(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RoundIcon(icon, tint, background, size = 40.dp)
        Text(label, color = KontazColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatCurrency(amount), color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SpendingChart(month: YearMonth, transactions: List<Transaction>) {
    val expensesByDay = transactions
        .filter { it.type == "expense" && it.date.startsWith(month.toString()) }
        .groupBy { it.date.takeLast(2).toIntOrNull() ?: 1 }
        .mapValues { (_, values) -> values.sumOf { it.amount } }
    val cumulative = (1..month.lengthOfMonth()).runningFold(0.0) { total, day -> total + (expensesByDay[day] ?: 0.0) }.drop(1)

    FinanceCard(contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardTitle(Icons.AutoMirrored.Outlined.ShowChart, "Gastos acumulados", KontazColors.Orange)
            if (cumulative.maxOrNull() == null || cumulative.maxOrNull() == 0.0) {
                EmptyChartPlaceholder("Sem despesas registradas no mês")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Valor acumulado (R$)", color = KontazColors.Muted, style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    val maxValue = cumulative.maxOrNull() ?: 0.0
                    Column(
                        modifier = Modifier.height(138.dp).padding(end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        (4 downTo 0).forEach { step ->
                            Text(
                                formatAxisValue(maxValue * step / 4),
                                style = MaterialTheme.typography.labelSmall,
                                color = KontazColors.Muted
                            )
                        }
                    }
                    CumulativeLineChart(cumulative, Modifier.weight(1f))
                    }
                }
                val labels = listOf(1, 5, 10, 15, 20, 25, month.lengthOfMonth()).distinct()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Spacer(Modifier.width(34.dp))
                    labels.forEach { day ->
                        Text(day.toString(), color = KontazColors.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(
                    "Dia do mês",
                    modifier = Modifier.fillMaxWidth(),
                    color = KontazColors.Muted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun CumulativeLineChart(values: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(138.dp).padding(top = 6.dp)) {
        val left = 2.dp.toPx()
        val right = size.width - 2.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 4.dp.toPx()
        val plotHeight = bottom - top
        val maxValue = max(values.maxOrNull() ?: 0.0, 1.0)

        for (i in 0..4) {
            val y = top + plotHeight * i / 4
            drawLine(KontazColors.Border, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(right, y), 1.dp.toPx())
        }
        val points = values.mapIndexed { index, value ->
            val x = if (values.size <= 1) left else left + (right - left) * index / (values.size - 1)
            val y = bottom - (value / maxValue).toFloat() * plotHeight
            androidx.compose.ui.geometry.Offset(x, y)
        }
        if (points.isNotEmpty()) {
            val fill = Path().apply {
                moveTo(points.first().x, bottom)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, bottom)
                close()
            }
            drawPath(fill, KontazColors.Orange.copy(alpha = 0.16f))
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(line, KontazColors.Orange, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun OverviewCard(data: Dashboard) {
    val rows = listOf(
        Triple("Receitas", data.income, KontazColors.Green),
        Triple("Despesas", data.expenses, KontazColors.Orange),
        Triple("Investimentos", data.investments, KontazColors.Blue)
    )
    val maxValue = max(rows.maxOf { it.second }, 1.0)
    FinanceCard(contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            CardTitle(Icons.AutoMirrored.Outlined.ShowChart, "Visão geral", KontazColors.Green)
            rows.forEach { (label, value, tint) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = KontazColors.Muted)
                        Text(formatCompactCurrency(value), style = MaterialTheme.typography.labelSmall, color = KontazColors.Text)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Color(0xFFF0F2F5))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth((value / maxValue).toFloat().coerceIn(0f, 1f))
                                .height(12.dp).clip(CircleShape).background(tint)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(transactions: List<Transaction>) {
    val totals = transactions.filter { it.type == "expense" }
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }.take(5)
    val total = totals.sumOf { it.value }
    val segments = totals.zip(categoryColors(investment = false))

    FinanceCard(contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CardTitle(Icons.Outlined.Home, "Gasto por categoria", KontazColors.Green)
            if (total <= 0.0) {
                EmptyChartPlaceholder("Sem despesas por categoria")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Canvas(modifier = Modifier.size(118.dp)) {
                        var startAngle = -90f
                        segments.forEach { (entry, color) ->
                            val sweep = (entry.value / total * 360f).toFloat()
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        segments.forEach { (entry, color) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                Text(
                                    entry.key,
                                    modifier = Modifier.weight(1f),
                                    color = KontazColors.Muted,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("${(entry.value / total * 100).toInt()}%", color = KontazColors.Text, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentCategoriesCard(transactions: List<Transaction>) {
    val totals = transactions.filter { it.type == "investment" }
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }.take(5)
    val total = totals.sumOf { it.value }
    val segments = totals.zip(categoryColors(investment = true))

    FinanceCard(contentPadding = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardTitle(Icons.Outlined.Savings, "Investimentos por categoria", KontazColors.Green)
            if (total <= 0.0) {
                EmptyChartPlaceholder("Sem investimentos registrados")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Canvas(modifier = Modifier.size(118.dp)) {
                        var startAngle = -90f
                        segments.forEach { (entry, color) ->
                            val sweep = (entry.value / total * 360f).toFloat()
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        segments.forEach { (entry, color) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                Text(
                                    entry.key,
                                    modifier = Modifier.weight(1f),
                                    color = KontazColors.Muted,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("${(entry.value / total * 100).toInt()}%", color = KontazColors.Text, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun categoryColors(investment: Boolean): List<Color> =
    if (investment) {
        listOf(
            Color(0xFF1565C0),
            Color(0xFF18A8E0),
            Color(0xFF5B8DEF),
            Color(0xFF3949AB),
            Color(0xFF7E57C2)
        )
    } else {
        listOf(
            KontazColors.Red,
            Color(0xFFB4232F),
            Color(0xFFEF7C7C),
            Color(0xFF8B5CF6),
            Color(0xFFF2B544)
        )
    }

@Composable
private fun TransactionCard(
    transaction: Transaction,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    val tint = when (transaction.type) {
        "income" -> KontazColors.Green
        "investment" -> KontazColors.Blue
        else -> KontazColors.Orange
    }
    val soft = when (transaction.type) {
        "income" -> KontazColors.GreenSoft
        "investment" -> KontazColors.BlueSoft
        else -> KontazColors.OrangeSoft
    }
    val icon = when (transaction.type) {
        "income" -> Icons.Outlined.ArrowUpward
        "investment" -> Icons.Outlined.Savings
        else -> Icons.Outlined.ArrowDownward
    }
    FinanceCard(contentPadding = 12.dp) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundIcon(icon, tint, soft, size = 40.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(transaction.description?.takeIf(String::isNotBlank) ?: transaction.category, color = KontazColors.Text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${transactionLabel(transaction.type)} · ${transaction.date.toDisplayDate()}", color = KontazColors.Muted, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                formatCurrency(
                    if (transaction.type == "income") transaction.amount else -transaction.amount,
                    withSign = true
                ),
                color = tint,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            IconButton(onClick = { onEdit(transaction) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = KontazColors.Muted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { onDelete(transaction) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir", tint = KontazColors.Orange, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = KontazColors.Text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = KontazColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyChartPlaceholder(text: String) {
    Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
        Text(text, color = KontazColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InlineMessage(message: String, onRetry: () -> Unit) {
    Surface(color = Color(0xFFFFF4F2), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = KontazColors.Red, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

@Composable
private fun CardTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Text(title, color = KontazColors.Text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    background: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Surface(color = background, shape = CircleShape, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.48f))
        }
    }
}

@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    borderColor: Color = KontazColors.Border,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = KontazColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

fun YearMonth.toDisplayName(): String =
    format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }

private fun YearMonth.remainingDaysLabel(): String {
    val today = LocalDate.now()
    return if (this == YearMonth.from(today)) {
        val remaining = (lengthOfMonth() - today.dayOfMonth).coerceAtLeast(0)
        "$remaining dias restantes no mês"
    } else "Resumo de ${toDisplayName().lowercase(Locale("pt", "BR"))}"
}

private fun String.toDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(this)

private fun formatCurrency(value: Double, withSign: Boolean = false): String {
    val formatted = String.format(Locale("pt", "BR"), "%,.2f", kotlin.math.abs(value))
    val sign = when {
        withSign && value < 0 -> "-"
        withSign && value > 0 -> "+"
        else -> ""
    }
    return "${sign}R$ $formatted"
}

private fun formatCompactCurrency(value: Double): String =
    if (value >= 1000) String.format(Locale("pt", "BR"), "R$ %.1fk", value / 1000)
    else String.format(Locale("pt", "BR"), "R$ %.0f", value)

private fun formatAxisValue(value: Double): String =
    if (value >= 1000) String.format(Locale("pt", "BR"), "%.1fk", value / 1000)
    else String.format(Locale("pt", "BR"), "%.0f", value)

private fun transactionLabel(type: String): String = when (type) {
    "income" -> "Receita"
    "investment" -> "Investimento"
    else -> "Despesa"
}
