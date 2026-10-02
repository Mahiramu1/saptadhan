package com.example.ui.screens.disrub

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Borrower
import com.example.data.model.JlgGroup
import com.example.data.model.KeyFactStatement
import com.example.data.repository.FieldBankingRepository
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import com.example.ui.viewmodel.BankingUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgtGrtMilestonesScreen(
    state: BankingUiState,
    groupId: String?,
    onNavigateBack: () -> Unit,
    onUpdateCgt: (String, Int) -> Unit,
    onApproveGrt: (String, String) -> Unit,
    onOpenKfs: (Borrower, JlgGroup) -> Unit
) {
    val isTelugu = state.isTeluguLanguage
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val rawGroup = state.groups.find { it.groupId == groupId } ?: state.groups.firstOrNull()
    if (rawGroup == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No group found.")
        }
        return
    }
    val group = rawGroup
    val groupBorrowers = state.borrowers.filter { it.groupId == group.groupId }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: CGT/GRT, 1: Key Fact Statement (KFS)
    var kfsLanguageTelugu by remember { mutableStateOf(isTelugu) }

    var showSanctionLetterModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = group?.groupName ?: "CGT & GRT Milestones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${group?.groupId} • Cycle ${group?.loanCycle} • Wanaparthy Hub",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_cgt_back")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("1. CGT & GRT Approval") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("2. KFS & Sanction") }
                )
            }

            if (selectedTab == 0) {
                // Tab 0: CGT & GRT Workflow
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isTelugu) "నిరంతర గ్రూప్ శిక్షణ (CGT 1, 2, 3)" else "Continuous Group Training (CGT Milestones)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTelugu)
                                "5గురు సభ్యుల హాజరు మరియు ఆర్థిక అక్షరాస్యత పరీక్షను నమోదు చేయండి."
                            else
                                "Ensure 100% attendance and financial literacy comprehension across 5 members.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // CGT 1
                    item {
                        CgtCard(
                            cgtNumber = 1,
                            title = "CGT 1: Group Discipline & Mutual Guarantee",
                            teluguTitle = "CGT 1: గ్రూప్ క్రమశిక్షణ & ఉమ్మడి బాధ్యత నియమాలు",
                            desc = "Attendance, group leadership, joint liability principles, and weekly meeting rules.",
                            isCompleted = group.cgt1Completed,
                            onComplete = {
                                onUpdateCgt(group.groupId, 1)
                                scope.launch { snackbarHostState.showSnackbar("CGT 1 completed successfully!") }
                            }
                        )
                    }

                    // CGT 2
                    item {
                        CgtCard(
                            cgtNumber = 2,
                            title = "CGT 2: Financial Literacy & Cash Flow",
                            teluguTitle = "CGT 2: ఆర్థిక అక్షరాస్యత & నగదు నిర్వహణ",
                            desc = "Household budget assessment, reducing balance calculation, avoiding over-indebtedness.",
                            isCompleted = group.cgt2Completed,
                            enabled = group.cgt1Completed,
                            onComplete = {
                                onUpdateCgt(group.groupId, 2)
                                scope.launch { snackbarHostState.showSnackbar("CGT 2 completed successfully!") }
                            }
                        )
                    }

                    // CGT 3
                    item {
                        CgtCard(
                            cgtNumber = 3,
                            title = "CGT 3: Loan Utilization & Timely Repayment",
                            teluguTitle = "CGT 3: రుణ వినియోగం & కేంద్ర సమావేశంలో చెల్లింపు",
                            desc = "Income generating activity verification (dairy, cotton, retail) and digital receipting.",
                            isCompleted = group.cgt3Completed,
                            enabled = group.cgt2Completed,
                            onComplete = {
                                onUpdateCgt(group.groupId, 3)
                                scope.launch { snackbarHostState.showSnackbar("CGT 3 completed successfully!") }
                            }
                        )
                    }

                    // Branch Manager GRT Section
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTelugu) "బ్రాంచ్ మేనేజర్ GRT ఆమోదం" else "Branch Manager (BM) GRT Sign-off",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_grt_approval"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (group.grtApprovedByBm) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
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
                                            text = "Group Recognition Test (GRT)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Sign-off Authority: S. Rajesh Kumar (BM)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (group.grtApprovedByBm) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = if (group.grtApprovedByBm) "GRT APPROVED" else "PENDING BM TEST",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (group.grtApprovedByBm) Color(0xFF166534) else Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (isTelugu)
                                        "బ్రాంచ్ మేనేజర్ గ్రూప్ సభ్యులను ప్రశ్నించి, వారి ఆదాయం మరియు ఉమ్మడి బాధ్యతను ధృవీకరించిన తర్వాత డిజిటల్ సంతకం చేస్తారు."
                                    else
                                        "BM conducts physical/digital oral test assessing borrower rights, grievance redressal, and zero prepayment fee awareness.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                if (group.grtApprovedByBm) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showSanctionLetterModal = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("btn_view_sanction_letter"),
                                            colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View Sanction Letter", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { selectedTab = 1 },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("btn_view_kfs_tab"),
                                            colors = ButtonDefaults.buttonColors(containerColor = RupeeGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("KFS Disclosure", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (!group.cgt3Completed) {
                                                scope.launch { snackbarHostState.showSnackbar("Please complete CGT 1, 2, and 3 before BM GRT approval!") }
                                            } else {
                                                onApproveGrt(group.groupId, "S. Rajesh Kumar (BM-WNP-01)")
                                                scope.launch { snackbarHostState.showSnackbar("BM GRT Approved! Sanction letters generated.") }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_approve_grt"),
                                        colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isTelugu) "BM డిజిటల్ సైన్-ఆఫ్ & మంజూరు చేయండి" else "BM Digital Sign-off & Sanction Loan",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            } else {
                // Tab 1: Key Fact Statement (KFS) in English and Telugu
                val leader = groupBorrowers.firstOrNull()
                val kfs = FieldBankingRepository.calculateKfs(
                    borrowerName = leader?.fullName ?: "Borrower Leader",
                    groupName = group.groupName,
                    loanAmount = group.sanctionedAmountPerMember,
                    loanCycle = group.loanCycle
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (kfsLanguageTelugu) "ముఖ్య సమాచార పత్రం (KFS)" else "Key Fact Statement (KFS)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = { kfsLanguageTelugu = !kfsLanguageTelugu },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_kfs_lang_toggle")
                            ) {
                                Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (kfsLanguageTelugu) "Switch to English" else "తెలుగులో చూడండి", fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_kfs_content"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (kfsLanguageTelugu)
                                        "ఆర్.బి.ఐ. నిబంధనల ప్రకారం రుణ నిబంధనల వెల్లడి పత్రం"
                                    else
                                        "RBI Regulatory Framework Mandatory Pricing Disclosure",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                HorizontalDivider()

                                KfsRow(
                                    label = if (kfsLanguageTelugu) "రుణగ్రహీత పేరు" else "Borrower Name",
                                    value = kfs.borrowerName
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "సంఘం పేరు (JLG)" else "JLG Group",
                                    value = kfs.groupName
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "మంజూరైన రుణ మొత్తం" else "Sanctioned Loan Amount",
                                    value = FieldBankingRepository.formatInr(kfs.loanAmount),
                                    isBold = true
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "రుణ కాలపరిమితి" else "Tenure / Installments",
                                    value = "${kfs.tenureMonths} Months (${kfs.numberOfInstallments} ${kfs.repaymentFrequency})"
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "తగ్గుతున్న నిల్వ వడ్డీ రేటు" else "Reducing Balance Interest Rate",
                                    value = "${kfs.reducingInterestRatePa}% p.a."
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "ప్రాసెసింగ్ ఫీజు (1.5% + GST)" else "Processing Fee (1.5% + 18% GST)",
                                    value = "₹${(kfs.processingFeeAmount + kfs.gstOnProcessingFee).toInt()}"
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "క్రెడిట్ షీల్డ్ బీమా ప్రీమియం" else "Credit Shield Insurance Premium",
                                    value = "₹${kfs.insurancePremium.toInt()}"
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "ఖాతాలో జమ అయ్యే నికర మొత్తం" else "Net Disbursed to Bank Account",
                                    value = FieldBankingRepository.formatInr(kfs.netDisbursedAmount),
                                    isBold = true,
                                    valueColor = RupeeGreen
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "పాక్షిక వాయిదా మొత్తం (వాయిదాకు)" else "Fortnightly Installment Due",
                                    value = FieldBankingRepository.formatInr(kfs.installmentAmount),
                                    isBold = true
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "మొత్తం తిరిగి చెల్లించాల్సిన మొత్తం" else "Total Repayable Amount",
                                    value = FieldBankingRepository.formatInr(kfs.totalRepaymentAmount)
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "వార్షిక శాతం రేటు (APR)" else "Annual Percentage Rate (APR)",
                                    value = "${kfs.aprPercentage}%",
                                    isBold = true
                                )
                                KfsRow(
                                    label = if (kfsLanguageTelugu) "ముందస్తు చెల్లింపు జరిమానా" else "Prepayment Penalty",
                                    value = kfs.prepaymentPenalty,
                                    valueColor = Color(0xFF166534)
                                )

                                HorizontalDivider()

                                Text(
                                    text = if (kfsLanguageTelugu)
                                        "గమనిక: ఈ రుణం కోసం ఎటువంటి తనఖా అవసరం లేదు. RBI 2022 నిబంధనల ప్రకారం ఎలాంటి దాగివున్న ఛార్జీలు లేవు."
                                    else
                                        "Note: This is a collateral-free microfinance loan complying with RBI Master Directions 2022. No hidden charges.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("KFS dispatched via SMS to all 5 group borrowers in Telugu.")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_send_kfs_sms"),
                            colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (kfsLanguageTelugu) "సభ్యుల మొబైల్‌కు KFS SMS పంపండి" else "Dispatch KFS via Regional SMS to Borrowers")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Sanction Letter Dialog
    if (showSanctionLetterModal) {
        AlertDialog(
            onDismissRequest = { showSanctionLetterModal = false },
            title = {
                Text(
                    text = "Craft Silicon BR.Net Sanction Letter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Wanaparthy Branch Code: TG-WNP-01\nSanction Ref: SAN-WNP-${group.groupId}\nDate: 2026-10-02",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    HorizontalDivider()
                    Text(
                        text = "To:\n${group.groupName}\nLeader: ${groupBorrowers.firstOrNull()?.fullName}\n\nDear Borrower,\nWe are pleased to inform that your JLG loan of ₹${(group.sanctionedAmountPerMember * 5).toInt()} (₹${group.sanctionedAmountPerMember.toInt()} per member) is SANCTIONED under Cycle ${group.loanCycle}.\n\nDisbursement initiated directly to Aadhaar-linked Bank Accounts via NACH/NEFT.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    HorizontalDivider()
                    Text(
                        text = "Sign-off: S. Rajesh Kumar, Branch Manager",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSanctionLetterModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CgtCard(
    cgtNumber: Int,
    title: String,
    teluguTitle: String,
    desc: String,
    isCompleted: Boolean,
    enabled: Boolean = true,
    onComplete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isCompleted) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isCompleted) Color(0xFFD1FAE5) else Color(0xFFF1F5F9),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cgtNumber.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isCompleted) Color(0xFF065F46) else Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = teluguTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = RupeeGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            if (!isCompleted) {
                Button(
                    onClick = onComplete,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_complete_cgt_$cgtNumber"),
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (enabled) "Record CGT $cgtNumber Attendance & Assessment" else "Complete Prior CGT First",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun KfsRow(label: String, value: String, isBold: Boolean = false, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
