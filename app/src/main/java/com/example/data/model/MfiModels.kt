package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Wanaparthy District clusters recognized by Craft Silicon BR.Net Hub TG-WNP-01
 */
enum class MandalCluster(val displayName: String, val description: String) {
    KOTHAKOTA("Kothakota", "High JLG density, cotton & groundnut farming"),
    GOPALPET("Gopalpet", "Dairy farming & small retail clusters"),
    PEBBAIR("Pebbair", "Commercial highway retail & poultry hubs"),
    PANGAL("Pangal", "Rural micro-enterprises & weaving"),
    PEDDAMANDADI("Peddamandadi", "Agrarian SHGs & allied services"),
    WANAPARTHY_TOWN("Wanaparthy Town/Rural", "Peri-urban micro-enterprises")
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    PROXY
}

enum class PaymentMode {
    CASH,
    DYNAMIC_UPI_QR,
    MUTUAL_GUARANTEE
}

enum class OverdueReason(val code: String, val label: String) {
    COTTON_HARVEST_DELAY("CR-01", "Cotton Harvest Delay"),
    MEDICAL_EMERGENCY("MD-02", "Medical Emergency in Family"),
    MIGRATION("MG-03", "Seasonal Migration to Hyderabad"),
    CROP_FAILURE("CF-04", "Groundnut / Paddy Crop Failure"),
    LIVESTOCK_LOSS("LS-05", "Dairy Cattle / Poultry Loss"),
    FAMILY_FUNCTION("FF-06", "Marriage / Social Function"),
    OTHER("OT-99", "Other Verified Delay")
}

enum class LoanCycle(val cycleNumber: Int, val minAmount: Int, val maxAmount: Int) {
    CYCLE_1(1, 25000, 35000),
    CYCLE_2(2, 40000, 50000),
    CYCLE_3(3, 55000, 70000),
    CYCLE_4(4, 75000, 100000);

    companion object {
        fun forCycle(cycle: Int): LoanCycle = when (cycle) {
            1 -> CYCLE_1
            2 -> CYCLE_2
            3 -> CYCLE_3
            else -> CYCLE_4
        }
    }
}

@Entity(tableName = "kendra_centers")
data class KendraCenter(
    @PrimaryKey val centerId: String, // e.g. "KND-KTH-04"
    val centerName: String,           // e.g. "Kothakota Center #04"
    val mandal: MandalCluster,
    val meetingDay: String,           // e.g. "Every Tuesday"
    val meetingTime: String,          // e.g. "07:30 AM"
    val villageName: String,
    val expectedDemand: Double,
    val collectedAmount: Double,
    val totalBorrowers: Int,
    val centerLeaderName: String,
    val centerLeaderMobile: String,
    val isMeetingCompleted: Boolean = false
)

@Entity(tableName = "jlg_groups")
data class JlgGroup(
    @PrimaryKey val groupId: String,  // e.g. "JLG-WNP-012"
    val groupName: String,            // e.g. "Sri Lakshmi Mahila Sangham"
    val centerId: String,
    val mandal: MandalCluster,
    val leaderBorrowerId: String,
    val coLeaderBorrowerId: String,
    val loanCycle: Int,
    val sanctionedAmountPerMember: Double,
    val cgt1Completed: Boolean = false,
    val cgt2Completed: Boolean = false,
    val cgt3Completed: Boolean = false,
    val grtApprovedByBm: Boolean = false,
    val bmName: String = "S. Rajesh Kumar (BM)",
    val crossGuaranteeConsented: Boolean = false,
    val isSanctioned: Boolean = false,
    val isDisbursed: Boolean = false,
    val syncPending: Boolean = false
)

@Entity(tableName = "borrowers")
data class Borrower(
    @PrimaryKey val borrowerId: String, // e.g. "BOR-WNP-1001"
    val groupId: String,
    val fullName: String,
    val spouseOrFatherName: String,
    val aadhaarMasked: String,          // e.g. "XXXX-XXXX-4921"
    val panOrForm60: String,            // e.g. "ABCDE1234F" or "Form-60"
    val mobileNumber: String,
    val village: String,
    val mandal: MandalCluster,
    val annualHouseholdIncome: Double,  // Must be <= 3,00,000 for RBI 2022 MFI
    val monthlyHouseholdIncome: Double,
    val existingMonthlyDebtObligations: Double,
    val crifCreditScore: Int,           // 700+ for straight-through approval
    val activeMfiCount: Int,            // Max 3 active MFIs per RBI guidelines
    val geoLatitude: Double,
    val geoLongitude: Double,
    val bankAccountNumberMasked: String,
    val bankIfsc: String,
    val bankName: String,
    val isKycVerified: Boolean = false,
    val isBureauPassed: Boolean = false,
    val foirPercentage: Double = 0.0,   // FOIR <= 50%
    val syncPending: Boolean = false
)

