package com.example.ui.screens.collect

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.PaymentMode
import com.example.data.model.PendingRepaymentItem
import com.example.data.model.RepaymentRecord
import com.example.data.repository.FieldBankingRepository
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowerRepaymentHistoryView(
    selectedBorrower: PendingRepaymentItem,
    allBorrowers: List<PendingRepaymentItem>,
    repaymentRecords: List<RepaymentRecord>,
    isTelugu: Boolean,
    onSelectBorrower: (PendingRepaymentItem) -> Unit,
    onClose: () -> Unit,
    onPrintReceipt: (RepaymentRecord) -> Unit,
    onCollectPayment: (PendingRepaymentItem) -> Unit
) {
    val context = LocalContext.current
    var filterMode by remember { mutableStateOf<PaymentMode?>(null) }
    var sortDescending by remember { mutableStateOf(true) }

    // Support device back button
    BackHandler(onBack = onClose)

    // Filter repayment records for the selected borrower
    val borrowerHistory = repaymentRecords.filter { record ->
        record.borrowerId == selectedBorrower.borrowerId ||
                record.borrowerName.equals(selectedBorrower.borrowerName, ignoreCase = true)
    }.filter { record ->
        filterMode == null || record.paymentMode == filterMode
    }.sortedWith(
        if (sortDescending) compareByDescending { it.timestamp } else compareBy { it.timestamp }
    )

    val totalHistoricalPaid = borrowerHistory.sumOf { it.amountPaid }
    val paidInstallmentCount = borrowerHistory.count { it.amountPaid > 0 }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.ENGLISH) }
    val shortDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isTelugu) "రుణ చెల్లింపుల చరిత్ర" else "Repayment History",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = WanaparthyGold.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "Wanaparthy District",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BankPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Saptadhan Finance Private Limited • saptadhan.com",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close_repayment_history")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Borrower Selector Chips Row
            item {
                Text(
                    text = if (isTelugu) "లబ్ధిదారుల ఎంపిక (వనపర్తి క్లస్టర్)" else "Switch Borrower (Wanaparthy Cluster)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("row_borrower_history_selector")
                ) {
                    items(allBorrowers) { borrower ->
                        val isSelected = borrower.borrowerId == selectedBorrower.borrowerId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectBorrower(borrower) },
                            label = {
                                Text(
                                    text = borrower.borrowerName.split(" ").take(2).joinToString(" "),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) BankPrimary else Color.Gray
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BankPrimaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("chip_borrower_${borrower.borrowerId}")
                        )
                    }
                }
            }

            // Borrower Master Profile Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_borrower_history_header"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = BankPrimary,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val initials = selectedBorrower.borrowerName
                                            .split(" ")
                                            .mapNotNull { it.firstOrNull()?.toString() }
                                            .take(2)
                                            .joinToString("")
                                        Text(
                                            text = initials,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = selectedBorrower.borrowerName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Aadhaar: ${selectedBorrower.aadhaarMasked} • ${selectedBorrower.mobileNumber}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${selectedBorrower.village}, ${selectedBorrower.mandal.displayName} Cluster",
                                        fontSize = 11.sp,
                                        color = BankPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Call Button
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${selectedBorrower.mobileNumber}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.testTag("btn_history_call_borrower")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Borrower",
                                    tint = BankPrimary
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // JLG & Center details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("JLG Group", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedBorrower.groupName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text("Kendra Center", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedBorrower.centerName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Loan Cycle", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Cycle ${selectedBorrower.loanCycle} (₹${(selectedBorrower.sanctionedAmount / 1000).toInt()}k)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Summary Financial Metrics Bar
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = BankPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isTelugu) "మొత్తం చెల్లింపుల సంగ్రహం" else "Total Repayment Summary",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isTelugu) "మొత్తం జమ అయినది" else "Total Repaid",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = FieldBankingRepository.formatInr(totalHistoricalPaid),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFEF08A)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Installments",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$paidInstallmentCount Paid",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Current Status",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = if (selectedBorrower.pendingAmount > 0)
                                        "Due: ₹${selectedBorrower.pendingAmount.toInt()}"
                                    else "Cleared Today",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedBorrower.pendingAmount > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF86EFAC),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RBI MFI Guidelines (2022) Compliant: Fortnightly center installments recorded in Wanaparthy Room DB.",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
                }
            }

            // Repayment History Section Header & Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = BankPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTelugu) "గత చెల్లింపుల వివరాలు (${borrowerHistory.size})" else "Previous Payments (${borrowerHistory.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Sort Order Toggle
                    Text(
                        text = if (sortDescending) "Newest First" else "Oldest First",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BankPrimary,
                        modifier = Modifier
                            .clickable { sortDescending = !sortDescending }
                            .padding(4.dp)
                            .testTag("btn_toggle_history_sort")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Mode Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = filterMode == null,
                        onClick = { filterMode = null },
                        label = { Text("All Modes", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterMode == PaymentMode.CASH,
                        onClick = { filterMode = PaymentMode.CASH },
                        label = { Text("Cash", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterMode == PaymentMode.DYNAMIC_UPI_QR,
                        onClick = { filterMode = PaymentMode.DYNAMIC_UPI_QR },
                        label = { Text("UPI QR", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterMode == PaymentMode.MUTUAL_GUARANTEE,
                        onClick = { filterMode = PaymentMode.MUTUAL_GUARANTEE },
                        label = { Text("Group Guarantee", fontSize = 11.sp) }
                    )
                }
            }

            // Payment History List or Empty State
            if (borrowerHistory.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .testTag("card_empty_history"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = BankPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isTelugu) "గతంలో ఎటువంటి చెల్లింపులు నమోదు కాలేదు" else "No Previous Payments Recorded",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Installment #1 of ${FieldBankingRepository.formatInr(selectedBorrower.installmentDue)} is pending for this borrower in ${selectedBorrower.centerName}.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (selectedBorrower.pendingAmount > 0) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onCollectPayment(selectedBorrower) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_history_collect_now")
                                ) {
                                    Text("Collect Current Installment")
                                }
                            }
                        }
                    }
                }
            } else {
                items(borrowerHistory) { record ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_repayment_record_${record.receiptNumber}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Payment Date & Amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dateFormat.format(Date(record.timestamp)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Receipt: ${record.receiptNumber}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FieldBankingRepository.formatInr(record.amountPaid),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (record.amountPaid > 0) RupeeGreen else Color(0xFFDC2626)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (record.paymentMode) {
                                            PaymentMode.DYNAMIC_UPI_QR -> Color(0xFFEFF6FF)
                                            PaymentMode.CASH -> Color(0xFFDCFCE7)
                                            PaymentMode.MUTUAL_GUARANTEE -> Color(0xFFFEF2F2)
                                        }
                                    ) {
                                        Text(
                                            text = when (record.paymentMode) {
                                                PaymentMode.DYNAMIC_UPI_QR -> "UPI QR"
                                                PaymentMode.CASH -> "CASH"
                                                PaymentMode.MUTUAL_GUARANTEE -> "GROUP GUARANTEE"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = when (record.paymentMode) {
                                                PaymentMode.DYNAMIC_UPI_QR -> Color(0xFF1D4ED8)
                                                PaymentMode.CASH -> Color(0xFF166534)
                                                PaymentMode.MUTUAL_GUARANTEE -> Color(0xFF991B1B)
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Payment Details Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (record.attendance == AttendanceStatus.PRESENT) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (record.attendance == AttendanceStatus.PRESENT) "Present in Kendra" else "Absent (Proxy)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (record.upiTxnId != null) {
                                        Text(
                                            text = "Ref: ${record.upiTxnId}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = BankPrimary
                                        )
                                    }

                                    if (record.overdueReason != null) {
                                        Text(
                                            text = "Reason: ${record.overdueReason.label}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // GPS Field Audit location verification
                                    if (record.geoLatitude != null && record.geoLongitude != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "GPS Audit Location",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "GPS: ${record.geoLatitude}°N, ${record.geoLongitude}°E (${record.auditLocationAddress?.substringBefore(",") ?: "Wanaparthy Field"})",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E40AF)
                                            )
                                        }
                                    }
                                }

                                // Print Thermal Slip Action
                                OutlinedButton(
                                    onClick = { onPrintReceipt(record) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("btn_history_print_${record.receiptNumber}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Receipt Slip", fontSize = 10.sp)
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
}
