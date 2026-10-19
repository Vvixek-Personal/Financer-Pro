package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.Expense
import com.example.data.Money
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DataImporter"

data class ImportParseResult(
    val transactions: List<Expense>,
    val errors: List<String> = emptyList(),
    val format: String = "CSV"
)

object DataImporter {

    private val supportedDateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("MMM dd, yyyy", Locale.US)
    )

    fun parseFile(
        context: Context,
        uri: Uri,
        defaultCurrency: String = "INR",
        defaultAccountId: Long = 1L
    ): ImportParseResult {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: return ImportParseResult(emptyList(), listOf("Unable to open file stream."))

            val trimmed = content.trim()
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                parseJsonContent(trimmed, defaultCurrency, defaultAccountId)
            } else {
                parseCsvContent(trimmed, defaultCurrency, defaultAccountId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing file", e)
            ImportParseResult(emptyList(), listOf("Failed to parse file: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    private fun parseJsonContent(
        jsonString: String,
        defaultCurrency: String,
        defaultAccountId: Long
    ): ImportParseResult {
        val list = mutableListOf<Expense>()
        val errors = mutableListOf<String>()

        return try {
            val array = if (jsonString.startsWith("[")) {
                JSONArray(jsonString)
            } else {
                val root = JSONObject(jsonString)
                when {
                    root.has("expenses") -> root.getJSONArray("expenses")
                    root.has("transactions") -> root.getJSONArray("transactions")
                    root.has("items") -> root.getJSONArray("items")
                    else -> JSONArray()
                }
            }

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val amount = obj.optDouble("amount", 0.0)
                val category = obj.optString("category", "General").ifBlank { "General" }
                val type = obj.optString("type", "EXPENSE").uppercase(Locale.getDefault())
                val note = obj.optString("note", "").ifBlank { obj.optString("title", null) }
                val currency = obj.optString("currencyCode", obj.optString("currency", defaultCurrency))
                val dateVal = obj.optLong("date", obj.optLong("timestamp", System.currentTimeMillis()))
                val accountId = obj.optLong("accountId", defaultAccountId)

                if (amount > 0.0) {
                    list.add(
                        Expense(
                            amount = amount,
                            amountMinor = Money.fromDouble(amount, currency),
                            category = category,
                            date = if (dateVal > 0) dateVal else System.currentTimeMillis(),
                            note = note,
                            type = if (type == "INCOME") "INCOME" else "EXPENSE",
                            currencyCode = currency,
                            accountId = accountId,
                            kind = "REGULAR"
                        )
                    )
                }
            }
            ImportParseResult(list, errors, "JSON")
        } catch (e: Exception) {
            ImportParseResult(emptyList(), listOf("Invalid JSON format: ${e.message}"), "JSON")
        }
    }

    private fun parseCsvContent(
        csvString: String,
        defaultCurrency: String,
        defaultAccountId: Long
    ): ImportParseResult {
        val lines = csvString.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ImportParseResult(emptyList(), listOf("CSV file is empty."))
        }

        val headerTokens = parseCsvLine(lines.first()).map { it.trim().lowercase(Locale.getDefault()) }
        val dateIdx = headerTokens.indexOfFirst { it.contains("date") || it.contains("time") }
        val categoryIdx = headerTokens.indexOfFirst { it.contains("category") }
        val amountIdx = headerTokens.indexOfFirst { it.contains("amount") }
        val debitIdx = headerTokens.indexOfFirst { it.contains("debit") || it.contains("withdrawal") }
        val creditIdx = headerTokens.indexOfFirst { it.contains("credit") || it.contains("deposit") }
        val typeIdx = headerTokens.indexOfFirst { it == "type" }
        val noteIdx = headerTokens.indexOfFirst { it.contains("note") || it.contains("desc") || it.contains("narration") || it.contains("title") }
        val tagsIdx = headerTokens.indexOfFirst { it.contains("tag") }
        val accountIdx = headerTokens.indexOfFirst { it.contains("account") }
        val currencyIdx = headerTokens.indexOfFirst { it.contains("currency") }

        val list = mutableListOf<Expense>()
        val errors = mutableListOf<String>()

        for (i in 1 until lines.size) {
            val line = lines[i]
            val tokens = parseCsvLine(line)
            if (tokens.isEmpty()) continue

            try {
                var amount = 0.0
                var type = "EXPENSE"

                if (amountIdx >= 0 && amountIdx < tokens.size) {
                    val rawAmt = cleanNumber(tokens[amountIdx])
                    val parsed = rawAmt.toDoubleOrNull() ?: 0.0
                    amount = Math.abs(parsed)
                    if (parsed < 0) {
                        type = "EXPENSE"
                    }
                } else if (debitIdx >= 0 && creditIdx >= 0) {
                    val debit = (cleanNumber(tokens.getOrNull(debitIdx) ?: "")).toDoubleOrNull() ?: 0.0
                    val credit = (cleanNumber(tokens.getOrNull(creditIdx) ?: "")).toDoubleOrNull() ?: 0.0
                    if (credit > 0) {
                        amount = credit
                        type = "INCOME"
                    } else if (debit > 0) {
                        amount = debit
                        type = "EXPENSE"
                    }
                }

                if (typeIdx >= 0 && typeIdx < tokens.size) {
                    val t = tokens[typeIdx].trim().uppercase(Locale.getDefault())
                    if (t.contains("INC") || t.contains("CREDIT")) {
                        type = "INCOME"
                    } else if (t.contains("EXP") || t.contains("DEBIT")) {
                        type = "EXPENSE"
                    }
                }

                val category = if (categoryIdx >= 0 && categoryIdx < tokens.size && tokens[categoryIdx].isNotBlank()) {
                    tokens[categoryIdx].trim()
                } else {
                    if (type == "INCOME") "Salary" else "General"
                }

                val rawNote = if (noteIdx >= 0 && noteIdx < tokens.size && tokens[noteIdx].isNotBlank()) {
                    tokens[noteIdx].trim()
                } else ""

                val rawTags = if (tagsIdx >= 0 && tagsIdx < tokens.size && tokens[tagsIdx].isNotBlank()) {
                    tokens[tagsIdx].trim()
                } else ""

                val note = when {
                    rawNote.isNotBlank() && rawTags.isNotBlank() -> "$rawNote $rawTags"
                    rawTags.isNotBlank() -> rawTags
                    rawNote.isNotBlank() -> rawNote
                    else -> null
                }

                val currency = if (currencyIdx >= 0 && currencyIdx < tokens.size && tokens[currencyIdx].isNotBlank()) {
                    tokens[currencyIdx].trim().uppercase(Locale.getDefault())
                } else defaultCurrency

                var date = System.currentTimeMillis()
                if (dateIdx >= 0 && dateIdx < tokens.size) {
                    val dateStr = tokens[dateIdx].trim()
                    date = parseDate(dateStr)
                }

                if (amount > 0.0) {
                    list.add(
                        Expense(
                            amount = amount,
                            amountMinor = Money.fromDouble(amount, currency),
                            category = category,
                            date = date,
                            note = note,
                            type = type,
                            currencyCode = currency,
                            accountId = defaultAccountId,
                            kind = "REGULAR"
                        )
                    )
                }
            } catch (e: Exception) {
                errors.add("Line ${i + 1}: ${e.message}")
            }
        }

        return ImportParseResult(list, errors, "CSV")
    }

    private fun cleanNumber(str: String): String {
        return str.replace(",", "")
            .replace("₹", "")
            .replace("$", "")
            .replace("€", "")
            .replace("£", "")
            .trim()
    }

    private fun parseDate(str: String): Long {
        val clean = str.trim()
        val num = clean.toLongOrNull()
        if (num != null && num > 1000000000L) {
            return if (num < 10000000000L) num * 1000L else num
        }

        for (sdf in supportedDateFormats) {
            try {
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        cur.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(cur.toString())
                    cur.setLength(0)
                }
                else -> {
                    cur.append(c)
                }
            }
            i++
        }
        result.add(cur.toString())
        return result
    }
}
