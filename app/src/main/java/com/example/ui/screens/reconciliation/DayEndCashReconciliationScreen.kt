package com.example.ui.screens.reconciliation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashReconciliation
import com.example.data.model.PaymentMode
import com.example.data.repository.FieldBankingRepository
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import com.example.ui.viewmodel.BankingUiState
import kotlinx.coroutines.launch

@Composable
fun DayEndCashReconciliationScreen(
    state: BankingUiState,
    onCalculateDenominations: (Int, Int, Int, Int, Int, Int, Double) -> Unit,
    onCompleteHandover: (String, CashReconciliation) -> Unit
) {
    val isTelugu = state.isTeluguLanguage
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // System expected cash from all CASH repayments recorded today
    val systemExpectedCash = state.repayments
        .filter { it.paymentMode == PaymentMode.CASH }
        .sumOf { it.amountPaid }

    var count500 by remember { mutableStateOf("0") }
    var count200 by remember { mutableStateOf("0") }
    var count100 by remember { mutableStateOf("0") }
    var count50 by remember { mutableStateOf("0") }
    var count20 by remember { mutableStateOf("0") }
    var count10 by remember { mutableStateOf("0") }

    // Pre-populate if existing recon
    LaunchedEffect(state.todayReconciliation) {
        state.todayReconciliation?.let { r ->
            if (r.physicalCashTotal > 0) {
                count500 = r.notes500.toString()
                count200 = r.notes200.toString()
                count100 = r.notes100.toString()
                count50 = r.notes50.toString()
                count20 = r.notes20.toString()
                count10 = r.notes10.toString()
            }
        }
    }

    val n500 = count500.toIntOrNull() ?: 0
    val n200 = count200.toIntOrNull() ?: 0
    val n100 = count100.toIntOrNull() ?: 0
    val n50 = count50.toIntOrNull() ?: 0
    val n20 = count20.toIntOrNull() ?: 0
    val n10 = count10.toIntOrNull() ?: 0

    val physicalTotal = (n500 * 500 + n200 * 200 + n100 * 100 + n50 * 50 + n20 * 20 + n10 * 10).toDouble()
    val variance = physicalTotal - systemExpectedCash
    val isZeroVariance = Math.abs(variance) < 0.01 && physicalTotal > 0

    var showTellerHandoverModal by remember { mutableStateOf(false) }
    var tellerOtpInput by remember { mutableStateOf("918234") }

    val recon = state.todayReconciliation

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTelugu) "రోజు ముగింపు క్యాష్ లెక్క & బ్యాలెన్సింగ్" else "Day-End Cash & Vault Reconciliation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTelugu) "వనపర్తి మెయిన్ బ్రాంచ్ (TG-WNP-01) వాల్ట్ డ్రాప్" else "Wanaparthy Main Branch (TG-WNP-01) Vault Handover",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = BankPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Summary Card (System Cash vs Physical Cash vs Variance)
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_reconciliation_summary"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isZeroVariance) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isTelugu) "సిస్టమ్ రసీదుల మొత్తం" else "System Cash Receipts",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FieldBankingRepository.formatInr(systemExpectedCash),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isTelugu) "భౌతిక నగదు మొత్తం" else "Physical Cash Count",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FieldBankingRepository.formatInr(physicalTotal),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BankPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isTelugu) "తేడా (Variance)" else "Cash Variance:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (variance == 0.0) "₹0.00 (Zero Variance)" else FieldBankingRepository.formatInr(variance),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (variance == 0.0 && physicalTotal > 0) RupeeGreen else if (physicalTotal == 0.0) MaterialTheme.colorScheme.onSurface else Color(0xFFDC2626)
                                )
                            }

                            if (isZeroVariance) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RupeeGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ZERO VARIANCE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = if (physicalTotal == 0.0) "COUNT PENDING" else "RECONCILE BAG",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Denomination inputs for RBI currency notes
            item {
                Text(
                    text = if (isTelugu) "కరెన్సీ నోట్ల సంఖ్య నమోదు (RBI Denominations)" else "Physical Currency Denomination Counter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // RBI Note Rows
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DenominationRow("₹500", count500, n500 * 500) { count500 = it }
                        DenominationRow("₹200", count200, n200 * 200) { count200 = it }
                        DenominationRow("₹100", count100, n100 * 100) { count100 = it }
                        DenominationRow("₹50", count50, n50 * 50) { count50 = it }
                        DenominationRow("₹20", count20, n20 * 20) { count20 = it }
                        DenominationRow("₹10", count10, n10 * 10) { count10 = it }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Auto-calculate to match button
                        Button(
                            onClick = {
                                // Auto calculate note breakdown to match expected cash if user wants quick reconcile
                                if (systemExpectedCash > 0) {
                                    var remaining = systemExpectedCash.toInt()
                                    val f500 = remaining / 500
                                    remaining %= 500
                                    val f200 = remaining / 200
                                    remaining %= 200
                                    val f100 = remaining / 100
                                    remaining %= 100
                                    val f50 = remaining / 50
                                    remaining %= 50
                                    val f20 = remaining / 20
                                    remaining %= 20
                                    val f10 = remaining / 10

                                    count500 = f500.toString()
                                    count200 = f200.toString()
                                    count100 = f100.toString()
                                    count50 = f50.toString()
                                    count20 = f20.toString()
                                    count10 = f10.toString()

                                    onCalculateDenominations(f500, f200, f100, f50, f20, f10, systemExpectedCash)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Auto-calculated note bundle to match collections!")
                                    }
                                } else {
                                    onCalculateDenominations(n500, n200, n100, n50, n20, n10, systemExpectedCash)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_auto_calculate_denominations"),
                            colors = ButtonDefaults.buttonColors(containerColor = BankPrimaryContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Auto-Bundle Exact Note Count (Match System)",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Teller Handover Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (recon?.isLockedAndVouchered == true) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isTelugu) "బ్రాంచ్ టెల్లర్ హాండోవర్ & వోచర్" else "Wanaparthy Branch Teller Handover",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Wanaparthy Hub • Teller: TEL-WNP-04",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = if (recon?.isLockedAndVouchered == true) Icons.Default.Lock else Icons.Default.Password,
                                contentDescription = null,
                                tint = if (recon?.isLockedAndVouchered == true) RupeeGreen else BankPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (recon?.isLockedAndVouchered == true) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "VOUCHER GENERATED IN CRAFT SILICON BR.NET",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Journal Voucher No: ${recon.voucherNumber}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14532D)
                                    )
                                    Text(
                                        text = "Teller OTP: ${recon.tellerOtp} • Handover Time: Locked",
                                        fontSize = 10.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (isTelugu)
                                    "ఫీల్డ్ క్యాష్ బ్యాగ్‌ను బ్రాంచ్ టెల్లర్‌కు అప్పగించి, లాక్ చేయడానికి టెల్లర్ ఆథరైజేషన్ OTP నమోదు చేయండి."
                                else
                                    "Hand over physical cash bag to Wanaparthy teller. Teller enters authorization OTP to lock day-end books and post Core Ledger Journal voucher.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (physicalTotal == 0.0) {
                                        scope.launch { snackbarHostState.showSnackbar("Please count physical denominations first!") }
                                    } else {
                                        showTellerHandoverModal = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_teller_handover"),
                                colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTelugu) "టెల్లర్ OTP తో వోచర్ జారీ చేయండి" else "Teller OTP Handover & Generate Voucher",
                                    fontWeight = FontWeight.Bold
                                )
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

    // Teller Authorization OTP Modal
    if (showTellerHandoverModal) {
        AlertDialog(
            onDismissRequest = { showTellerHandoverModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = BankPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Branch Teller Authorization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Wanaparthy Branch Vault Drop (TG-WNP-01)\nPhysical Cash Amount: ${FieldBankingRepository.formatInr(physicalTotal)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Teller must enter 6-digit authorization OTP to generate Core Ledger Journal voucher in Craft Silicon BR.Net:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tellerOtpInput,
                        onValueChange = { tellerOtpInput = it },
                        label = { Text("Teller Authorization OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_teller_otp")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentRecon = state.todayReconciliation ?: CashReconciliation(
                            reconciliationDate = "2026-10-02",
                            notes500 = n500,
                            notes200 = n200,
                            notes100 = n100,
                            notes50 = n50,
                            notes20 = n20,
                            notes10 = n10,
                            physicalCashTotal = physicalTotal,
                            systemCashExpected = systemExpectedCash,
                            varianceAmount = variance,
                            isZeroVariance = isZeroVariance
                        )
                        onCompleteHandover(tellerOtpInput, currentRecon)
                        showTellerHandoverModal = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Cash handover completed! Core Ledger Voucher created.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                    modifier = Modifier.testTag("btn_confirm_teller_otp")
                ) {
                    Text("Authorize & Generate Voucher")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTellerHandoverModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DenominationRow(
    label: String,
    countStr: String,
    totalVal: Int,
    onCountChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.width(60.dp)
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        OutlinedTextField(
            value = countStr,
            onValueChange = { onCountChange(it.filter { c -> c.isDigit() }) },
            label = { Text("Notes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(100.dp)
                .testTag("denom_input_$label")
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = FieldBankingRepository.formatInr(totalVal.toDouble()),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(90.dp)
        )
    }
}
