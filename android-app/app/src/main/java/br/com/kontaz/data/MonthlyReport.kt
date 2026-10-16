package br.com.kontaz.data

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val reportLocale = Locale("pt", "BR")
private val reportDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun createMonthlyCsv(transactions: List<Transaction>, month: YearMonth): ByteArray {
    val rows = buildList {
        add(listOf("Data", "Tipo", "Categoria", "Descrição", "Valor (R$)"))
        transactions.forEach { transaction ->
            add(
                listOf(
                    transaction.date.toReportDate(),
                    transaction.type.toReportLabel(),
                    transaction.category,
                    transaction.description.orEmpty(),
                    String.format(reportLocale, "%.2f", transaction.amount)
                )
            )
        }
        add(emptyList())
        add(listOf("Resumo de ${month.toReportLabel()}"))
        val income = transactions.sumOf { if (it.type == "income") it.amount else 0.0 }
        val expenses = transactions.sumOf { if (it.type == "expense") it.amount else 0.0 }
        val investments = transactions.sumOf { if (it.type == "investment") it.amount else 0.0 }
        add(listOf("Receitas", "", "", "", String.format(reportLocale, "%.2f", income)))
        add(listOf("Despesas", "", "", "", String.format(reportLocale, "%.2f", expenses)))
        add(listOf("Investimentos", "", "", "", String.format(reportLocale, "%.2f", investments)))
        add(listOf("Saldo restante", "", "", "", String.format(reportLocale, "%.2f", income - expenses - investments)))
    }
    val content = rows.joinToString("\r\n") { row -> row.joinToString(";") { it.toCsvCell() } }
    return ("\uFEFF$content\r\n").toByteArray(Charsets.UTF_8)
}

fun createMonthlyPdf(transactions: List<Transaction>, month: YearMonth): ByteArray {
    val document = PdfDocument()
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(16, 24, 40)
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(52, 64, 84)
        textSize = 10f
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(102, 112, 133)
        textSize = 9f
    }
    val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(16, 24, 40)
        textSize = 10f
        textAlign = Paint.Align.RIGHT
    }
    val income = transactions.sumOf { if (it.type == "income") it.amount else 0.0 }
    val expenses = transactions.sumOf { if (it.type == "expense") it.amount else 0.0 }
    val investments = transactions.sumOf { if (it.type == "investment") it.amount else 0.0 }
    val balance = income - expenses - investments
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var y = 0f

    fun startPage() {
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        val canvas = requireNotNull(page).canvas
        y = 46f
        canvas.drawText("Kontaz - Relatório mensal", 40f, y, titlePaint)
        y += 22f
        canvas.drawText(month.toReportLabel(), 40f, y, bodyPaint)
        y += 24f
        if (pageNumber == 1) {
            canvas.drawText("Receitas: ${income.toReportCurrency()}", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText("Despesas: ${expenses.toReportCurrency()}", 40f, y, bodyPaint)
            y += 16f
            canvas.drawText("Investimentos: ${investments.toReportCurrency()}", 40f, y, bodyPaint)
            y += 24f
            canvas.drawText("Saldo restante: ${balance.toReportCurrency()}", 40f, y, bodyPaint)
            y += 24f
        }
        canvas.drawText("Data / tipo", 40f, y, mutedPaint)
        canvas.drawText("Valor", 555f, y, amountPaint)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, mutedPaint)
        y += 16f
    }

    try {
        startPage()
        transactions.forEach { transaction ->
            if (y > 790f) {
                document.finishPage(requireNotNull(page))
                startPage()
            }
            val canvas = requireNotNull(page).canvas
            canvas.drawText(
                "${transaction.date.toReportDate()}  ·  ${transaction.type.toReportLabel()}",
                40f,
                y,
                bodyPaint
            )
            canvas.drawText(transaction.amount.toReportCurrency(), 555f, y, amountPaint)
            y += 14f
            canvas.drawText(
                "${transaction.category} · ${transaction.description.orEmpty()}".take(88),
                40f,
                y,
                mutedPaint
            )
            y += 22f
        }
        if (transactions.isEmpty()) {
            requireNotNull(page).canvas.drawText("Nenhuma transação neste mês.", 40f, y, bodyPaint)
        }
        requireNotNull(page).canvas.drawText("Página $pageNumber", 500f, 820f, mutedPaint)
        document.finishPage(requireNotNull(page))

        return ByteArrayOutputStream().use { output ->
            document.writeTo(output)
            output.toByteArray()
        }
    } finally {
        document.close()
    }
}

private fun String.toCsvCell(): String =
    if (any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${replace("\"", "\"\"")}\""
    } else {
        this
    }

private fun String.toReportDate(): String =
    runCatching { java.time.LocalDate.parse(this).format(reportDateFormatter) }.getOrDefault(this)

private fun String.toReportLabel(): String = when (this) {
    "income" -> "Receita"
    "expense" -> "Despesa"
    "investment" -> "Investimento"
    else -> this
}

private fun YearMonth.toReportLabel(): String =
    format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", reportLocale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(reportLocale) else it.toString() }

private fun Double.toReportCurrency(): String =
    String.format(reportLocale, "R$ %,.2f", this)
