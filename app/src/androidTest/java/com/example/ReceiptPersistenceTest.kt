package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.AppDatabase
import com.example.data.local.InitialSeedData
import com.example.data.model.AttendanceStatus
import com.example.data.model.PaymentMode
import com.example.data.repository.FieldBankingRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptPersistenceTest {
    @Test fun offlineCashSurvivesRoomReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "field-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            val dao = db.fieldBankingDao()
            val group = InitialSeedData.getInitialGroups().first().copy(groupId = "TEST-JLG")
            val borrower = InitialSeedData.getInitialBorrowers().first().copy(
                borrowerId = "FICTIONAL", fullName = "Test Borrower", groupId = group.groupId)
            dao.insertCenters(InitialSeedData.getInitialCenters())
            dao.insertGroup(group)
            dao.insertBorrower(borrower)
            val repo = FieldBankingRepository(dao)
            suspend fun collect() = repo.recordRepayment(group.centerId, group.groupId, borrower.borrowerId,
                borrower.fullName, 1500.0, 1000.0, PaymentMode.CASH, AttendanceStatus.PRESENT,
                officerCode = "TEST-OFFICER")
            assertTrue(runCatching { collect() }.isFailure)
            dao.confirmDisbursement(group.groupId, LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(14).toString())
            val original = collect()
            db.close()
            db = open()
            val pending = db.fieldBankingDao().pending("TEST-OFFICER").single()
            assertTrue(pending.syncPending)
            assertEquals(original.receiptNumber, pending.receiptNumber)
            assertEquals(1000.0, pending.amountPaid, 0.0)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
