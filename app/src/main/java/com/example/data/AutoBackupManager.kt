package com.example.data

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AutoBackupManager"
private const val SNAPSHOT_DIR = "auto_backups"
private const val MAX_SNAPSHOTS = 7

/**
 * Real, on-device daily snapshotting for the "Automatic Daily Snapshot" setting.
 *
 * The setting used to be a switch that persisted a boolean and nothing else — no snapshot was ever
 * written, and "Last Local Snapshot" simply showed whatever string happened to be in prefs.
 *
 * This manager writes an actual JSON snapshot (via [AutoBackupExporter]) once per calendar day,
 * keeps the newest [MAX_SNAPSHOTS] files, and prunes the rest. Everything stays inside the app's
 * private files directory, consistent with the "100% offline" promise.
 */
object AutoBackupManager {

    fun snapshotDir(context: Context): File {
        val dir = File(context.filesDir, SNAPSHOT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun todayStamp(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun lastSnapshotTime(context: Context): String? {
        val latest = listSnapshots(context).firstOrNull() ?: return null
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date(latest.lastModified()))
    }

    fun listSnapshots(context: Context): List<File> {
        val dir = snapshotDir(context)
        return (dir.listFiles { file -> file.extension == "json" } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
    }

    /** Writes today's snapshot if one has not been written yet. Returns the file when written. */
    fun ensureTodaySnapshot(context: Context): File? {
        val dir = snapshotDir(context)
        val name = "snapshot_${todayStamp()}.json"
        val target = File(dir, name)
        if (target.exists()) return null
        return try {
            val payload = AutoBackupExporter.exportJson(context)
            target.writeText(payload)
            prune(context)
            target
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write automatic snapshot", e)
            null
        }
    }

    /** Writes an immediate snapshot (used by "Create Manual Local Snapshot"). */
    fun writeSnapshotNow(context: Context): File? {
        val dir = snapshotDir(context)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(dir, "snapshot_$stamp.json")
        return try {
            target.writeText(AutoBackupExporter.exportJson(context))
            prune(context)
            target
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write manual snapshot", e)
            null
        }
    }

    fun prune(context: Context) {
        val files = listSnapshots(context)
        if (files.size <= MAX_SNAPSHOTS) return
        files.drop(MAX_SNAPSHOTS).forEach { it.delete() }
    }

    fun totalSnapshotBytes(context: Context): Long =
        listSnapshots(context).sumOf { it.length() }

    fun clearAll(context: Context) {
        listSnapshots(context).forEach { it.delete() }
    }
}

/**
 * Minimal JSON snapshot writer. Deliberately dependency-free (no extra serialization library) and
 * limited to the tables the app owns, so it can never corrupt the live database — it is read-only
 * with respect to Room.
 */
object AutoBackupExporter {

    fun exportJson(context: Context): String {
        val db = FinanceDatabase.getDatabase(context)
        val out = StringBuilder()
        out.append("{")
        out.append("\"format\":\"financer-auto-snapshot\",")
        out.append("\"version\":1,")
        out.append("\"createdAt\":").append(System.currentTimeMillis()).append(",")
        out.append("\"generatedAt\":\"").append(escape(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        )).append("\",")
        out.append("\"tables\":{")

        val tables = listOf("expenses", "savings_goals", "recurring_rules", "reminders", "accounts")
        tables.forEachIndexed { index, table ->
            out.append("\"").append(table).append("\":[")
            out.append(dumpTable(db, table))
            out.append("]")
            if (index != tables.lastIndex) out.append(",")
        }
        out.append("}")
        out.append("}")
        return out.toString()
    }

    private fun dumpTable(db: FinanceDatabase, table: String): String {
        val rows = StringBuilder()
        try {
            db.openHelper.readableDatabase.query("SELECT * FROM $table LIMIT 5000").use { cursor: android.database.Cursor ->
                var first = true
                while (cursor.moveToNext()) {
                    if (!first) rows.append(",")
                    first = false
                    rows.append("{")
                    for (i in 0 until cursor.columnCount) {
                        if (i > 0) rows.append(",")
                        val column = cursor.getColumnName(i)
                        rows.append("\"").append(escape(column)).append("\":")
                        when (cursor.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> rows.append("null")
                            android.database.Cursor.FIELD_TYPE_INTEGER -> rows.append(cursor.getLong(i))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> rows.append(cursor.getDouble(i))
                            else -> rows.append("\"").append(escape(cursor.getString(i) ?: "")).append("\"")
                        }
                    }
                    rows.append("}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Skipping table $table in snapshot", e)
        }
        return rows.toString()
    }

    private fun escape(value: String): String {
        val sb = StringBuilder(value.length + 8)
        value.forEach { ch ->
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
