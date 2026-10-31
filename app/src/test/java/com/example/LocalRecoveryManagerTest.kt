package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.Account
import com.example.data.Expense
import com.example.data.FinanceDatabase
import com.example.data.LocalRecoveryManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalRecoveryManagerTest {
    private lateinit var context: Context
    private lateinit var database: FinanceDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        LocalRecoveryManager.getSnapshotDir(context).listFiles()?.forEach { it.delete() }
    }

    @Test
    fun snapshotRestorePreservesOriginalCurrencyMetadata() = runBlocking {
        val dao = database.financeDao()
        val accountId = dao.insertAccount(Account(name = "Travel", currencyCode = "USD"))
        dao.insertExpense(
            Expense(
                amount = 92.0,
                amountMinor = 9200L,
                category = "Travel",
                date = 1_800_000_000_000L,
                currencyCode = "EUR",
                accountId = accountId,
                originalCurrencyCode = "USD",
                originalAmountMinor = 10000L
            )
        )

        assertTrue(LocalRecoveryManager.createSnapshot(context, database))
        val snapshot = LocalRecoveryManager.listSnapshots(context).first { it.isValid }

        dao.deleteAllExpenses()
        assertTrue(
            LocalRecoveryManager.restoreSnapshot(context, database, snapshot.fileName) { _, _, _, _ -> }
        )

        val restored = dao.getExpensesSnapshot().single()
        assertEquals("USD", restored.originalCurrencyCode)
        assertEquals(10000L, restored.originalAmountMinor)
        assertEquals("EUR", restored.currencyCode)
        assertEquals(9200L, restored.amountMinor)
    }

    @Test
    fun invalidSnapshotPathIsRejected() = runBlocking {
        assertFalse(
            LocalRecoveryManager.restoreSnapshot(context, database, "../outside.json") { _, _, _, _ -> }
        )
    }

    @Test
    fun corruptSnapshotIsMarkedInvalidAndCannotRestore() = runBlocking {
        val dir = LocalRecoveryManager.getSnapshotDir(context)
        val corrupt = File(dir, "snapshot_123.json")
        corrupt.writeText("{\"formatVersion\":2,\"checksum\":\"wrong\",\"payload\":\"{}\"}")

        val listed = LocalRecoveryManager.listSnapshots(context).single()
        assertFalse(listed.isValid)
        assertFalse(
            LocalRecoveryManager.restoreSnapshot(context, database, corrupt.name) { _, _, _, _ -> }
        )
    }
}
