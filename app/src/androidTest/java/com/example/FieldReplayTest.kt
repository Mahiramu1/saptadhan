package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.ledger.Ledger
import com.example.data.local.AppDatabase
import com.example.data.local.InitialSeedData
import com.example.data.model.AttendanceStatus
import com.example.data.model.PaymentMode
import com.example.data.repository.FieldBankingRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Invoked in separate processes by .github/scripts/android-field-test.sh with a disposable ledger. */
@RunWith(AndroidJUnit4::class)
class FieldReplayTest {
    @Test fun runPhase() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("phase")
        assumeTrue(phase != null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "offline-replay-test.db").build()
        val prefs = context.getSharedPreferences("offline-replay-test", 0)
        val dao = db.fieldBankingDao()
        val repo = FieldBankingRepository(dao)
        val officer = "FIELD-TEST"
        try {
            when (phase) {
                "prepare" -> {
                    assertTrue(runCatching { Ledger.login(context, officer, "000000", false) }.isFailure)
                    Ledger.login(context, officer, requireNotNull(args.getString("pin")), false)
                    dao.insertCenters(InitialSeedData.getInitialCenters())
                    val group = InitialSeedData.getInitialGroups().first().copy(groupId = "TEST-JLG")
                    dao.insertGroup(group)
                    val borrower = InitialSeedData.getInitialBorrowers().first().copy(borrowerId = "TEST-BOR", groupId = group.groupId)
                    dao.insertBorrower(borrower)
                    repo.executeMorningSync()
                    assertTrue(requireNotNull(dao.getGroupById(group.groupId)).isDisbursed)
                    // CI removes the HTTPS tunnel and radio connectivity while this session stays open.
                    java.io.File(context.filesDir, "ready-for-offline").createNewFile()
                    val offline = java.io.File(context.filesDir, "offline")
                    for (i in 1..120) {
                        if (offline.exists()) break
                        kotlinx.coroutines.delay(500)
                    }
                    assertTrue(offline.exists())
                    val receipt = repo.recordRepayment(group.centerId, group.groupId, borrower.borrowerId,
                        "Fictional Borrower", 1500.0, 1000.0, PaymentMode.CASH, AttendanceStatus.PRESENT)
                    assertTrue(runCatching { repo.executeEveningSync() }.isFailure)
                    assertEquals(receipt.receiptNumber, dao.pending(officer).single().receiptNumber)
                    assertTrue(prefs.edit().putString("receipt", receipt.receiptNumber).commit())
                }
                "reopen" -> {
                    val saved = dao.pending(officer).single()
                    assertEquals(prefs.getString("receipt", null), saved.receiptNumber)
                    assertEquals(1000.0, saved.amountPaid, 0.0)
                    assertTrue(saved.syncPending)
                }
                "sync" -> {
                    Ledger.login(context, officer, requireNotNull(args.getString("pin")), false)
                    assertEquals(1, repo.executeEveningSync())
                    assertEquals(0, repo.executeEveningSync())
                    val saved = dao.getRepaymentsByBorrower("TEST-BOR").let { it.first() }.single()
                    assertFalse(saved.syncPending)
                    assertEquals(prefs.getString("receipt", null), saved.receiptNumber)
                    java.io.File(context.filesDir, "verified-receipt").writeText(saved.receiptNumber)
                }
                else -> error("Unknown test phase")
            }
        } finally { db.close() }
    }
}
