package com.example.ui.screens.collect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.Borrower
import com.example.data.model.OverdueReason
import com.example.data.model.PaymentMode
import com.example.data.model.RepaymentRecord
import com.example.data.repository.FieldBankingRepository
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import com.example.ui.viewmodel.BankingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KendraMeetingScreen(
    state: BankingUiState,
    centerId: String?,
    onNavigateBack: () -> Unit,
    onCollectCash: (String, String, Borrower, Double, AttendanceStatus) -> Unit,
    onOpenDynamicUpi: (Borrower) -> Unit,
    onCompleteUpiPayment: (String, String, Borrower, Double, String) -> Unit,
    onRecordDelinquency: (String, String, Borrower, Double, OverdueReason, Boolean) -> Unit,
    onDismissReceipt: () -> Unit
) {
    val isTelugu = state.isTeluguLanguage
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val rawCenter = state.centers.find { it.centerId == centerId } ?: state.centers.firstOrNull()
    if (rawCenter == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Center not found")
        }
        return
    }
    val center = rawCenter
    val centerGroups = state.groups.filter { it.centerId == center.centerId }
    val centerBorrowers = state.borrowers.filter { b -> centerGroups.any { it.groupId == b.groupId } }

    // Repayments for this center
    val centerRepayments = state.repayments.filter { it.centerId == center.centerId }

    // Attendance map
    val attendanceMap = remember { mutableStateMapOf<String, AttendanceStatus>() }
    LaunchedEffect(centerBorrowers) {
        centerBorrowers.forEach { b ->
            if (!attendanceMap.containsKey(b.borrowerId)) {
                attendanceMap[b.borrowerId] = AttendanceStatus.PRESENT
            }
        }
    }

    // Modal state for Delinquency
    var borrowerForDelinquency by remember { mutableStateOf<Borrower?>(null) }
    var selectedReason by remember { mutableStateOf(OverdueReason.COTTON_HARVEST_DELAY) }
    var activateMutualGuarantee by remember { mutableStateOf(false) }

    // Dynamic UPI Modal state
    var upiBorrower by remember { mutableStateOf<Borrower?>(null) }
    var upiSimulatingApp by remember { mutableStateOf<String?>(null) }

    // Handheld thermal printer dialog
    var receiptForPrint by remember { mutableStateOf<RepaymentRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = center?.centerName ?: "Kendra Meeting",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${center?.villageName} • ${center?.meetingTime}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_meeting_back")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val totalExpected = center.expectedDemand
        val totalCollectedNow = centerRepayments.sumOf { it.amountPaid }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Live Center Demand vs Collected Header Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = BankPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isTelugu) "ఈ కేంద్రం డిమాండ్" else "Kendra Expected Demand",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = FieldBankingRepository.formatInr(totalExpected),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isTelugu) "వసూలైన మొత్తం" else "Collected Live",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = FieldBankingRepository.formatInr(totalCollectedNow),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78F8D4)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Paid: ${centerRepayments.count { it.amountPaid > 0 }}/${centerBorrowers.size} Borrowers",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Branch: TG-WNP-01",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Member Roster & Meeting Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTelugu) "సభ్యుల హాజరు & వాయిదా సేకరణ" else "Borrower Attendance & Installments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(centerBorrowers) { borrower ->
                val group = centerGroups.find { it.groupId == borrower.groupId }
                val pastRepayment = centerRepayments.find { it.borrowerId == borrower.borrowerId }
                val isPaid = pastRepayment != null && pastRepayment.amountPaid > 0
                val currentAttendance = attendanceMap[borrower.borrowerId] ?: AttendanceStatus.PRESENT

                // Installment standard calculation: approx 2150 per fortnight
                val installmentDue = 2150.0

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrower_meeting_row_${borrower.borrowerId}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isPaid) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = borrower.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${borrower.borrowerId} • ${group?.groupName ?: ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = FieldBankingRepository.formatInr(installmentDue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isPaid) RupeeGreen else MaterialTheme.colorScheme.primary
                                )
                                if (isPaid) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = if (pastRepayment.paymentMode == PaymentMode.DYNAMIC_UPI_QR) "PAID (UPI)" else "PAID (CASH)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Due Today",
                                        fontSize = 11.sp,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Attendance Selector (Present / Absent / Proxy)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTelugu) "హాజరు:" else "Attendance:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AttendanceChip(
                                    label = if (isTelugu) "హాజరు" else "Present",
                                    selected = currentAttendance == AttendanceStatus.PRESENT,
                                    onSelect = { attendanceMap[borrower.borrowerId] = AttendanceStatus.PRESENT }
                                )
                                AttendanceChip(
                                    label = if (isTelugu) "గైర్హాజరు" else "Absent",
                                    selected = currentAttendance == AttendanceStatus.ABSENT,
                                    onSelect = { attendanceMap[borrower.borrowerId] = AttendanceStatus.ABSENT }
                                )
                                AttendanceChip(
                                    label = if (isTelugu) "ప్రాక్సీ" else "Proxy",
                                    selected = currentAttendance == AttendanceStatus.PROXY,
                                    onSelect = { attendanceMap[borrower.borrowerId] = AttendanceStatus.PROXY }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isPaid) {
                            // Paid State with Thermal Print Slip CTA
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RupeeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Receipt: ${pastRepayment.receiptNumber}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF166534)
                                    )
                                }

                                OutlinedButton(
                                    onClick = { receiptForPrint = pastRepayment },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("btn_print_receipt_${borrower.borrowerId}")
                                ) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Print Slip", fontSize = 11.sp)
                                }
                            }
                        } else {
                            // Dual-Mode Repayment Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Cash Mode
                                Button(
                                    onClick = {
                                        onCollectCash(
                                            center.centerId,
                                            borrower.groupId,
                                            borrower,
                                            installmentDue,
                                            currentAttendance
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Collected ₹${installmentDue.toInt()} Cash from ${borrower.fullName}")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_cash_${borrower.borrowerId}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cash (నగదు)", fontSize = 11.sp)
                                }

                                // Dynamic UPI QR Mode
                                Button(
                                    onClick = {
                                        upiBorrower = borrower
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_upi_${borrower.borrowerId}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("UPI QR", fontSize = 11.sp)
                                }

                                // Default / Overdue Tagging
                                OutlinedButton(
                                    onClick = {
                                        borrowerForDelinquency = borrower
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_overdue_${borrower.borrowerId}"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFDC2626))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Default", fontSize = 11.sp, color = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Dynamic UPI QR Modal with simulated PhonePe / GPay webhook
    if (upiBorrower != null) {
        val targetBorrower = upiBorrower!!
        val installmentDue = 2150.0

        AlertDialog(
            onDismissRequest = {
                upiBorrower = null
                upiSimulatingApp = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF0284C7))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dynamic NPCI UPI QR Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Scan to Pay: ${FieldBankingRepository.formatInr(installmentDue)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "VPA: craftsilicon.tgwnp01@icici\nBorrower: ${targetBorrower.fullName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Simulated Visual QR Code Pattern
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .border(2.dp, Color(0xFF0284C7), RoundedCornerShape(12.dp))
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "NPCI QR",
                                modifier = Modifier.size(90.dp),
                                tint = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NPCI Bharat QR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    if (upiSimulatingApp != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Awaiting Webhook Callback from $upiSimulatingApp...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "Customer can scan with PhonePe, Google Pay, BHIM, or Paytm:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    upiSimulatingApp = "PhonePe"
                                    scope.launch {
                                        delay(1200)
                                        onCompleteUpiPayment(center.centerId, targetBorrower.groupId, targetBorrower, installmentDue, "PhonePe")
                                        upiBorrower = null
                                        upiSimulatingApp = null
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_simulate_phonepe"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F259F)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("PhonePe", fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    upiSimulatingApp = "Google Pay"
                                    scope.launch {
                                        delay(1200)
                                        onCompleteUpiPayment(center.centerId, targetBorrower.groupId, targetBorrower, installmentDue, "GooglePay")
                                        upiBorrower = null
                                        upiSimulatingApp = null
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_simulate_gpay"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("GPay", fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    upiSimulatingApp = "Paytm"
                                    scope.launch {
                                        delay(1200)
                                        onCompleteUpiPayment(center.centerId, targetBorrower.groupId, targetBorrower, installmentDue, "Paytm")
                                        upiBorrower = null
                                        upiSimulatingApp = null
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_simulate_paytm"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B9F1)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Paytm", fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { upiBorrower = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delinquency / Overdue Tagging Dialog with Mutual Guarantee Protocol
    if (borrowerForDelinquency != null) {
        val targetBorrower = borrowerForDelinquency!!
        val installmentDue = 2150.0

        AlertDialog(
            onDismissRequest = { borrowerForDelinquency = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ReportProblem, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delinquency & Guarantee Protocol", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Member: ${targetBorrower.fullName} (Due: ₹${installmentDue.toInt()})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "Standard Reason Code:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    OverdueReason.values().take(5).forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${reason.code}: ${reason.label}", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider()

                    // Mutual Guarantee trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activateMutualGuarantee = !activateMutualGuarantee },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = activateMutualGuarantee,
                            onClick = { activateMutualGuarantee = !activateMutualGuarantee }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Trigger JLG Mutual Guarantee",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "4 group members contribute ₹538 each to cover this installment",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordDelinquency(
                            center.centerId,
                            targetBorrower.groupId,
                            targetBorrower,
                            installmentDue,
                            selectedReason,
                            activateMutualGuarantee
                        )
                        borrowerForDelinquency = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (activateMutualGuarantee) "Mutual guarantee protocol recorded for ${targetBorrower.fullName}"
                                else "Default logged for ${targetBorrower.fullName}"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activateMutualGuarantee) RupeeGreen else Color(0xFFDC2626)
                    )
                ) {
                    Text(if (activateMutualGuarantee) "Apply Guarantee Payment" else "Log Unpaid Delinquency")
                }
            },
            dismissButton = {
                TextButton(onClick = { borrowerForDelinquency = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Handheld Bluetooth Thermal Printer Slip Dialog (58mm ESC/POS)
    if (receiptForPrint != null) {
        val rec = receiptForPrint!!
        AlertDialog(
            onDismissRequest = { receiptForPrint = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = BankPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bluetooth Thermal Slip Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "================================\n" +
                                "    CRAFT SILICON BR.NET MFI    \n" +
                                "      TG-WNP-01 WANAPARTHY      \n" +
                                "--------------------------------\n" +
                                "RECEIPT : ${rec.receiptNumber}\n" +
                                "CENTER  : ${center.centerName}\n" +
                                "BORROWER: ${rec.borrowerName}\n" +
                                "BORR ID : ${rec.borrowerId}\n" +
                                "PAY MODE: ${rec.paymentMode.name}\n" +
                                "AMOUNT  : INR ₹${rec.amountPaid.toInt()}.00\n" +
                                (if (rec.upiTxnId != null) "UPI TXN : ${rec.upiTxnId}\n" else "") +
                                (if (rec.isCoveredByGuarantee) "NOTE    : JLG MUTUAL GUARANTEE\n" else "") +
                                "STATUS  : TRANSACTION SUCCESS\n" +
                                "--------------------------------\n" +
                                "REGIONAL TELUGU SMS SENT TO MOB \n" +
                                "ధన్యవాదాలు - వనపర్తి బ్రాంచ్     \n" +
                                "================================",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        receiptForPrint = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Print sent to Bluetooth Thermal Printer (ESC/POS 58mm).")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send to BT Printer")
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptForPrint = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AttendanceChip(label: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) BankPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
