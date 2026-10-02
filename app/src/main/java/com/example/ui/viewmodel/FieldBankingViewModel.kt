package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiLoanAssessment
import com.example.data.ai.GeminiLoanService
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceStatus
import com.example.data.model.Borrower
import com.example.data.model.CashReconciliation
import com.example.data.model.JlgGroup
import com.example.data.model.KendraCenter
import com.example.data.model.KeyFactStatement
import com.example.data.model.LoanApplication
import com.example.data.model.MandalCluster
import com.example.data.model.OverdueReason
import com.example.data.model.PaymentMode
import com.example.data.model.PendingRepaymentItem
import com.example.data.model.RepaymentRecord
import com.example.data.model.RepaymentStatus
import com.example.data.model.UserProfile
import com.example.data.repository.FieldBankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    LOGIN,
    DASHBOARD,
    FIELD_COLLECTIONS,
    DISRUB_SOURCING,
    DISRUB_NEW_JLG,
    DISRUB_CGT_GRT,
    LOAN_APPLICATION,
    COLLECT_CENTERS,
    COLLECT_MEETING,
    RECONCILIATION,
    SYNC_ENGINE
}

data class BankingUiState(
    val isLoggedIn: Boolean = false,
    val currentUser: UserProfile = UserProfile(),
    val centers: List<KendraCenter> = emptyList(),
    val groups: List<JlgGroup> = emptyList(),
    val borrowers: List<Borrower> = emptyList(),
    val repayments: List<RepaymentRecord> = emptyList(),
    val loanApplications: List<LoanApplication> = emptyList(),
    val pendingRepayments: List<PendingRepaymentItem> = emptyList(),
    val totalDistrictDemand: Double = 0.0,
    val totalDistrictCollected: Double = 0.0,
    val totalPendingDemand: Double = 0.0,
    val collectionsEfficiency: Double = 0.0,
    val pendingSyncCount: Int = 0,
    val todayReconciliation: CashReconciliation? = null,
    val currentScreen: AppScreen = AppScreen.LOGIN,
    val selectedCenterId: String? = null,
    val selectedGroupId: String? = null,
    val isOnlineMode: Boolean = true,
    val isTeluguLanguage: Boolean = false,
    val syncMessage: String? = null,
    val isSyncing: Boolean = false
)

class FieldBankingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FieldBankingRepository

    private val _isLoggedIn = MutableStateFlow(false)
    private val _currentUser = MutableStateFlow(UserProfile())
    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    private val _selectedCenterId = MutableStateFlow<String?>("KND-KTH-04")
    private val _selectedGroupId = MutableStateFlow<String?>("JLG-KTH-001")
    private val _isOnlineMode = MutableStateFlow(true)
    private val _isTeluguLanguage = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>("Connected to Saptadhan Wanaparthy Hub (TG-WNP-01)")
    private val _isSyncing = MutableStateFlow(false)

    // Active repayment state
    private val _lastIssuedReceipt = MutableStateFlow<RepaymentRecord?>(null)
    val lastIssuedReceipt = _lastIssuedReceipt.asStateFlow()

    private val _activeUpiBorrower = MutableStateFlow<Borrower?>(null)
    val activeUpiBorrower = _activeUpiBorrower.asStateFlow()

    private val _activeKfs = MutableStateFlow<KeyFactStatement?>(null)
    val activeKfs = _activeKfs.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FieldBankingRepository(db.fieldBankingDao())
        viewModelScope.launch {
            repository.initializeSeedDataIfNeeded()
        }
    }

    val uiState: StateFlow<BankingUiState> = combine(
        repository.allCenters,
        repository.allGroups,
        repository.allBorrowers,
        repository.allRepayments,
        repository.allLoanApplications,
        repository.pendingRepaymentsCount,
        repository.getTodayReconciliation(),
        _currentScreen,
        _selectedCenterId,
        _selectedGroupId,
        _isOnlineMode,
        _isTeluguLanguage,
        _syncMessage,
        _isSyncing,
        _isLoggedIn,
        _currentUser
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val centers = args[0] as List<KendraCenter>
        @Suppress("UNCHECKED_CAST")
        val groups = args[1] as List<JlgGroup>
        @Suppress("UNCHECKED_CAST")
        val borrowers = args[2] as List<Borrower>
        @Suppress("UNCHECKED_CAST")
        val repayments = args[3] as List<RepaymentRecord>
        @Suppress("UNCHECKED_CAST")
        val loanApps = args[4] as List<LoanApplication>
        val pendingRepayments = args[5] as Int
        val recon = args[6] as? CashReconciliation
        val screen = args[7] as AppScreen
        val centerId = args[8] as? String
        val groupId = args[9] as? String
        val isOnline = args[10] as Boolean
        val isTelugu = args[11] as Boolean
        val syncMsg = args[12] as? String
        val syncing = args[13] as Boolean
        val loggedIn = args[14] as Boolean
        val user = args[15] as UserProfile

        val pendingItems = repository.computePendingRepayments(centers, groups, borrowers, repayments)
        val totalDemand = centers.sumOf { it.expectedDemand }
        val totalCollected = centers.sumOf { it.collectedAmount }
        val totalPending = pendingItems.sumOf { it.pendingAmount }
        val efficiency = if (totalDemand > 0) Math.round((totalCollected / totalDemand) * 1000.0) / 10.0 else 0.0

        BankingUiState(
            isLoggedIn = loggedIn,
            currentUser = user,
            centers = centers,
            groups = groups,
            borrowers = borrowers,
            repayments = repayments,
            loanApplications = loanApps,
            pendingRepayments = pendingItems,
            totalDistrictDemand = totalDemand,
            totalDistrictCollected = totalCollected,
            totalPendingDemand = totalPending,
            collectionsEfficiency = efficiency,
            pendingSyncCount = pendingRepayments,
            todayReconciliation = recon,
            currentScreen = screen,
            selectedCenterId = centerId,
            selectedGroupId = groupId,
            isOnlineMode = isOnline,
            isTeluguLanguage = isTelugu,
            syncMessage = syncMsg,
            isSyncing = syncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BankingUiState()
    )

    fun submitLoanApplication(application: LoanApplication) {
        viewModelScope.launch {
            repository.saveLoanApplication(application)
            _syncMessage.value = "Loan application for ${application.applicantName} saved to Room database (Status: ${application.status})."
        }
    }

    fun evaluateLoanApplicationWithAi(
        application: LoanApplication,
        onResult: (AiLoanAssessment) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val assessment = GeminiLoanService.analyzeLoanApplication(application)
            _isSyncing.value = false
            onResult(assessment)
        }
    }

    fun enhancePurposeWithAi(
        applicantName: String,
        mandal: MandalCluster,
        rawPurpose: String,
        amount: Double,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val polished = GeminiLoanService.enhanceLoanPurpose(applicantName, mandal, rawPurpose, amount)
            onResult(polished)
        }
    }

    fun login(officerCode: String, role: String) {
        _isLoggedIn.value = true
        _currentUser.value = UserProfile(
            employeeId = officerCode,
            fullName = if (officerCode.contains("08") || officerCode.contains("FCO")) "K. Srinivas" else "S. Rajesh Kumar",
            role = role,
            branchCode = "TG-WNP-01",
            branchName = "Wanaparthy District Hub",
            companyName = "Saptadhan Finance Private Limited",
            brandName = "Saptadhan",
            domain = "saptadhan.com"
        )
        _currentScreen.value = AppScreen.DASHBOARD
        _syncMessage.value = "Welcome $officerCode. Connected to Saptadhan Wanaparthy Hub."
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentScreen.value = AppScreen.LOGIN
        _syncMessage.value = "Logged out of Saptadhan."
    }

    fun collectPendingRepayment(
        item: PendingRepaymentItem,
        amountPaid: Double,
        mode: PaymentMode,
        overdueReason: OverdueReason?,
        isMutualGuarantee: Boolean,
        geoLat: Double? = null,
        geoLong: Double? = null,
        locationAccuracy: Float? = null,
        auditAddress: String? = null
    ) {
        viewModelScope.launch {
            val receipt = repository.recordRepayment(
                centerId = item.centerId,
                groupId = item.groupId,
                borrowerId = item.borrowerId,
                borrowerName = item.borrowerName,
                installmentDue = item.installmentDue,
                amountPaid = amountPaid,
                paymentMode = mode,
                attendance = item.attendance,
                overdueReason = overdueReason,
                upiTxnId = if (mode == PaymentMode.DYNAMIC_UPI_QR) "UPI-NPCI-${(100000000000L..999999999999L).random()}" else null,
                isCoveredByGuarantee = isMutualGuarantee,
                geoLatitude = geoLat,
                geoLongitude = geoLong,
                locationAccuracyMeters = locationAccuracy,
                auditLocationAddress = auditAddress
            )
            _lastIssuedReceipt.value = receipt
            _syncMessage.value = "Payment ₹${amountPaid.toInt()} saved in Room DB with GPS Audit for ${item.borrowerName}."
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectCenter(centerId: String) {
        _selectedCenterId.value = centerId
    }

    fun selectGroup(groupId: String) {
        _selectedGroupId.value = groupId
    }

    fun toggleOnlineMode() {
        val newState = !_isOnlineMode.value
        _isOnlineMode.value = newState
        _syncMessage.value = if (newState) {
            "Switched to Online Mode (BR.Net ESB Gateway Active)"
        } else {
            "Switched to Offline-First Mode (Local Encrypted Room Storage)"
        }
    }

    fun toggleLanguage() {
        _isTeluguLanguage.value = !_isTeluguLanguage.value
    }

    // DISRUB: Create JLG Group with 5 female borrowers
    fun createJlgGroup(
        groupName: String,
        centerId: String,
        mandal: MandalCluster,
        cycle: Int,
        borrowerList: List<Borrower>
    ) {
        viewModelScope.launch {
            repository.createJlgGroupWithBorrowers(
                groupName = groupName,
                centerId = centerId,
                mandal = mandal,
                cycle = cycle,
                borrowers = borrowerList
            )
            _syncMessage.value = "New JLG '$groupName' registered for CGT in Wanaparthy Cluster."
            _currentScreen.value = AppScreen.DISRUB_SOURCING
        }
    }

    // DISRUB: e-KYC update
    fun updateBorrowerKyc(
        borrower: Borrower,
        pan: String,
        income: Double,
        geoLat: Double,
        geoLong: Double
    ) {
        viewModelScope.launch {
            val monthly = income / 12.0
            val foir = if (monthly > 0) ((borrower.existingMonthlyDebtObligations + 1800.0) / monthly) * 100.0 else 0.0
            val updated = borrower.copy(
                panOrForm60 = pan,
                annualHouseholdIncome = income,
                monthlyHouseholdIncome = monthly,
                geoLatitude = geoLat,
                geoLongitude = geoLong,
                isKycVerified = true,
                foirPercentage = Math.round(foir * 10.0) / 10.0,
                syncPending = true
            )
            repository.updateBorrower(updated)
            _syncMessage.value = "e-KYC verified with Aadhaar XML & GPS coordinates for ${borrower.fullName}"
        }
    }

    // DISRUB: Credit bureau evaluation
    fun runCreditBureauCheck(borrower: Borrower) {
        viewModelScope.launch {
            // CRIF High Mark & CIBIL MFI criteria: score >= 700, active MFIs <= 3, FOIR <= 50%
            val isEligible = borrower.crifCreditScore >= 700 &&
                    borrower.activeMfiCount <= 3 &&
                    borrower.annualHouseholdIncome <= 300000.0 &&
                    borrower.foirPercentage <= 50.0

            val updated = borrower.copy(
                isBureauPassed = isEligible,
                syncPending = true
            )
            repository.updateBorrower(updated)
            _syncMessage.value = if (isEligible) {
                "CRIF Score ${borrower.crifCreditScore}: Green-Channel Straight-Through Approval!"
            } else {
                "CRIF Check Flagged: Review active MFI obligations & household income"
            }
        }
    }

    // DISRUB: Update CGT & GRT
    fun updateCgtStatus(groupId: String, milestone: Int) {
        viewModelScope.launch {
            val group = uiState.value.groups.find { it.groupId == groupId } ?: return@launch
            val updated = when (milestone) {
                1 -> group.copy(cgt1Completed = true, syncPending = true)
                2 -> group.copy(cgt2Completed = true, syncPending = true)
                3 -> group.copy(cgt3Completed = true, syncPending = true)
                else -> group
            }
            repository.updateGroup(updated)
            _syncMessage.value = "CGT $milestone attendance & literacy milestone recorded for ${group.groupName}"
        }
    }

    fun approveGrt(groupId: String, bmName: String) {
        viewModelScope.launch {
            val group = uiState.value.groups.find { it.groupId == groupId } ?: return@launch
            val updated = group.copy(
                grtApprovedByBm = true,
                bmName = bmName,
                isSanctioned = true,
                isDisbursed = true,
                syncPending = true
            )
            repository.updateGroup(updated)
            _syncMessage.value = "Branch Manager GRT Approved! Sanction Letter & NACH Disbursement generated."
        }
    }

    fun openKfs(borrower: Borrower, group: JlgGroup) {
        val kfs = repository.calculateKfs(
            borrowerName = borrower.fullName,
            groupName = group.groupName,
            loanAmount = group.sanctionedAmountPerMember,
            loanCycle = group.loanCycle
        )
        _activeKfs.value = kfs
    }

    fun closeKfs() {
        _activeKfs.value = null
    }

    // COLLECT: Record Repayment
    fun collectCashRepayment(
        centerId: String,
        groupId: String,
        borrower: Borrower,
        amount: Double,
        attendance: AttendanceStatus
    ) {
        viewModelScope.launch {
            val receipt = repository.recordRepayment(
                centerId = centerId,
                groupId = groupId,
                borrowerId = borrower.borrowerId,
                borrowerName = borrower.fullName,
                installmentDue = amount,
                amountPaid = amount,
                paymentMode = PaymentMode.CASH,
                attendance = attendance
            )
            _lastIssuedReceipt.value = receipt
            _syncMessage.value = "Cash collected ₹${amount.toInt()} from ${borrower.fullName}. Receipt generated."
        }
    }

    fun prepareDynamicUpi(borrower: Borrower) {
        _activeUpiBorrower.value = borrower
    }

    fun dismissUpiDialog() {
        _activeUpiBorrower.value = null
    }

    fun completeUpiPayment(
        centerId: String,
        groupId: String,
        borrower: Borrower,
        amount: Double,
        appType: String
    ) {
        viewModelScope.launch {
            val txnId = "UPI-NPCI-${(100000000000L..999999999999L).random()}"
            val receipt = repository.recordRepayment(
                centerId = centerId,
                groupId = groupId,
                borrowerId = borrower.borrowerId,
                borrowerName = borrower.fullName,
                installmentDue = amount,
                amountPaid = amount,
                paymentMode = PaymentMode.DYNAMIC_UPI_QR,
                attendance = AttendanceStatus.PRESENT,
                upiTxnId = "$appType:$txnId"
            )
            _activeUpiBorrower.value = null
            _lastIssuedReceipt.value = receipt
            _syncMessage.value = "NPCI UPI Webhook Verified via $appType! ₹${amount.toInt()} marked as PAID."
        }
    }

    fun recordDelinquency(
        centerId: String,
        groupId: String,
        borrower: Borrower,
        installment: Double,
        reason: OverdueReason,
        mutualGuaranteeCovered: Boolean
    ) {
        viewModelScope.launch {
            val receipt = repository.recordRepayment(
                centerId = centerId,
                groupId = groupId,
                borrowerId = borrower.borrowerId,
                borrowerName = borrower.fullName,
                installmentDue = installment,
                amountPaid = if (mutualGuaranteeCovered) installment else 0.0,
                paymentMode = if (mutualGuaranteeCovered) PaymentMode.MUTUAL_GUARANTEE else PaymentMode.CASH,
                attendance = AttendanceStatus.ABSENT,
                overdueReason = reason,
                isCoveredByGuarantee = mutualGuaranteeCovered
            )
            _lastIssuedReceipt.value = receipt
            _syncMessage.value = if (mutualGuaranteeCovered) {
                "JLG Mutual Guarantee Protocol Activated: ₹${installment.toInt()} covered by group!"
            } else {
                "Delinquency logged for ${borrower.fullName}. Reason: ${reason.label}"
            }
        }
    }

    fun dismissReceipt() {
        _lastIssuedReceipt.value = null
    }

    // RECONCILIATION: Save Denominations
    fun calculateDenominations(
        n500: Int,
        n200: Int,
        n100: Int,
        n50: Int,
        n20: Int,
        n10: Int,
        systemExpectedCash: Double
    ) {
        viewModelScope.launch {
            val recon = repository.saveDenominations(
                n500 = n500,
                n200 = n200,
                n100 = n100,
                n50 = n50,
                n20 = n20,
                n10 = n10,
                systemExpectedCash = systemExpectedCash
            )
            _syncMessage.value = if (recon.isZeroVariance) {
                "Zero Cash Variance Achieved! Ready for Branch Vault Drop."
            } else {
                "Variance detected: ₹${recon.varianceAmount.toInt()}. Verify physical cash bag."
            }
        }
    }

    fun handoverToBranchTeller(tellerOtp: String, existingRecon: CashReconciliation) {
        viewModelScope.launch {
            val updated = repository.completeBranchTellerHandover(tellerOtp, existingRecon)
            _syncMessage.value = "Handover complete! Core Ledger Voucher ${updated.voucherNumber} created in Craft Silicon BR.Net."
        }
    }

    // SYNC OPERATIONS
    fun performMorningSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.executeMorningSync()
            _isSyncing.value = false
            _syncMessage.value = res
        }
    }

    fun performEveningSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val syncedCount = repository.executeEveningSync()
            _isSyncing.value = false
            _syncMessage.value = "Evening Sync Complete: Pushed $syncedCount offline records to BR.Net ESB."
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
