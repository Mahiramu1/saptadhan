package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiLoanService
import com.example.data.local.AppDatabase
import com.example.data.local.InitialSeedData
import com.example.data.model.AttendanceStatus
import com.example.data.model.LoanApplication
import com.example.data.model.MandalCluster
import com.example.data.model.PaymentMode
import com.example.data.model.RepaymentStatus
import com.example.data.repository.FieldBankingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Saptadhan", appName)
    }

    @Test
    fun `room database stores and retrieves loan applications`() = runBlocking {
        val dao = db.fieldBankingDao()
        val application = LoanApplication(
            applicationId = "APP-TEST-001",
            applicantName = "Lakshmi Bai",
            mobileNumber = "+91 98480 12345",
            aadhaarMasked = "XXXX-XXXX-8921",
            mandal = MandalCluster.KOTHAKOTA,
            village = "Natavalli",
            requestedAmount = 35000.0,
            loanPurpose = "Purchase of hybrid cotton seeds and farm fertilizers",
            annualIncome = 180000.0,
            monthlyIncome = 15000.0,
            existingMonthlyDebt = 2000.0,
            foirPercentage = 34.5,
            loanCycle = 1,
            status = "APPROVED",
            aiRiskScore = 86
        )

        dao.insertLoanApplication(application)
        val loaded = dao.getLoanApplicationById("APP-TEST-001")
        assertNotNull(loaded)
        assertEquals("Lakshmi Bai", loaded?.applicantName)
        assertEquals(35000.0, loaded?.requestedAmount ?: 0.0, 0.01)
        assertEquals("APPROVED", loaded?.status)
        assertEquals(86, loaded?.aiRiskScore)

        val allApps = dao.getAllLoanApplications().first()
        assertTrue(allApps.isNotEmpty())
        assertEquals("APP-TEST-001", allApps.first().applicationId)
    }

    @Test
    fun `kfs calculation complies with RBI 2022 guidelines`() {
        val kfs = FieldBankingRepository.calculateKfs(
            borrowerName = "K. Renuka",
            groupName = "Sri Rama JLG",
            loanAmount = 40000.0,
            loanCycle = 2
        )

        assertEquals("K. Renuka", kfs.borrowerName)
        assertEquals(40000.0, kfs.loanAmount, 0.01)
        assertEquals(22.0, kfs.reducingInterestRatePa, 0.01)
        assertEquals("NIL (0% as per RBI MFI Master Directions 2022)", kfs.prepaymentPenalty)
        assertTrue(kfs.netDisbursedAmount < kfs.loanAmount)
        assertTrue(kfs.installmentAmount > 0)
    }

    @Test
    fun `ai underwriter risk assessment evaluates applicant`() = runBlocking {
        val testApp = LoanApplication(
            applicationId = "APP-TEST-002",
            applicantName = "B. Kavitha",
            mobileNumber = "+91 98480 99887",
            aadhaarMasked = "XXXX-XXXX-3344",
            mandal = MandalCluster.GOPALPET,
            village = "Gopalpet Rural",
            requestedAmount = 45000.0,
            loanPurpose = "Dairy cattle fodder and milk preservation chiller",
            annualIncome = 210000.0,
            monthlyIncome = 17500.0,
            existingMonthlyDebt = 2500.0,
            foirPercentage = 31.2,
            loanCycle = 2
        )

        val assessment = GeminiLoanService.analyzeLoanApplication(testApp)
        assertNotNull(assessment)
        assertTrue(assessment.riskScore in 1..100)
        assertNotNull(assessment.status)
        assertTrue(assessment.summary.isNotBlank())
        assertTrue(assessment.advisoryRecommendations.isNotEmpty())
    }

    @Test
    fun `field collections compute pending repayments from Room database`() = runBlocking {
        val dao = db.fieldBankingDao()
        val repo = FieldBankingRepository(dao)

        val centers = InitialSeedData.getInitialCenters()
        val groups = InitialSeedData.getInitialGroups()
        val borrowers = InitialSeedData.getInitialBorrowers()
        dao.insertCenters(centers)
        dao.insertGroups(groups)
        dao.insertBorrowers(borrowers)

        val initialRepayments = InitialSeedData.getInitialRepayments()
        initialRepayments.forEach { dao.insertRepayment(it) }

        val dbCenters = dao.getAllCenters().first()
        val dbGroups = dao.getAllGroups().first()
        val dbBorrowers = dao.getAllBorrowers().first()
        val dbRepayments = dao.getAllRepayments().first()

        val pendingItems = repo.computePendingRepayments(dbCenters, dbGroups, dbBorrowers, dbRepayments)
        assertTrue(pendingItems.isNotEmpty())

        // Verified that Padmamma has paid her installment
        val padmamma = pendingItems.find { it.borrowerId == "BOR-KTH-101" }
        assertNotNull(padmamma)
        assertEquals(RepaymentStatus.PAID, padmamma?.status)
        assertEquals(0.0, padmamma?.pendingAmount ?: 0.0, 0.01)

        // Record a new cash collection for an unpaid borrower
        val unpaid = pendingItems.first { it.pendingAmount > 0 }
        val receipt = repo.recordRepayment(
            centerId = unpaid.centerId,
            groupId = unpaid.groupId,
            borrowerId = unpaid.borrowerId,
            borrowerName = unpaid.borrowerName,
            installmentDue = unpaid.installmentDue,
            amountPaid = unpaid.pendingAmount,
            paymentMode = PaymentMode.CASH,
            attendance = AttendanceStatus.PRESENT
        )

        assertNotNull(receipt.receiptNumber)
        val updatedRepayments = dao.getAllRepayments().first()
        val updatedPending = repo.computePendingRepayments(dbCenters, dbGroups, dbBorrowers, updatedRepayments)
        val paidNow = updatedPending.find { it.borrowerId == unpaid.borrowerId }
        assertEquals(RepaymentStatus.PAID, paidNow?.status)
        assertEquals(0.0, paidNow?.pendingAmount ?: 0.0, 0.01)
    }

    @Test
    fun `borrower repayment history retrieves previous payment dates and amounts from Room database`() = runBlocking {
        val dao = db.fieldBankingDao()
        val repo = FieldBankingRepository(dao)

        val centers = InitialSeedData.getInitialCenters()
        val groups = InitialSeedData.getInitialGroups()
        val borrowers = InitialSeedData.getInitialBorrowers()
        dao.insertCenters(centers)
        dao.insertGroups(groups)
        dao.insertBorrowers(borrowers)

        val initialRepayments = InitialSeedData.getInitialRepayments()
        initialRepayments.forEach { dao.insertRepayment(it) }

        // Test borrower repayment history retrieval for BOR-KTH-101 (K. Padmamma)
        val borrowerHistory = repo.getRepaymentsForBorrower("BOR-KTH-101").first()
        assertTrue(borrowerHistory.isNotEmpty())
        assertTrue("Borrower should have multiple previous payments", borrowerHistory.size >= 3)

        // Verify previous payment dates and amounts
        borrowerHistory.forEach { record ->
            assertEquals("BOR-KTH-101", record.borrowerId)
            assertTrue("Payment date timestamp must be positive", record.timestamp > 0L)
            assertTrue("Payment amount must be greater than zero", record.amountPaid > 0.0)
            assertNotNull(record.receiptNumber)
            assertNotNull(record.paymentMode)
        }

        // Verify total amount paid across history
        val totalPaid = borrowerHistory.sumOf { it.amountPaid }
        assertTrue(totalPaid >= 4000.0)

        // Verify sorting (descending by timestamp)
        for (i in 0 until borrowerHistory.size - 1) {
            assertTrue(borrowerHistory[i].timestamp >= borrowerHistory[i + 1].timestamp)
        }
    }
}