@Entity(tableName = "repayment_records")
data class RepaymentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,         // e.g. "REC-WNP-2026-98102"
    val centerId: String,
    val groupId: String,
    val borrowerId: String,
    val borrowerName: String,
    val installmentDue: Double,
    val amountPaid: Double,
    val paymentMode: PaymentMode,
    val attendance: AttendanceStatus,
    val overdueReason: OverdueReason? = null,
    val upiTxnId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isCoveredByGuarantee: Boolean = false,
    val syncPending: Boolean = true,
    val geoLatitude: Double? = null,
    val geoLongitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val auditLocationAddress: String? = null
)

@Entity(tableName = "cash_reconciliation")
data class CashReconciliation(
    @PrimaryKey val reconciliationDate: String, // e.g. "2026-10-02"
    val branchCode: String = "TG-WNP-01",
    val notes500: Int = 0,
    val notes200: Int = 0,
    val notes100: Int = 0,
    val notes50: Int = 0,
    val notes20: Int = 0,
    val notes10: Int = 0,
    val physicalCashTotal: Double = 0.0,
    val systemCashExpected: Double = 0.0,
    val varianceAmount: Double = 0.0,
    val isZeroVariance: Boolean = false,
    val tellerEmpId: String = "TEL-WNP-04",
    val tellerOtp: String = "",
    val isLockedAndVouchered: Boolean = false,
    val voucherNumber: String? = null,
    val handoverTimestamp: Long? = null
)

data class KeyFactStatement(
    val borrowerName: String,
    val groupName: String,
    val loanAmount: Double,
    val loanCycle: Int,
    val tenureMonths: Int = 12,
    val numberOfInstallments: Int = 24, // Fortnightly
    val repaymentFrequency: String = "Fortnightly",
    val reducingInterestRatePa: Double = 22.0, // 20% - 24% p.a.
    val processingFeePercentage: Double = 1.5, // 1% - 2%
    val processingFeeAmount: Double,
    val gstOnProcessingFee: Double,
    val insurancePremium: Double,
    val netDisbursedAmount: Double,
    val installmentAmount: Double,
    val totalRepaymentAmount: Double,
    val aprPercentage: Double,
    val prepaymentPenalty: String = "NIL (0% as per RBI MFI Master Directions 2022)"
)

@Entity(tableName = "loan_applications")
data class LoanApplication(
    @PrimaryKey val applicationId: String, // e.g. "APP-WNP-2026-1049"
    val applicantName: String,
    val mobileNumber: String,
    val aadhaarMasked: String,
    val mandal: MandalCluster,
    val village: String,
    val requestedAmount: Double,
    val loanPurpose: String,
    val annualIncome: Double,
    val monthlyIncome: Double,
    val existingMonthlyDebt: Double,
    val foirPercentage: Double,
    val loanCycle: Int,
    val status: String = "UNDER_REVIEW", // "APPROVED", "CONDITIONALLY_APPROVED", "UNDER_REVIEW", "REJECTED"
    val aiRiskScore: Int = 0,
    val aiAssessmentSummary: String? = null,
    val aiRecommendation: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val syncPending: Boolean = true
)

enum class RepaymentStatus(val displayName: String) {
    DUE_TODAY("Due Today"),
    OVERDUE("Overdue"),
    PARTIALLY_PAID("Partially Paid"),
    DELINQUENT("Delinquent (Cross-Guarantee Active)"),
    PAID("Collected")
}

data class PendingRepaymentItem(
    val borrowerId: String,
    val borrowerName: String,
    val mobileNumber: String,
    val aadhaarMasked: String,
    val groupId: String,
    val groupName: String,
    val centerId: String,
    val centerName: String,
    val mandal: MandalCluster,
    val village: String,
    val loanCycle: Int,
    val sanctionedAmount: Double,
    val installmentDue: Double,
    val amountPaid: Double,
    val pendingAmount: Double,
    val meetingDay: String,
    val meetingTime: String,
    val overdueDays: Int = 0,
    val isDelinquent: Boolean = false,
    val overdueReason: OverdueReason? = null,
    val isCoveredByGuarantee: Boolean = false,
    val attendance: AttendanceStatus = AttendanceStatus.PRESENT,
    val status: RepaymentStatus = RepaymentStatus.DUE_TODAY,
    val totalPaidHistorical: Double = 0.0,
    val previousPaymentsCount: Int = 0
)

data class UserProfile(
    val employeeId: String = "FCO-WNP-08",
    val fullName: String = "K. Srinivas",
    val role: String = "Senior Field Credit Officer",
    val branchCode: String = "TG-WNP-01",
    val branchName: String = "Wanaparthy District Hub",
    val companyName: String = "Saptadhan Finance Private Limited",
    val brandName: String = "Saptadhan",
    val domain: String = "saptadhan.com",
    val mobileNumber: String = "+91 98480 55112"
)


