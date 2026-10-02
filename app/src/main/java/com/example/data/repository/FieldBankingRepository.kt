package com.example.data.repository

import com.example.data.local.FieldBankingDao
import com.example.data.local.InitialSeedData
import com.example.data.model.AttendanceStatus
import com.example.data.model.Borrower
import com.example.data.model.CashReconciliation
import com.example.data.model.JlgGroup
import com.example.data.model.KendraCenter
import com.example.data.model.KeyFactStatement
import com.example.data.model.LoanApplication
import com.example.data.model.LoanCycle
import com.example.data.model.MandalCluster
import com.example.data.model.OverdueReason
import com.example.data.model.PaymentMode
import com.example.data.model.PendingRepaymentItem
import com.example.data.model.RepaymentRecord
import com.example.data.model.RepaymentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FieldBankingRepository(private val dao: FieldBankingDao) {

    val allCenters: Flow<List<KendraCenter>> = dao.getAllCenters()
    val allGroups: Flow<List<JlgGroup>> = dao.getAllGroups()
    val allBorrowers: Flow<List<Borrower>> = dao.getAllBorrowers()
    val allRepayments: Flow<List<RepaymentRecord>> = dao.getAllRepayments()
    val allLoanApplications: Flow<List<LoanApplication>> = dao.getAllLoanApplications()
    val pendingRepaymentsCount: Flow<Int> = dao.getPendingRepaymentsCount()
    val pendingBorrowersCount: Flow<Int> = dao.getPendingBorrowersCount()

    fun getRepaymentsForBorrower(borrowerId: String): Flow<List<RepaymentRecord>> =
        dao.getRepaymentsByBorrower(borrowerId)

    fun getTodayReconciliation(): Flow<CashReconciliation?> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        return dao.getReconciliationForDate(todayStr)
    }

    suspend fun initializeSeedDataIfNeeded() {
        val existing = dao.getAllCenters().firstOrNull()
        if (existing.isNullOrEmpty()) {
            dao.insertCenters(InitialSeedData.getInitialCenters())
            dao.insertGroups(InitialSeedData.getInitialGroups())
            dao.insertBorrowers(InitialSeedData.getInitialBorrowers())
        }
        val existingRepayments = dao.getAllRepayments().firstOrNull()
        if (existingRepayments.isNullOrEmpty()) {
            InitialSeedData.getInitialRepayments().forEach { dao.insertRepayment(it) }
            val kothakotaCenter = dao.getCenterById("KND-KTH-04")
            if (kothakotaCenter != null) {
                dao.updateCenter(kothakotaCenter.copy(collectedAmount = 2870.0))
            }
            val gopalpetCenter = dao.getCenterById("KND-GPL-02")
            if (gopalpetCenter != null) {
                dao.updateCenter(gopalpetCenter.copy(collectedAmount = 1913.0))
            }
        }
        val existingApps = dao.getAllLoanApplications().firstOrNull()
        if (existingApps.isNullOrEmpty()) {
            dao.insertLoanApplication(
                LoanApplication(
                    applicationId = "APP-WNP-2026-101",
                    applicantName = "Y. Lakshmi Devi",
                    mobileNumber = "+91 98480 33441",
                    aadhaarMasked = "XXXX-XXXX-9821",
                    mandal = MandalCluster.KOTHAKOTA,
                    village = "Natavalli, Kothakota",
                    requestedAmount = 35000.0,
                    loanPurpose = "Purchase of certified hybrid cotton seeds, organic fertilizers, and drip sprayers for upcoming kharif crop.",
                    annualIncome = 190000.0,
                    monthlyIncome = 15833.0,
                    existingMonthlyDebt = 2200.0,
                    foirPercentage = 24.3,
                    loanCycle = 1,
                    status = "APPROVED",
                    aiRiskScore = 88,
                    aiAssessmentSummary = "Application fully compliant with RBI Master Directions 2022. Strong repayment capacity supported by seasonal cotton harvest in Kothakota cluster.",
                    aiRecommendation = "Sanction approved at ₹35,000 for 12 months with fortnightly Kendra repayments."
                )
            )
        }
    }

    fun computePendingRepayments(
        centers: List<KendraCenter>,
        groups: List<JlgGroup>,
        borrowers: List<Borrower>,
        repayments: List<RepaymentRecord>
    ): List<PendingRepaymentItem> {
        val groupMap = groups.associateBy { it.groupId }
        val centerMap = centers.associateBy { it.centerId }
        val repaymentMap = repayments.groupBy { it.borrowerId }
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 3600 * 1000L

        return borrowers.mapNotNull { borrower ->
            val group = groupMap[borrower.groupId] ?: return@mapNotNull null
            val center = centerMap[group.centerId] ?: return@mapNotNull null

            val kfs = calculateKfs(borrower.fullName, group.groupName, group.sanctionedAmountPerMember, group.loanCycle)
            val installmentDue = kfs.installmentAmount

            val borrowerRecords = repaymentMap[borrower.borrowerId].orEmpty()
            val todayRecords = borrowerRecords.filter { (now - it.timestamp) < oneDayMillis }
            val amountPaid = todayRecords.sumOf { it.amountPaid }
            val pendingAmount = (installmentDue - amountPaid).coerceAtLeast(0.0)

            val totalHistoricalPaid = borrowerRecords.sumOf { it.amountPaid }
            val previousPaymentsCount = borrowerRecords.count { it.amountPaid > 0 }

            val lastRecord = borrowerRecords.maxByOrNull { it.timestamp }
            val isDelinquent = todayRecords.any { it.overdueReason != null || it.isCoveredByGuarantee }

            val pseudoHash = Math.abs(borrower.borrowerId.hashCode())
            val overdueDays = when {
                isDelinquent -> 14
                amountPaid >= installmentDue -> 0
                pseudoHash % 5 == 0 -> 21
                pseudoHash % 3 == 0 -> 7
                else -> 0
            }

            val status = when {
                amountPaid >= installmentDue -> RepaymentStatus.PAID
                amountPaid > 0 -> RepaymentStatus.PARTIALLY_PAID
                isDelinquent -> RepaymentStatus.DELINQUENT
                overdueDays > 0 -> RepaymentStatus.OVERDUE
                else -> RepaymentStatus.DUE_TODAY
            }

            PendingRepaymentItem(
                borrowerId = borrower.borrowerId,
                borrowerName = borrower.fullName,
                mobileNumber = borrower.mobileNumber,
                aadhaarMasked = borrower.aadhaarMasked,
                groupId = group.groupId,
                groupName = group.groupName,
                centerId = center.centerId,
                centerName = center.centerName,
                mandal = borrower.mandal,
                village = borrower.village,
                loanCycle = group.loanCycle,
                sanctionedAmount = group.sanctionedAmountPerMember,
                installmentDue = installmentDue,
                amountPaid = amountPaid,
                pendingAmount = pendingAmount,
                meetingDay = center.meetingDay,
                meetingTime = center.meetingTime,
                overdueDays = overdueDays,
                isDelinquent = isDelinquent,
                overdueReason = lastRecord?.overdueReason,
                isCoveredByGuarantee = lastRecord?.isCoveredByGuarantee ?: false,
                attendance = lastRecord?.attendance ?: AttendanceStatus.PRESENT,
                status = status,
                totalPaidHistorical = totalHistoricalPaid,
                previousPaymentsCount = previousPaymentsCount
            )
        }
    }

    suspend fun saveLoanApplication(application: LoanApplication) {
        dao.insertLoanApplication(application)
    }

    suspend fun updateLoanApplication(application: LoanApplication) {
        dao.updateLoanApplication(application)
    }

    suspend fun getCenterById(centerId: String): KendraCenter? = dao.getCenterById(centerId)

    fun getGroupsByCenter(centerId: String): Flow<List<JlgGroup>> = dao.getGroupsByCenter(centerId)

    fun getBorrowersByGroup(groupId: String): Flow<List<Borrower>> = dao.getBorrowersByGroup(groupId)

    suspend fun getBorrowerById(borrowerId: String): Borrower? = dao.getBorrowerById(borrowerId)

    suspend fun updateBorrower(borrower: Borrower) = dao.updateBorrower(borrower)

    suspend fun updateGroup(group: JlgGroup) = dao.updateGroup(group)

    suspend fun createJlgGroupWithBorrowers(
        groupName: String,
        centerId: String,
        mandal: MandalCluster,
        cycle: Int,
        borrowers: List<Borrower>
    ): JlgGroup {
        val uniqueSuffix = (100..999).random()
        val groupId = "JLG-${mandal.name.take(3)}-$uniqueSuffix"
        val cycleObj = LoanCycle.forCycle(cycle)
        val sanctionedAmt = cycleObj.minAmount.toDouble()

        val leader = borrowers.getOrNull(0)
        val coLeader = borrowers.getOrNull(1)

        val updatedBorrowers = borrowers.mapIndexed { index, b ->
            val isLeader = index == 0
            val isCoLeader = index == 1
            val title = when {
                isLeader -> " (Leader)"
                isCoLeader -> " (Co-Leader)"
                else -> ""
            }
            b.copy(
                borrowerId = "BOR-${mandal.name.take(3)}-${(1000..9999).random()}",
                groupId = groupId,
                fullName = if (b.fullName.contains("(")) b.fullName else "${b.fullName}$title",
                syncPending = true
            )
        }

        val newGroup = JlgGroup(
            groupId = groupId,
            groupName = groupName,
            centerId = centerId,
            mandal = mandal,
            leaderBorrowerId = updatedBorrowers.getOrNull(0)?.borrowerId ?: "",
            coLeaderBorrowerId = updatedBorrowers.getOrNull(1)?.borrowerId ?: "",
            loanCycle = cycle,
            sanctionedAmountPerMember = sanctionedAmt,
            cgt1Completed = false,
            cgt2Completed = false,
            cgt3Completed = false,
            grtApprovedByBm = false,
            crossGuaranteeConsented = true,
            isSanctioned = false,
            isDisbursed = false,
            syncPending = true
        )

        dao.insertGroup(newGroup)
        dao.insertBorrowers(updatedBorrowers)
        return newGroup
    }

    suspend fun recordRepayment(
        centerId: String,
        groupId: String,
        borrowerId: String,
        borrowerName: String,
        installmentDue: Double,
        amountPaid: Double,
        paymentMode: PaymentMode,
        attendance: AttendanceStatus,
        overdueReason: OverdueReason? = null,
        upiTxnId: String? = null,
        isCoveredByGuarantee: Boolean = false,
        geoLatitude: Double? = null,
        geoLongitude: Double? = null,
        locationAccuracyMeters: Float? = null,
        auditLocationAddress: String? = null
    ): RepaymentRecord {
        val receiptNumber = "REC-WNP-${SimpleDateFormat("yyMMdd", Locale.ENGLISH).format(Date())}-${(1000..9999).random()}"
        val record = RepaymentRecord(
            receiptNumber = receiptNumber,
            centerId = centerId,
            groupId = groupId,
            borrowerId = borrowerId,
            borrowerName = borrowerName,
            installmentDue = installmentDue,
            amountPaid = amountPaid,
            paymentMode = paymentMode,
            attendance = attendance,
            overdueReason = overdueReason,
            upiTxnId = upiTxnId,
            timestamp = System.currentTimeMillis(),
            isCoveredByGuarantee = isCoveredByGuarantee,
            syncPending = true,
            geoLatitude = geoLatitude,
            geoLongitude = geoLongitude,
            locationAccuracyMeters = locationAccuracyMeters,
            auditLocationAddress = auditLocationAddress
        )
        dao.insertRepayment(record)

        // Update collected amount in Kendra Center
        val center = dao.getCenterById(centerId)
        if (center != null) {
            val newCollected = center.collectedAmount + amountPaid
            dao.updateCenter(center.copy(collectedAmount = newCollected))
        }

        return record
    }

    fun calculateKfs(
        borrowerName: String,
        groupName: String,
        loanAmount: Double,
        loanCycle: Int
    ): KeyFactStatement {
        val feeRate = 0.015 // 1.5%
        val fee = loanAmount * feeRate
        val gst = fee * 0.18 // 18% GST
        val insurance = loanAmount * 0.012 // 1.2% credit shield insurance
        val netDisbursed = loanAmount - (fee + gst + insurance)

        // 24 fortnights (1 year), reducing balance 22% p.a.
        // Approx fortnightly installment formula
        val r = (0.22 / 24.0) // periodic rate
        val n = 24.0
        val installment = (loanAmount * r * Math.pow(1.0 + r, n)) / (Math.pow(1.0 + r, n) - 1.0)
        val totalRepayment = installment * 24.0
        val apr = 23.8 // Effective APR including processing fees and insurance

        return KeyFactStatement(
            borrowerName = borrowerName,
            groupName = groupName,
            loanAmount = loanAmount,
            loanCycle = loanCycle,
            tenureMonths = 12,
            numberOfInstallments = 24,
            repaymentFrequency = "Fortnightly",
            reducingInterestRatePa = 22.0,
            processingFeePercentage = 1.5,
            processingFeeAmount = fee,
            gstOnProcessingFee = gst,
            insurancePremium = insurance,
            netDisbursedAmount = netDisbursed,
            installmentAmount = Math.round(installment).toDouble(),
            totalRepaymentAmount = Math.round(totalRepayment).toDouble(),
            aprPercentage = apr
        )
    }

    suspend fun saveDenominations(
        n500: Int,
        n200: Int,
        n100: Int,
        n50: Int,
        n20: Int,
        n10: Int,
        systemExpectedCash: Double
    ): CashReconciliation {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val physicalTotal = (n500 * 500 + n200 * 200 + n100 * 100 + n50 * 50 + n20 * 20 + n10 * 10).toDouble()
        val variance = physicalTotal - systemExpectedCash
        val isZero = Math.abs(variance) < 0.01

        val recon = CashReconciliation(
            reconciliationDate = todayStr,
            branchCode = "TG-WNP-01",
            notes500 = n500,
            notes200 = n200,
            notes100 = n100,
            notes50 = n50,
            notes20 = n20,
            notes10 = n10,
            physicalCashTotal = physicalTotal,
            systemCashExpected = systemExpectedCash,
            varianceAmount = variance,
            isZeroVariance = isZero,
            tellerEmpId = "TEL-WNP-04",
            tellerOtp = "",
            isLockedAndVouchered = false,
            voucherNumber = null
        )
        dao.saveReconciliation(recon)
        return recon
    }

    suspend fun completeBranchTellerHandover(tellerOtp: String, existingRecon: CashReconciliation): CashReconciliation {
        val voucherNo = "CS-BRNET-JV-2026-WNP-${(10000..99999).random()}"
        val updated = existingRecon.copy(
            tellerOtp = tellerOtp,
            isLockedAndVouchered = true,
            voucherNumber = voucherNo,
            handoverTimestamp = System.currentTimeMillis()
        )
        dao.saveReconciliation(updated)
        return updated
    }

    suspend fun executeMorningSync(): String {
        // Simulates Craft Silicon BR.Net ESB morning sync for TG-WNP-01
        return "BR.Net ESB: Downloaded 6 Kendra Rosters, 5 JLG Groups, 25 Borrower profiles successfully."
    }

    suspend fun executeEveningSync(): Int {
        // Delta sync to Craft Silicon BR.Net ESB
        val repaymentsCount = dao.getPendingRepaymentsCount().firstOrNull() ?: 0
        val borrowersCount = dao.getPendingBorrowersCount().firstOrNull() ?: 0
        dao.markAllRepaymentsSynced()
        dao.markAllBorrowersSynced()
        dao.markAllGroupsSynced()
        return repaymentsCount + borrowersCount
    }

    companion object {
        fun formatInr(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            return format.format(amount)
        }

        fun calculateKfs(
            borrowerName: String,
            groupName: String,
            loanAmount: Double,
            loanCycle: Int
        ): KeyFactStatement {
            val feeRate = 0.015 // 1.5%
            val fee = loanAmount * feeRate
            val gst = fee * 0.18 // 18% GST
            val insurance = loanAmount * 0.012 // 1.2% credit shield insurance
            val netDisbursed = loanAmount - (fee + gst + insurance)

            // 24 fortnights (1 year), reducing balance 22% p.a.
            val r = (0.22 / 24.0) // periodic rate
            val n = 24.0
            val installment = (loanAmount * r * Math.pow(1.0 + r, n)) / (Math.pow(1.0 + r, n) - 1.0)
            val totalRepayment = installment * 24.0
            val apr = 23.8

            return KeyFactStatement(
                borrowerName = borrowerName,
                groupName = groupName,
                loanAmount = loanAmount,
                loanCycle = loanCycle,
                tenureMonths = 12,
                numberOfInstallments = 24,
                repaymentFrequency = "Fortnightly",
                reducingInterestRatePa = 22.0,
                processingFeePercentage = 1.5,
                processingFeeAmount = fee,
                gstOnProcessingFee = gst,
                insurancePremium = insurance,
                netDisbursedAmount = netDisbursed,
                installmentAmount = Math.round(installment).toDouble(),
                totalRepaymentAmount = Math.round(totalRepayment).toDouble(),
                aprPercentage = apr
            )
        }
    }
}
