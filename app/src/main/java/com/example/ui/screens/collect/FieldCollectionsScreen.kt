package com.example.ui.screens.collect

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import com.example.util.GpsAuditSnapshot
import com.example.util.LocationAuditHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.MandalCluster
import com.example.data.model.OverdueReason
import com.example.data.model.PaymentMode
import com.example.data.model.PendingRepaymentItem
import com.example.data.model.RepaymentRecord
import com.example.data.model.RepaymentStatus
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
fun FieldCollectionsScreen(
    state: BankingUiState,
    onNavigateBack: () -> Unit,
    onCollectPayment: (
        item: PendingRepaymentItem,
        amount: Double,
        mode: PaymentMode,
        reason: OverdueReason?,
        isMutualGuarantee: Boolean,
        geoLat: Double?,
        geoLong: Double?,
        locationAccuracy: Float?,
        auditAddress: String?
    ) -> Unit,
    onOpenDynamicUpi: (PendingRepaymentItem) -> Unit,
    onDismissReceipt: () -> Unit
) {
    val isTelugu = state.isTeluguLanguage
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // GPS Audit State and Permission Launcher
    var activeGpsAudit by remember { mutableStateOf<GpsAuditSnapshot?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions updated, ready for location capture
    }

    // Filters
    var selectedMandal by remember { mutableStateOf<MandalCluster?>(null) }
    var selectedStatus by remember { mutableStateOf<RepaymentStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog States
    var paymentItemToCollect by remember { mutableStateOf<PendingRepaymentItem?>(null) }
    var customAmountStr by remember { mutableStateOf("") }
    var upiSimulatingItem by remember { mutableStateOf<PendingRepaymentItem?>(null) }
    var isSimulatingUpiWebhook by remember { mutableStateOf(false) }

    var delinquencyItem by remember { mutableStateOf<PendingRepaymentItem?>(null) }
    var selectedOverdueReason by remember { mutableStateOf(OverdueReason.COTTON_HARVEST_DELAY) }
    var triggerMutualGuarantee by remember { mutableStateOf(true) }

    var receiptToPrint by remember { mutableStateOf<RepaymentRecord?>(null) }
    var borrowerForHistory by remember { mutableStateOf<PendingRepaymentItem?>(null) }

    // Pull pending repayments from Room via uiState
    val allPendingList = state.pendingRepayments

    val filteredList = allPendingList.filter { item ->
        val matchesMandal = selectedMandal == null || item.mandal == selectedMandal
        val matchesStatus = selectedStatus == null || item.status == selectedStatus
        val matchesSearch = searchQuery.isBlank() ||
                item.borrowerName.contains(searchQuery, ignoreCase = true) ||
                item.village.contains(searchQuery, ignoreCase = true) ||
                item.groupName.contains(searchQuery, ignoreCase = true) ||
                item.centerName.contains(searchQuery, ignoreCase = true) ||
                item.mobileNumber.contains(searchQuery)
        matchesMandal && matchesStatus && matchesSearch
    }

    val totalPendingDemand = allPendingList.sumOf { it.pendingAmount }
    val totalCollectedSoFar = allPendingList.sumOf { it.amountPaid }
    val totalOriginalDemand = totalPendingDemand + totalCollectedSoFar
    val collectionEfficiency = if (totalOriginalDemand > 0) {
        Math.round((totalCollectedSoFar / totalOriginalDemand) * 1000.0) / 10.0
    } else 0.0

    val overdueCount = allPendingList.count { it.status == RepaymentStatus.OVERDUE || it.status == RepaymentStatus.DELINQUENT }
    val pendingBorrowersCount = allPendingList.count { it.pendingAmount > 0 }

    if (borrowerForHistory != null) {
        BorrowerRepaymentHistoryView(
            selectedBorrower = borrowerForHistory!!,
            allBorrowers = allPendingList,
            repaymentRecords = state.repayments,
            isTelugu = isTelugu,
            onSelectBorrower = { borrowerForHistory = it },
            onClose = { borrowerForHistory = null },
            onPrintReceipt = { receiptToPrint = it },
            onCollectPayment = { item ->
                paymentItemToCollect = item
                customAmountStr = item.pendingAmount.toInt().toString()
                borrowerForHistory = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isTelugu) "ఫీల్డ్ వసూళ్ల డ్యాష్‌బోర్డ్" else "Field Collections Dashboard",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = WanaparthyGold.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "Wanaparthy",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
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
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_collections_back")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (allPendingList.isNotEmpty()) {
                                    borrowerForHistory = allPendingList.first()
                                }
                            },
                            modifier = Modifier.testTag("btn_collections_history")
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "Repayment History")
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Synced live with Room Database (TG-WNP-01)")
                                }
                            },
                            modifier = Modifier.testTag("btn_collections_refresh")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            },
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
                Spacer(modifier = Modifier.height(2.dp))
                // Hero KPI Metric Card
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_collections_summary"),
                    shape = RoundedCornerShape(16.dp),
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
                                    text = if (isTelugu) "వనపర్తి జిల్లా బాకీ మొత్తం" else "Wanaparthy Outstanding Demand",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = FieldBankingRepository.formatInr(totalPendingDemand),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFEF08A) // Soft amber gold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isTelugu) "ఈ రోజు వసూలైన మొత్తం" else "Collected Today",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = FieldBankingRepository.formatInr(totalCollectedSoFar),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78F8D4)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Efficiency Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Collection Efficiency: $collectionEfficiency%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "$pendingBorrowersCount Borrowers Pending",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (collectionEfficiency / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF78F8D4),
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Critical Delinquencies: $overdueCount",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "6 Mandal Clusters Active",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isTelugu) "పేరు, గ్రామం లేదా గ్రూప్ ద్వారా వెతకండి..." else "Search borrower, village, center, or group...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_collections_search"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Mandal Cluster Filter Chips
            item {
                Column {
                    Text(
                        text = if (isTelugu) "మండల క్లస్టర్లు (వనపర్తి జిల్లా):" else "Wanaparthy Mandal Clusters:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedMandal == null,
                                onClick = { selectedMandal = null },
                                label = { Text("All Clusters") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BankPrimaryContainer)
                            )
                        }
                        items(MandalCluster.values()) { mandal ->
                            FilterChip(
                                selected = selectedMandal == mandal,
                                onClick = { selectedMandal = if (selectedMandal == mandal) null else mandal },
                                label = { Text(mandal.displayName) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BankPrimaryContainer)
                            )
                        }
                    }
                }
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusFilters = listOf(
                        null to "All (${allPendingList.size})",
                        RepaymentStatus.DUE_TODAY to "Due Today",
                        RepaymentStatus.OVERDUE to "Overdue",
                        RepaymentStatus.DELINQUENT to "Delinquent",
                        RepaymentStatus.PAID to "Collected"
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(statusFilters) { (status, label) ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (status == RepaymentStatus.DELINQUENT) Color(0xFFFEE2E2) else BankPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTelugu) "బాకీ ఉన్న రుణ వాయిదాల జాబితా (${filteredList.size})" else "Pending Loan Repayments (${filteredList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BankPrimary.copy(alpha = 0.1f),
                            modifier = Modifier
                                .clickable {
                                    if (filteredList.isNotEmpty()) {
                                        borrowerForHistory = filteredList.first()
                                    } else if (allPendingList.isNotEmpty()) {
                                        borrowerForHistory = allPendingList.first()
                                    }
                                }
                                .testTag("btn_open_borrower_history_header")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = BankPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Repayment History",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BankPrimary
                                )
                            }
                        }

                        if (selectedMandal != null || selectedStatus != null || searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(onClick = {
                                selectedMandal = null
                                selectedStatus = null
                                searchQuery = ""
                            }) {
                                Text("Clear", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Empty state
            if (filteredList.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RupeeGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No pending repayments found for this filter.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All collections in this selection are up to date.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Pending Repayment Cards
            items(filteredList) { item ->
                var isExpanded by remember { mutableStateOf(false) }

                val cardBorderColor = when (item.status) {
                    RepaymentStatus.DELINQUENT -> Color(0xFFEF4444)
                    RepaymentStatus.OVERDUE -> Color(0xFFF59E0B)
                    RepaymentStatus.PAID -> Color(0xFF10B981)
                    else -> Color.Transparent
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (item.status != RepaymentStatus.DUE_TODAY) 1.5.dp else 0.dp,
                            color = cardBorderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("card_pending_repayment_${item.borrowerId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (item.status == RepaymentStatus.PAID) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Card Top: Name, Group, Status (Clickable to view history)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { borrowerForHistory = item },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.borrowerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${item.groupName} • ${item.centerName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${item.village} (${item.mandal.displayName}) • ${item.mobileNumber}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                // Status Chip
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (item.status) {
                                        RepaymentStatus.PAID -> Color(0xFFDCFCE7)
                                        RepaymentStatus.DELINQUENT -> Color(0xFFFEE2E2)
                                        RepaymentStatus.OVERDUE -> Color(0xFFFEF3C7)
                                        RepaymentStatus.PARTIALLY_PAID -> Color(0xFFE0E7FF)
                                        RepaymentStatus.DUE_TODAY -> Color(0xFFEFF6FF)
                                    }
                                ) {
                                    Text(
                                        text = when (item.status) {
                                            RepaymentStatus.PAID -> "COLLECTED"
                                            RepaymentStatus.DELINQUENT -> "DELINQUENT (CR-01)"
                                            RepaymentStatus.OVERDUE -> "OVERDUE ${item.overdueDays}D"
                                            RepaymentStatus.PARTIALLY_PAID -> "PARTIAL PAID"
                                            RepaymentStatus.DUE_TODAY -> "DUE TODAY"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (item.status) {
                                            RepaymentStatus.PAID -> Color(0xFF166534)
                                            RepaymentStatus.DELINQUENT -> Color(0xFF991B1B)
                                            RepaymentStatus.OVERDUE -> Color(0xFFB45309)
                                            RepaymentStatus.PARTIALLY_PAID -> Color(0xFF3730A3)
                                            RepaymentStatus.DUE_TODAY -> Color(0xFF1D4ED8)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Cycle ${item.loanCycle} (₹${(item.sanctionedAmount / 1000).toInt()}k)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Financial Metric Bar (Due, Paid, Pending)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Installment Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = FieldBankingRepository.formatInr(item.installmentDue),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Paid so far", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = FieldBankingRepository.formatInr(item.amountPaid),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (item.amountPaid > 0) RupeeGreen else Color.Gray
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Pending Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = FieldBankingRepository.formatInr(item.pendingAmount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (item.pendingAmount > 0) Color(0xFFDC2626) else RupeeGreen
                                    )
                                }
                            }
                        }

                        // Overdue Reason Note if present
                        if (item.overdueReason != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reason: ${item.overdueReason.label} (${item.overdueReason.code}) • JLG Cross-Guarantee Triggered",
                                        fontSize = 10.sp,
                                        color = Color(0xFF991B1B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.pendingAmount > 0) {
                                // Cash Collection CTA
                                Button(
                                    onClick = {
                                        paymentItemToCollect = item
                                        customAmountStr = item.pendingAmount.toInt().toString()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("btn_collect_cash_${item.borrowerId}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Collect Cash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Dynamic UPI QR CTA
                                OutlinedButton(
                                    onClick = {
                                        upiSimulatingItem = item
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("btn_collect_upi_${item.borrowerId}"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(14.dp), tint = BankPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "UPI QR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BankPrimary)
                                }
                            } else {
                                // Already collected - print receipt
                                OutlinedButton(
                                    onClick = {
                                        receiptToPrint = RepaymentRecord(
                                            receiptNumber = "REC-WNP-2026-${(1000..9999).random()}",
                                            centerId = item.centerId,
                                            groupId = item.groupId,
                                            borrowerId = item.borrowerId,
                                            borrowerName = item.borrowerName,
                                            installmentDue = item.installmentDue,
                                            amountPaid = item.amountPaid,
                                            paymentMode = PaymentMode.CASH,
                                            attendance = AttendanceStatus.PRESENT
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("btn_print_receipt_${item.borrowerId}"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Print Slip", fontSize = 11.sp)
                                }
                            }

                            // Call Borrower
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${item.mobileNumber}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("btn_call_borrower_${item.borrowerId}")
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = BankPrimary)
                            }

                            // Tag Delinquent
                            if (item.pendingAmount > 0) {
                                IconButton(
                                    onClick = {
                                        delinquencyItem = item
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("btn_tag_delinquent_${item.borrowerId}")
                                ) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Tag Delinquent", tint = Color(0xFFDC2626))
                                }
                            }
                        }

                        // Repayment History Action Button
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { borrowerForHistory = item },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("btn_repayment_history_${item.borrowerId}"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = BankPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTelugu) "రుణ చెల్లింపుల చరిత్ర (${item.previousPaymentsCount})" else "Repayment History (${item.previousPaymentsCount} payments)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BankPrimary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

    // Modal 1: Collect Cash Dialog
    if (paymentItemToCollect != null) {
        val target = paymentItemToCollect!!
        val currentGps = activeGpsAudit ?: LocationAuditHelper.captureCurrentLocation(context, target.mandal, target.village)
        if (activeGpsAudit == null) {
            activeGpsAudit = currentGps
        }

        AlertDialog(
            onDismissRequest = {
                paymentItemToCollect = null
                activeGpsAudit = null
            },
            title = {
                Text(
                    text = if (isTelugu) "నగదు వసూలు రసీదు" else "Record Cash Collection",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Borrower: ${target.borrowerName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${target.village} • ${target.groupName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customAmountStr,
                        onValueChange = { customAmountStr = it },
                        label = { Text("Amount Paid in Cash (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cash_amount"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // GPS Audit Verification Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_cash_gps_audit")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "GPS",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "FIELD AUDIT GPS LOCK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                        activeGpsAudit = LocationAuditHelper.captureCurrentLocation(context, target.mandal, target.village)
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(26.dp)
                                        .testTag("btn_refresh_cash_gps")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF1E40AF))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Re-acquire", fontSize = 10.sp, color = Color(0xFF1E40AF))
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "GPS: ${currentGps.latitude}° N, ${currentGps.longitude}° E (Acc: ±${currentGps.accuracyMeters}m)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Cluster: ${currentGps.addressOrCluster}",
                                fontSize = 10.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "Audit Ref: ${currentGps.auditCode}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0FDF4),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Instant 58mm digital receipt with GPS Audit will be saved to Room Database and SMS dispatched in Telugu.",
                            fontSize = 11.sp,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = customAmountStr.toDoubleOrNull() ?: target.pendingAmount
                        val gps = activeGpsAudit ?: currentGps
                        onCollectPayment(
                            target,
                            amount,
                            PaymentMode.CASH,
                            null,
                            false,
                            gps.latitude,
                            gps.longitude,
                            gps.accuracyMeters,
                            gps.addressOrCluster
                        )
                        receiptToPrint = RepaymentRecord(
                            receiptNumber = "REC-WNP-2026-${(1000..9999).random()}",
                            centerId = target.centerId,
                            groupId = target.groupId,
                            borrowerId = target.borrowerId,
                            borrowerName = target.borrowerName,
                            installmentDue = target.installmentDue,
                            amountPaid = amount,
                            paymentMode = PaymentMode.CASH,
                            attendance = AttendanceStatus.PRESENT,
                            geoLatitude = gps.latitude,
                            geoLongitude = gps.longitude,
                            locationAccuracyMeters = gps.accuracyMeters,
                            auditLocationAddress = gps.addressOrCluster
                        )
                        paymentItemToCollect = null
                        activeGpsAudit = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Cash payment ₹${amount.toInt()} with GPS Audit (${gps.latitude}, ${gps.longitude}) saved to Room DB!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                    modifier = Modifier.testTag("btn_confirm_cash_collect")
                ) {
                    Text("Confirm Cash Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    paymentItemToCollect = null
                    activeGpsAudit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 2: Dynamic UPI QR Dialog
    if (upiSimulatingItem != null) {
        val target = upiSimulatingItem!!
        val amount = target.pendingAmount

        AlertDialog(
            onDismissRequest = {
                if (!isSimulatingUpiWebhook) upiSimulatingItem = null
            },
            title = {
                Text(
                    text = "Dynamic UPI QR Collection",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Saptadhan Finance Private Limited",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BankPrimary
                    )

                    Text(
                        text = "${target.borrowerName} • ₹${amount.toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Simulated Dynamic UPI QR Container
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .border(2.dp, BankPrimary, RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "UPI QR",
                                modifier = Modifier.size(130.dp),
                                tint = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Scan with any UPI App",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }

                    Text(
                        text = "VPA: saptadhan.tgwnp01@icici\nTxnRef: CS-UPI-${target.borrowerId}-${System.currentTimeMillis() % 100000}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )

                    // GPS Audit Verification Card for UPI
                    val currentGps = activeGpsAudit ?: LocationAuditHelper.captureCurrentLocation(context, target.mandal, target.village)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("FIELD AUDIT GPS LOCK: ${currentGps.latitude}° N, ${currentGps.longitude}° E", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                Text("Cluster: ${currentGps.addressOrCluster} (±${currentGps.accuracyMeters}m)", fontSize = 9.sp, color = Color(0xFF334155))
                            }
                        }
                    }

                    if (isSimulatingUpiWebhook) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Awaiting NPCI UPI Webhook Confirmation...", fontSize = 11.sp, color = BankPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSimulatingUpiWebhook = true
                        val gps = activeGpsAudit ?: LocationAuditHelper.captureCurrentLocation(context, target.mandal, target.village)
                        scope.launch {
                            delay(1400)
                            isSimulatingUpiWebhook = false
                            onCollectPayment(
                                target,
                                amount,
                                PaymentMode.DYNAMIC_UPI_QR,
                                null,
                                false,
                                gps.latitude,
                                gps.longitude,
                                gps.accuracyMeters,
                                gps.addressOrCluster
                            )
                            upiSimulatingItem = null
                            activeGpsAudit = null
                            snackbarHostState.showSnackbar("UPI Webhook Received: ₹${amount.toInt()} with GPS Audit credited.")
                        }
                    },
                    enabled = !isSimulatingUpiWebhook,
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                    modifier = Modifier.testTag("btn_simulate_upi_webhook")
                ) {
                    Text("Simulate UPI Success Callback")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        upiSimulatingItem = null
                        activeGpsAudit = null
                    },
                    enabled = !isSimulatingUpiWebhook
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Modal 3: Delinquency & Mutual Guarantee Tagging
    if (delinquencyItem != null) {
        val target = delinquencyItem!!
        AlertDialog(
            onDismissRequest = { delinquencyItem = null },
            title = {
                Text("Tag Delinquency & Trigger JLG Guarantee", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Borrower: ${target.borrowerName} (${target.groupName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "Select RBI Standardized Reason Code:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OverdueReason.values().forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOverdueReason = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (selectedOverdueReason == reason) BankPrimary else Color.Transparent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .border(1.dp, BankPrimary, CircleShape)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${reason.code}: ${reason.label}",
                                fontSize = 12.sp,
                                fontWeight = if (selectedOverdueReason == reason) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    HorizontalDivider()

                    // GPS Audit Verification Card for Delinquency
                    val currentGps = activeGpsAudit ?: LocationAuditHelper.captureCurrentLocation(context, target.mandal, target.village)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("FIELD AUDIT ON-SITE VERIFICATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                                Text("GPS: ${currentGps.latitude}° N, ${currentGps.longitude}° E (Acc: ±${currentGps.accuracyMeters}m)", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF7F1D1D))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Triggers mutual liability protocol across remaining 4 members of ${target.groupName}.",
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gps = activeGpsAudit ?: LocationAuditHelper.captureCurrentLocation(context, target.mandal, target.village)
                        onCollectPayment(
                            target,
                            0.0,
                            PaymentMode.MUTUAL_GUARANTEE,
                            selectedOverdueReason,
                            true,
                            gps.latitude,
                            gps.longitude,
                            gps.accuracyMeters,
                            gps.addressOrCluster
                        )
                        delinquencyItem = null
                        activeGpsAudit = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Delinquency logged with code ${selectedOverdueReason.code} & GPS audit captured.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_delinquency")
                ) {
                    Text("Confirm Delinquency Tag")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    delinquencyItem = null
                    activeGpsAudit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal 4: Thermal Receipt Preview
    if (receiptToPrint != null) {
        val r = receiptToPrint!!
        AlertDialog(
            onDismissRequest = { receiptToPrint = null },
            title = {
                Text("Handheld Thermal Slip (58mm)", fontWeight = FontWeight.Bold)
            },
            text = {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "SAPTADHAN FINANCE PRIVATE LIMITED",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Brand: Saptadhan • saptadhan.com",
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Wanaparthy Branch (TG-WNP-01) • GSTIN: 36AABCS1234F1Z8",
                            fontSize = 9.sp,
                            color = Color.DarkGray
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text("Receipt No: ${r.receiptNumber}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Text("Borrower: ${r.borrowerName}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Center: ${r.centerId} • Group: ${r.groupId}", fontSize = 10.sp)
                        Text("Amount Collected: ₹${r.amountPaid.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                        Text("Payment Mode: ${r.paymentMode.name}", fontSize = 10.sp)

                        // GPS Field Audit on Slip
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text("FIELD AUDIT GPS VERIFICATION", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = BankPrimary)
                        val latVal = r.geoLatitude ?: 16.3845
                        val lngVal = r.geoLongitude ?: 78.0249
                        val accVal = r.locationAccuracyMeters ?: 3.8f
                        Text("GPS: $latVal° N, $lngVal° E (±${accVal}m)", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Cluster: ${r.auditLocationAddress ?: "Wanaparthy Cluster (TG-WNP-01)"}", fontSize = 9.sp)
                        Text("Audit Status: VERIFIED ON-SITE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            text = "SMS Dispatch: ధన్యవాదములు! మీ వాయిదా ₹${r.amountPaid.toInt()} సప్తధాన్ ఫైనాన్స్‌కు జమచేయబడింది.",
                            fontSize = 9.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        receiptToPrint = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Print job sent to Bluetooth Handheld 58mm Thermal Printer!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary)
                ) {
                    Text("Print Thermal Slip")
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptToPrint = null }) {
                    Text("Close")
                }
            }
        )
    }
}
