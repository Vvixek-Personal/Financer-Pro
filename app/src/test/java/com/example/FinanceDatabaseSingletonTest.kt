package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FinanceDatabase
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceArray

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceDatabaseSingletonTest {
    @Test
    fun concurrentInitializationReturnsOneRoomInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workerCount = 12
        val results = AtomicReferenceArray<FinanceDatabase>(workerCount)
        val ready = CountDownLatch(workerCount)
        val start = CountDownLatch(1)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(workerCount)

        try {
            repeat(workerCount) { index ->
                executor.execute {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS))
                    results.set(index, FinanceDatabase.getDatabase(context))
                }
            }
            check(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            executor.shutdown()
            check(executor.awaitTermination(20, TimeUnit.SECONDS))

            val first = results.get(0)
            repeat(workerCount) { index -> assertSame(first, results.get(index)) }
        } finally {
            executor.shutdownNow()
        }
    }
}
