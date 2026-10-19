package com.example.ui

import com.example.ui.theme.*
import android.content.Context
import android.content.Intent
import android.util.Log
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.Expense
import com.example.data.Money
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DataExporter"

object DataExporter {

    fun sharePdfReport(
        context: Context,
        expenses: List<Expense>,
        dateRangeStr: String,
        typeFilterStr: String,
        categoryFilterStr: String,
        amountSaved: Double = 0.0,
        monthsOverBudget: Int = 0,
        includeDetailedTxns: Boolean = true,
        currencyCode: String = "INR"
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var pageNumber = 1
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val gridPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val headerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10f
        }
        val sectionTitlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderBgPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val tableHeaderTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9f
        }
        val incomePaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val expensePaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw Header Banner on Page 1
        canvas.drawRect(0f, 0f, 595f, 90f, headerBgPaint)
        canvas.drawText("FINANCE LEDGER STATEMENT", 30f, 38f, headerTitlePaint)
        canvas.drawText("Period: $dateRangeStr   |   Type: $typeFilterStr", 30f, 58f, headerSubPaint)
        canvas.drawText("Categories: $categoryFilterStr", 30f, 72f, headerSubPaint)
        canvas.drawText(
            "Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}",
            380f,
            72f,
            headerSubPaint
        )

        // Calculate Totals
        val totalIncome = expenses.realIncome(currencyCode)
        val totalExpense = expenses.realExpense(currencyCode)
        val netBalance = totalIncome - totalExpense
        val totalCashFlown = totalIncome + totalExpense
        val totalTxCount = expenses.size
        val activeDaysCount = expenses.map { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)) }.distinct().size

        // KPI Cards Summary Box
        var y = 105f
        canvas.drawText("ANALYTICAL STATISTICS & SUMMARY", 30f, y, sectionTitlePaint)
        y += 12f

        // 6 Cards across 2 Rows
        val cardWidth = 165f
        val cardHeight = 42f

        // --- ROW 1 ---
        // Card 1: Total Income
        val fillIncome = Paint().apply { color = Color.parseColor("#ECFDF5"); style = Paint.Style.FILL }
        canvas.drawRoundRect(30f, y, 30f + cardWidth, y + cardHeight, 6f, 6f, fillIncome)
        canvas.drawRoundRect(30f, y, 30f + cardWidth, y + cardHeight, 6f, 6f, gridPaint)
        canvas.drawText("Total Income", 40f, y + 16f, Paint().apply { color = Color.parseColor("#065F46"); textSize = 8f })
        canvas.drawText("+₹${String.format(Locale.getDefault(), "%,.2f", totalIncome)}", 40f, y + 32f, Paint().apply {
            color = Color.parseColor("#059669"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        // Card 2: Total Expense
        val fillExpense = Paint().apply { color = Color.parseColor("#FEF2F2"); style = Paint.Style.FILL }
        canvas.drawRoundRect(210f, y, 210f + cardWidth, y + cardHeight, 6f, 6f, fillExpense)
        canvas.drawRoundRect(210f, y, 210f + cardWidth, y + cardHeight, 6f, 6f, gridPaint)
        canvas.drawText("Total Expense", 220f, y + 16f, Paint().apply { color = Color.parseColor("#991B1B"); textSize = 8f })
        canvas.drawText("-₹${String.format(Locale.getDefault(), "%,.2f", totalExpense)}", 220f, y + 32f, Paint().apply {
            color = Color.parseColor("#DC2626"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        // Card 3: Net Cashflow
        val netColorHex = if (netBalance >= 0) "#1E40AF" else "#991B1B"
        val netBgHex = if (netBalance >= 0) "#EFF6FF" else "#FEF2F2"
        val fillNet = Paint().apply { color = Color.parseColor(netBgHex); style = Paint.Style.FILL }
        canvas.drawRoundRect(390f, y, 390f + cardWidth, y + cardHeight, 6f, 6f, fillNet)
        canvas.drawRoundRect(390f, y, 390f + cardWidth, y + cardHeight, 6f, 6f, gridPaint)
        canvas.drawText("Net Cashflow", 400f, y + 16f, Paint().apply { color = Color.parseColor(netColorHex); textSize = 8f })
        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", netBalance)}", 400f, y + 32f, Paint().apply {
            color = Color.parseColor(netColorHex); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        y += cardHeight + 8f

        // --- ROW 2 ---
        // Card 4: Total Cash Flown
        val fillPurple = Paint().apply { color = Color.parseColor("#F3E8FF"); style = Paint.Style.FILL }
        canvas.drawRoundRect(30f, y, 30f + cardWidth, y + cardHeight, 6f, 6f, fillPurple)
        canvas.drawRoundRect(30f, y, 30f + cardWidth, y + cardHeight, 6f, 6f, gridPaint)
        canvas.drawText("Total Cash Flown", 40f, y + 16f, Paint().apply { color = Color.parseColor("#6B21A8"); textSize = 8f })
        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", totalCashFlown)}", 40f, y + 32f, Paint().apply {
            color = Color.parseColor("#7E22CE"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        // Card 5: Transactions Done
        val fillAmber = Paint().apply { color = Color.parseColor("#FEF3C7"); style = Paint.Style.FILL }
        canvas.drawRoundRect(210f, y, 210f + cardWidth, y + cardHeight, 6f, 6f, fillAmber)
        canvas.drawRoundRect(210f, y, 210f + cardWidth, y + cardHeight, 6f, 6f, gridPaint)
        canvas.drawText("Transactions Done", 220f, y + 16f, Paint().apply { color = Color.parseColor("#92400E"); textSize = 8f })
        canvas.drawText("$totalTxCount txns", 220f, y + 32f, Paint().apply {
            color = Color.parseColor("#D97706"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        // Card 6: Active Days
        val fillCyan = Paint().apply { color = Color.parseColor("#E0F2FE"); style = Paint.Style.FILL }
        canvas.drawRoundRect(390f, y, 390f + cardWidth, y + cardHeight, 6f, 6f, fillCyan)
        canvas.drawRoundRect(390f, y, 390f + cardWidth, y + cardHeight, 6f, 6f, gridPaint)
        canvas.drawText("Active Days", 400f, y + 16f, Paint().apply { color = Color.parseColor("#075985"); textSize = 8f })
        canvas.drawText("$activeDaysCount days", 400f, y + 32f, Paint().apply {
            color = Color.parseColor("#0284C7"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        y += cardHeight + 8f

        // --- ROW 3 ---
        val wideCardWidth = 252f
        // Card 7: Amount Saved
        val fillTeal = Paint().apply { color = Color.parseColor("#CCFBF1"); style = Paint.Style.FILL }
        canvas.drawRoundRect(30f, y, 30f + wideCardWidth, y + cardHeight, 6f, 6f, fillTeal)
        canvas.drawRoundRect(30f, y, 30f + wideCardWidth, y + cardHeight, 6f, 6f, gridPaint)
        canvas.drawText("Amount Saved (Savings Goals)", 40f, y + 16f, Paint().apply { color = Color.parseColor("#115E59"); textSize = 8f })
        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", amountSaved)}", 40f, y + 32f, Paint().apply {
            color = Color.parseColor("#0D9488"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        // Card 8: Months Over Budget
        val fillRose = Paint().apply { color = Color.parseColor("#FFE4E6"); style = Paint.Style.FILL }
        canvas.drawRoundRect(302f, y, 302f + wideCardWidth, y + cardHeight, 6f, 6f, fillRose)
        canvas.drawRoundRect(302f, y, 302f + wideCardWidth, y + cardHeight, 6f, 6f, gridPaint)
        canvas.drawText("Months Over Budget", 312f, y + 16f, Paint().apply { color = Color.parseColor("#9F1239"); textSize = 8f })
        canvas.drawText("$monthsOverBudget months", 312f, y + 32f, Paint().apply {
            color = Color.parseColor("#E11D48"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        y += cardHeight + 20f

        // Category Distribution Section
        canvas.drawText("CATEGORY DISTRIBUTION", 30f, y, sectionTitlePaint)
        y += 12f

        // Category Distribution Grid Table
        val catHeaderY = y
        canvas.drawRect(30f, catHeaderY, 565f, catHeaderY + 20f, tableHeaderBgPaint)
        canvas.drawText("CATEGORY", 40f, catHeaderY + 14f, tableHeaderTextPaint)
        canvas.drawText("TYPE", 180f, catHeaderY + 14f, tableHeaderTextPaint)
        canvas.drawText("ITEMS", 280f, catHeaderY + 14f, tableHeaderTextPaint)
        canvas.drawText("TOTAL AMOUNT", 360f, catHeaderY + 14f, tableHeaderTextPaint)
        canvas.drawText("SHARE %", 480f, catHeaderY + 14f, tableHeaderTextPaint)
        y += 20f

        val totalVolume = totalIncome + totalExpense
        val catGroups = expenses.groupBy { it.category }

        if (catGroups.isEmpty()) {
            canvas.drawRect(30f, y, 565f, y + 20f, gridPaint)
            canvas.drawText("No items recorded in this period.", 40f, y + 14f, bodyPaint)
            y += 20f
        } else {
            catGroups.forEach { (catName, items) ->
                val catInc = items.realIncome(currencyCode)
                val catExp = items.realExpense(currencyCode)
                val catTotal = catInc + catExp
                val sharePct = if (totalVolume > 0) (catTotal / totalVolume) * 100 else 0.0
                val mainType = if (catInc >= catExp) "Income" else "Expense"

                val rowBg = Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL }
                canvas.drawRect(30f, y, 565f, y + 18f, rowBg)
                canvas.drawRect(30f, y, 565f, y + 18f, gridPaint)

                // Grid vertical dividers
                canvas.drawLine(160f, y, 160f, y + 18f, gridPaint)
                canvas.drawLine(260f, y, 260f, y + 18f, gridPaint)
                canvas.drawLine(340f, y, 340f, y + 18f, gridPaint)
                canvas.drawLine(460f, y, 460f, y + 18f, gridPaint)

                canvas.drawText(catName.take(18), 40f, y + 13f, bodyPaint)
                canvas.drawText(mainType, 180f, y + 13f, bodyPaint)
                canvas.drawText("${items.size}", 280f, y + 13f, bodyPaint)
                canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", catTotal)}", 360f, y + 13f, bodyPaint)
                canvas.drawText("${String.format(Locale.getDefault(), "%.1f", sharePct)}%", 480f, y + 13f, bodyPaint)

                y += 18f
            }
        }

        y += 20f

        if (includeDetailedTxns) {
            // Detailed Transactions Section
            canvas.drawText("DETAILED TRANSACTIONS GRID", 30f, y, sectionTitlePaint)
            y += 12f

            fun drawTxTableHeader(c: Canvas, topY: Float) {
                c.drawRect(30f, topY, 565f, topY + 22f, tableHeaderBgPaint)
                c.drawText("DATE", 40f, topY + 15f, tableHeaderTextPaint)
                c.drawText("CATEGORY", 125f, topY + 15f, tableHeaderTextPaint)
                c.drawText("TYPE", 225f, topY + 15f, tableHeaderTextPaint)
                c.drawText("NOTE / DESCRIPTION", 300f, topY + 15f, tableHeaderTextPaint)
                c.drawText("AMOUNT", 475f, topY + 15f, tableHeaderTextPaint)
            }

            drawTxTableHeader(canvas, y)
            y += 22f

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            expenses.forEachIndexed { index, item ->
                if (y > 780f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    canvas = page.canvas

                    // Page Header on subsequent pages
                    canvas.drawRect(0f, 0f, 595f, 40f, headerBgPaint)
                    canvas.drawText("FINANCE LEDGER STATEMENT (Cont. Page $pageNumber)", 30f, 25f, headerTitlePaint.apply { textSize = 12f })

                    y = 50f
                    drawTxTableHeader(canvas, y)
                    y += 22f
                }

                val isIncome = item.type == "INCOME"
                val rowBgColor = if (index % 2 == 0) "#FFFFFF" else "#F8FAFC"
                val rowBg = Paint().apply { color = Color.parseColor(rowBgColor); style = Paint.Style.FILL }

                canvas.drawRect(30f, y, 565f, y + 20f, rowBg)
                canvas.drawRect(30f, y, 565f, y + 20f, gridPaint)

                // Grid column lines
                canvas.drawLine(115f, y, 115f, y + 20f, gridPaint)
                canvas.drawLine(215f, y, 215f, y + 20f, gridPaint)
                canvas.drawLine(290f, y, 290f, y + 20f, gridPaint)
                canvas.drawLine(460f, y, 460f, y + 20f, gridPaint)

                canvas.drawText(sdf.format(Date(item.date)), 38f, y + 14f, bodyPaint)
                canvas.drawText(item.category.take(14), 125f, y + 14f, bodyPaint)
                canvas.drawText(if (isIncome) "INCOME" else "EXPENSE", 225f, y + 14f, if (isIncome) incomePaint else expensePaint)
                canvas.drawText((item.note ?: "No note").take(24), 300f, y + 14f, bodyPaint)

                val itemAmt = Money.toDouble(item.amountMinor, item.currencyCode)
                val amtStr = String.format(Locale.getDefault(), "%s₹%,.2f", if (isIncome) "+" else "-", itemAmt)
                canvas.drawText(amtStr, 475f, y + 14f, if (isIncome) incomePaint else expensePaint)

                y += 20f
            }

            if (expenses.isEmpty()) {
                canvas.drawRect(30f, y, 565f, y + 20f, gridPaint)
                canvas.drawText("No matching transactions found.", 40f, y + 14f, bodyPaint)
                y += 20f
            }
        }

        pdfDocument.finishPage(page)

        cleanOldReports(context)
        val cachePath = File(context.cacheDir, "reports")
        cachePath.mkdirs()
        val pdfFile = File(cachePath, "finance_statement.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Finance Statement PDF")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF Statement"))
    }

    fun exportToCSV(
        context: Context,
        expenses: List<Expense>,
        dateRangeStr: String,
        typeFilterStr: String,
        categoryFilterStr: String
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvHeader = "ID,Date,Category,Amount,Currency,Type,Note\n"
        val csvRows = expenses.joinToString("\n") { expense ->
            val dateFormatted = sdf.format(Date(expense.date))
            val noteEscaped = (expense.note ?: "").replace("\"", "\"\"")
            val amountPlain = Money.formatPlain(expense.amountMinor, expense.currencyCode)
            "${expense.id},\"$dateFormatted\",\"${expense.category}\",$amountPlain,\"${expense.currencyCode}\",\"${expense.type}\",\"$noteEscaped\""
        }
        val csvContent = csvHeader + csvRows

        cleanOldReports(context)
        val cachePath = File(context.cacheDir, "reports")
        cachePath.mkdirs()
        val csvFile = File(cachePath, "finance_statement.csv")
        csvFile.writeText(csvContent)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", csvFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Finance CSV Statement")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share CSV Statement"))
    }

    fun exportToJson(
        context: Context,
        expenses: List<Expense>,
        dateRangeStr: String,
        typeFilterStr: String,
        categoryFilterStr: String,
        currencyCode: String = "INR"
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val generatedAt = sdf.format(Date())

        val totalIncome = expenses.realIncome(currencyCode)
        val totalExpense = expenses.realExpense(currencyCode)
        val netBalance = totalIncome - totalExpense

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"statement\": {\n")
        sb.append("    \"generated_at\": \"$generatedAt\",\n")
        sb.append("    \"date_range\": \"$dateRangeStr\",\n")
        sb.append("    \"type_filter\": \"$typeFilterStr\",\n")
        sb.append("    \"category_filter\": \"$categoryFilterStr\",\n")
        sb.append("    \"summary\": {\n")
        sb.append("      \"total_records\": ${expenses.size},\n")
        sb.append("      \"total_income\": $totalIncome,\n")
        sb.append("      \"total_expense\": $totalExpense,\n")
        sb.append("      \"net_balance\": $netBalance\n")
        sb.append("    },\n")
        sb.append("    \"transactions\": [\n")

        expenses.forEachIndexed { index, exp ->
            val noteEscaped = (exp.note ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
            val comma = if (index < expenses.size - 1) "," else ""
            sb.append("      {\n")
            sb.append("        \"id\": ${exp.id},\n")
            sb.append("        \"date\": \"${sdf.format(Date(exp.date))}\",\n")
            sb.append("        \"timestamp\": ${exp.date},\n")
            sb.append("        \"category\": \"${exp.category.replace("\"", "\\\"")}\",\n")
            sb.append("        \"amount\": ${Money.toDouble(exp.amountMinor, exp.currencyCode)},\n")
            sb.append("        \"amountMinor\": ${exp.amountMinor},\n")
            sb.append("        \"currencyCode\": \"${exp.currencyCode}\",\n")
            sb.append("        \"type\": \"${exp.type}\",\n")
            sb.append("        \"note\": \"$noteEscaped\"\n")
            sb.append("      }$comma\n")
        }

        sb.append("    ]\n")
        sb.append("  }\n")
        sb.append("}")

        val cachePath = File(context.cacheDir, "reports")
        cachePath.mkdirs()
        val jsonFile = File(cachePath, "finance_statement.json")
        jsonFile.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", jsonFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Finance JSON Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share JSON Export"))
    }

    fun shareImageReport(
        context: Context,
        expenses: List<Expense>,
        dateRangeStr: String,
        typeFilterStr: String,
        categoryFilterStr: String,
        currencyCode: String = "INR"
    ) {
        val width = 800
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Sleek App Dark Theme Canvas
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.parseColor("#0F172A"), Color.parseColor("#1E293B"), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val paint = Paint()
        val gridPaint = Paint().apply {
            color = Color.parseColor("#334155")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 13f
        }
        val sectionPaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Header Card
        canvas.drawText("FINANCE LEDGER STATEMENT", 50f, 65f, titlePaint)
        canvas.drawText("Period: $dateRangeStr   •   Type: $typeFilterStr", 50f, 95f, subTitlePaint)
        canvas.drawText("Categories: ${categoryFilterStr.take(50)}", 50f, 118f, subTitlePaint)

        // Totals
        val totalIncome = expenses.realIncome(currencyCode)
        val totalExpense = expenses.realExpense(currencyCode)
        val netBalance = totalIncome - totalExpense

        // KPI Grid Cards (Y: 140..220)
        val cardW = 215f
        val cardH = 75f

        // Income Card
        paint.color = Color.parseColor("#064E3B")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(50f, 140f, 50f + cardW, 140f + cardH, 12f, 12f, paint)
        canvas.drawRoundRect(50f, 140f, 50f + cardW, 140f + cardH, 12f, 12f, gridPaint)
        canvas.drawText("TOTAL INCOME", 65f, 168f, Paint().apply { color = Color.parseColor("#A7F3D0"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("+₹${String.format(Locale.getDefault(), "%,.2f", totalIncome)}", 65f, 198f, Paint().apply {
            color = Color.parseColor("#34D399"); textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        // Expense Card
        paint.color = Color.parseColor("#7F1D1D")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(290f, 140f, 290f + cardW, 140f + cardH, 12f, 12f, paint)
        canvas.drawRoundRect(290f, 140f, 290f + cardW, 140f + cardH, 12f, 12f, gridPaint)
        canvas.drawText("TOTAL EXPENSE", 305f, 168f, Paint().apply { color = Color.parseColor("#FECACA"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("-₹${String.format(Locale.getDefault(), "%,.2f", totalExpense)}", 305f, 198f, Paint().apply {
            color = Color.parseColor("#F87171"); textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        // Net Balance Card
        val netBg = if (netBalance >= 0) "#1E3A8A" else "#7F1D1D"
        val netText = if (netBalance >= 0) "#93C5FD" else "#F8FAFC"
        paint.color = Color.parseColor(netBg)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(530f, 140f, 530f + cardW, 140f + cardH, 12f, 12f, paint)
        canvas.drawRoundRect(530f, 140f, 530f + cardW, 140f + cardH, 12f, 12f, gridPaint)
        canvas.drawText("NET BALANCE", 545f, 168f, Paint().apply { color = Color.parseColor("#E2E8F0"); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", netBalance)}", 545f, 198f, Paint().apply {
            color = Color.parseColor(netText); textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        // Category Distribution Breakdown Box
        var y = 245f
        canvas.drawText("TOTAL CATEGORY DISTRIBUTION", 50f, y, sectionPaint)
        y += 15f

        val catCardBg = Paint().apply { color = Color.parseColor("#1E293B"); style = Paint.Style.FILL }
        val catGroups = expenses.groupBy { it.category }
        val totalVolume = totalIncome + totalExpense

        canvas.drawRoundRect(50f, y, 745f, y + 170f, 16f, 16f, catCardBg)
        canvas.drawRoundRect(50f, y, 745f, y + 170f, 16f, 16f, gridPaint)

        var distY = y + 25f
        if (catGroups.isEmpty()) {
            canvas.drawText("No category distribution available for selected portion.", 70f, distY + 20f, subTitlePaint)
        } else {
            catGroups.entries.take(4).forEach { entry ->
                val catInc = entry.value.realIncome(currencyCode)
                val catExp = entry.value.realExpense(currencyCode)
                val catSum = catInc + catExp
                val share = if (totalVolume > 0) (catSum / totalVolume) * 100 else 0.0

                canvas.drawText(entry.key, 70f, distY, Paint().apply { color = Color.WHITE; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", catSum)} (${String.format(Locale.getDefault(), "%.1f", share)}%)", 500f, distY, Paint().apply { color = Color.parseColor("#38BDF8"); textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

                // Distribution Progress Bar Grid
                val barW = 605f
                val fillW = (barW * (share / 100)).coerceIn(10.0, barW.toDouble()).toFloat()
                canvas.drawRoundRect(70f, distY + 8f, 70f + barW, distY + 18f, 4f, 4f, Paint().apply { color = Color.parseColor("#334155") })
                canvas.drawRoundRect(70f, distY + 8f, 70f + fillW, distY + 18f, 4f, 4f, Paint().apply { color = Color.parseColor("#0EA5E9") })

                distY += 36f
            }
        }

        y += 195f

        // Transactions Table Grid
        canvas.drawText("RECENT TRANSACTIONS GRID", 50f, y, sectionPaint)
        y += 15f

        // Table Header
        canvas.drawRect(50f, y, 745f, y + 30f, Paint().apply { color = Color.parseColor("#334155"); style = Paint.Style.FILL })
        canvas.drawRect(50f, y, 745f, y + 30f, gridPaint)
        canvas.drawText("DATE", 65f, y + 20f, Paint().apply { color = Color.WHITE; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("CATEGORY & NOTE", 200f, y + 20f, Paint().apply { color = Color.WHITE; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("TYPE", 520f, y + 20f, Paint().apply { color = Color.WHITE; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        canvas.drawText("AMOUNT", 630f, y + 20f, Paint().apply { color = Color.WHITE; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

        y += 30f

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        expenses.take(12).forEachIndexed { index, item ->
            val isIncome = item.type == "INCOME"
            val rowBg = Paint().apply { color = Color.parseColor(if (index % 2 == 0) "#1E293B" else "#0F172A"); style = Paint.Style.FILL }

            canvas.drawRect(50f, y, 745f, y + 38f, rowBg)
            canvas.drawRect(50f, y, 745f, y + 38f, gridPaint)

            // Grid column lines
            canvas.drawLine(185f, y, 185f, y + 38f, gridPaint)
            canvas.drawLine(500f, y, 500f, y + 38f, gridPaint)
            canvas.drawLine(610f, y, 610f, y + 38f, gridPaint)

            canvas.drawText(sdf.format(Date(item.date)), 65f, y + 24f, Paint().apply { color = Color.parseColor("#CBD5E1"); textSize = 11f })
            canvas.drawText("${item.category} • ${(item.note ?: "No description").take(22)}", 200f, y + 24f, Paint().apply { color = Color.WHITE; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText(if (isIncome) "INCOME" else "EXPENSE", 520f, y + 24f, Paint().apply { color = Color.parseColor(if (isIncome) "#34D399" else "#F8FAFC"); textSize = 11f })

            val itemAmt = Money.toDouble(item.amountMinor, item.currencyCode)
            val amtStr = String.format(Locale.getDefault(), "%s₹%,.2f", if (isIncome) "+" else "-", itemAmt)
            canvas.drawText(amtStr, 630f, y + 24f, Paint().apply { color = Color.parseColor(if (isIncome) "#34D399" else "#F8FAFC"); textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

            y += 38f
        }

        if (expenses.size > 12) {
            canvas.drawText("... and ${expenses.size - 12} more items logged", 50f, y + 25f, subTitlePaint)
        }

        // Footer
        canvas.drawText("Generated with Personal Finance App", 50f, 1160f, subTitlePaint)

        cleanOldReports(context)
        val cachePath = File(context.cacheDir, "reports")
        cachePath.mkdirs()
        val imageFile = File(cachePath, "finance_statement.jpg")

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Finance Image Statement")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Image Statement"))
    }

    private fun cleanOldReports(context: Context) {
        try {
            val cachePath = File(context.cacheDir, "reports")
            if (cachePath.exists()) {
                val now = System.currentTimeMillis()
                cachePath.listFiles()?.forEach { file ->
                    // Delete files older than 1 hour to prevent accumulation
                    if (now - file.lastModified() > 3600_000L) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean old cached reports", e)
        }
    }
}
