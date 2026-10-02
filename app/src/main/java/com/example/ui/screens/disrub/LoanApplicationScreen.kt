package com.example.ui.screens.disrub

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiLoanAssessment
import com.example.data.model.LoanApplication
import com.example.data.model.LoanCycle
import com.example.data.model.MandalCluster
import com.example.data.repository.FieldBankingRepository
import com.example.ui.components.CreditRiskScoreCard
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import com.example.ui.viewmodel.BankingUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationScreen(
    state: BankingUiState,
    onNavigateBack: () -> Unit,
    onSubmitApplication: (LoanApplication) -> Unit,
    onEvaluateAi: (LoanApplication, (AiLoanAssessment) -> Unit) -> Unit,
    onEnhancePurposeAi: (String, MandalCluster, String, Double, (String) -> Unit) -> Unit
) {
    val isTelugu = state.isTeluguLanguage
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Form, 1: Room DB Saved Applications

    // Form Fields
    var applicantName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var aadhaarNumber by remember { mutableStateOf("") }
    var selectedMandal by remember { mutableStateOf(MandalCluster.KOTHAKOTA) }
    var villageName by remember { mutableStateOf("Natavalli") }
    var annualIncomeStr by remember { mutableStateOf("180000") }
    var existingDebtStr by remember { mutableStateOf("2000") }
    var requestedAmountStr by remember { mutableStateOf("35000") }
    var selectedCycle by remember { mutableIntStateOf(1) }
    var loanPurpose by remember { mutableStateOf("Purchase of hybrid cotton seeds, bio-fertilizers, and farm input materials for upcoming kharif season.") }

    var mandalDropdownExpanded by remember { mutableStateOf(false) }
    var cycleDropdownExpanded by remember { mutableStateOf(false) }

    // AI state
    var isAnalyzingWithAi by remember { mutableStateOf(false) }
    var isEnhancingPurpose by remember { mutableStateOf(false) }
    var aiAssessmentResult by remember { mutableStateOf<AiLoanAssessment?>(null) }

    // Search query for saved list
    var searchQuery by remember { mutableStateOf("") }

    val annualIncome = annualIncomeStr.toDoubleOrNull() ?: 0.0
    val monthlyIncome = annualIncome / 12.0
    val existingDebt = existingDebtStr.toDoubleOrNull() ?: 0.0
    val requestedAmount = requestedAmountStr.toDoubleOrNull() ?: 35000.0

    // Approximate fortnightly EMI (24 fortnights @ 22% reducing)
    val r = 0.22 / 24.0
    val n = 24.0
    val estimatedInstallment = if (requestedAmount > 0) {
        (requestedAmount * r * Math.pow(1.0 + r, n)) / (Math.pow(1.0 + r, n) - 1.0)
    } else 0.0
    val foirPercentage = if (monthlyIncome > 0) {
        Math.round(((existingDebt + (estimatedInstallment * 2.0)) / monthlyIncome) * 1000.0) / 10.0
    } else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isTelugu) "రుణ దరఖాస్తు & AI అండర్‌రైటర్" else "Loan Application & AI Underwriter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Craft Silicon BR.Net • Wanaparthy Hub (TG-WNP-01)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_loan_app_back")
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
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(if (isTelugu) "1. కొత్త దరఖాస్తు" else "New Application Form") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            if (isTelugu) "2. సేవ్ చేయబడినవి (${state.loanApplications.size})"
                            else "Saved in Room DB (${state.loanApplications.size})"
                        )
                    }
                )
            }

            if (selectedTabIndex == 0) {
                // Application Form Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        // AI Underwriter Banner
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF9333EA), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Gemini AI",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isTelugu) "జెమినీ AI స్మార్ట్ అండర్‌రైటింగ్ సిస్టమ్" else "Gemini AI Credit Underwriting Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF6B21A8)
                                    )
                                    Text(
                                        text = if (isTelugu)
                                            "RBI 2022 నియమావళి ప్రకారం ఆదాయం, FOIR మరియు క్లస్టర్ సాధ్యతను స్వయంచాలకంగా విశ్లేషిస్తుంది."
                                        else
                                            "Evaluates RBI 2022 income caps, FOIR obligations, and agro-economic viability in Wanaparthy.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF7E22CE),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Section 1: Applicant Details
                    item {
                        Text(
                            text = if (isTelugu) "దరఖాస్తుదారు వివరాలు" else "1. Applicant Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = applicantName,
                                    onValueChange = { applicantName = it },
                                    label = { Text("Applicant Full Name (మహిళా పేరు)") },
                                    placeholder = { Text("e.g. M. Sujathamma") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_applicant_name")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = mobileNumber,
                                        onValueChange = { mobileNumber = it },
                                        label = { Text("Mobile (+91)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_applicant_mobile")
                                    )

                                    OutlinedTextField(
                                        value = aadhaarNumber,
                                        onValueChange = { aadhaarNumber = it },
                                        label = { Text("Aadhaar Number") },
                                        placeholder = { Text("4921-XXXX-XXXX") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_applicant_aadhaar")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Mandal Cluster Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = mandalDropdownExpanded,
                                        onExpandedChange = { mandalDropdownExpanded = !mandalDropdownExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedMandal.displayName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Mandal Cluster") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mandalDropdownExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = mandalDropdownExpanded,
                                            onDismissRequest = { mandalDropdownExpanded = false }
                                        ) {
                                            MandalCluster.values().forEach { c ->
                                                DropdownMenuItem(
                                                    text = { Text(c.displayName) },
                                                    onClick = {
                                                        selectedMandal = c
                                                        mandalDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = villageName,
                                        onValueChange = { villageName = it },
                                        label = { Text("Village (గ్రామం)") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_applicant_village")
                                    )
                                }

                                // Household Income & Debt
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = annualIncomeStr,
                                        onValueChange = { annualIncomeStr = it },
                                        label = { Text("Annual Income (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        supportingText = {
                                            Text(
                                                text = if (annualIncome > 300000.0) "Exceeds RBI limit (Max ₹3L)" else "RBI Cap: ≤ ₹3,00,000",
                                                color = if (annualIncome > 300000.0) Color(0xFFDC2626) else RupeeGreen,
                                                fontSize = 10.sp
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_applicant_income")
                                    )

                                    OutlinedTextField(
                                        value = existingDebtStr,
                                        onValueChange = { existingDebtStr = it },
                                        label = { Text("Existing Debt / Mo") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_applicant_debt")
                                    )
                                }
                            }
                        }
                    }

                    // Section 2: Loan Amount & Cycle
                    item {
                        Text(
                            text = if (isTelugu) "రుణ మొత్తం & సైకిల్" else "2. Requested Loan Amount & Cycle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = requestedAmountStr,
                                        onValueChange = { requestedAmountStr = it },
                                        label = { Text("Requested Amount (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_requested_amount")
                                    )

                                    // Cycle selector
                                    ExposedDropdownMenuBox(
                                        expanded = cycleDropdownExpanded,
                                        onExpandedChange = { cycleDropdownExpanded = !cycleDropdownExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = "Cycle $selectedCycle",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Loan Cycle") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleDropdownExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = cycleDropdownExpanded,
                                            onDismissRequest = { cycleDropdownExpanded = false }
                                        ) {
                                            (1..4).forEach { cyc ->
                                                val c = LoanCycle.forCycle(cyc)
                                                DropdownMenuItem(
                                                    text = { Text("Cycle $cyc (₹${c.minAmount / 1000}k - ₹${c.maxAmount / 1000}k)") },
                                                    onClick = {
                                                        selectedCycle = cyc
                                                        requestedAmountStr = c.minAmount.toString()
                                                        cycleDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Financial Metrics preview
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Est. Fortnightly Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = FieldBankingRepository.formatInr(estimatedInstallment),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = BankPrimary
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Projected FOIR", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "$foirPercentage%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (foirPercentage <= 50.0) RupeeGreen else Color(0xFFDC2626)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Prepayment Fee", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("0% NIL", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Purpose of Loan with AI Polish
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTelugu) "రుణ వినియోగ ఉద్దేశం" else "3. Purpose of Loan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            // AI Polish button
                            OutlinedButton(
                                onClick = {
                                    isEnhancingPurpose = true
                                    onEnhancePurposeAi(
                                        if (applicantName.isNotBlank()) applicantName else "Borrower",
                                        selectedMandal,
                                        loanPurpose,
                                        requestedAmount
                                    ) { enhanced ->
                                        loanPurpose = enhanced
                                        isEnhancingPurpose = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Loan purpose polished by Gemini AI!")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_ai_polish_purpose")
                            ) {
                                if (isEnhancingPurpose) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF9333EA))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("✨ AI Polish Purpose", fontSize = 11.sp)
                            }
                        }
                    }

                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Quick Pre-fill Suggestions:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val quickPurposes = listOf(
                                        "Cotton Crop Seeds & Fertilizers",
                                        "Dairy Buffalo & Cattle Feed",
                                        "Groundnut Harvest Inputs",
                                        "Kirana Grocery Stock",
                                        "Handloom Silk Yarn",
                                        "Poultry Feeder & Shed"
                                    )
                                    items(quickPurposes) { item ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.clickable {
                                                loanPurpose = when (item) {
                                                    "Cotton Crop Seeds & Fertilizers" -> "Purchase of high-yield cotton seeds, organic fertilizers, and weeding labor for 2-acre plot in ${selectedMandal.displayName}."
                                                    "Dairy Buffalo & Cattle Feed" -> "Acquisition of 1 high-yielding Murrah buffalo and commercial dairy feed to supply daily milk to local cooperative in ${selectedMandal.displayName}."
                                                    "Groundnut Harvest Inputs" -> "Procurement of groundnut seed kernels, bio-pesticides, and tractor tilling services."
                                                    "Kirana Grocery Stock" -> "Replenishment of fast-moving grocery FMCG inventory, cooking oils, and provisions for retail shop."
                                                    "Handloom Silk Yarn" -> "Procurement of raw mulberry silk yarn and natural dyes for traditional Gadwal/Pangal saree weaving."
                                                    else -> "Expansion of poultry shed and feed stock."
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = item,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = loanPurpose,
                                    onValueChange = { loanPurpose = it },
                                    label = { Text("Detailed Loan Purpose Narrative") },
                                    minLines = 3,
                                    maxLines = 5,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_loan_purpose")
                                )
                            }
                        }
                    }

                    // AI Underwriter Action Button
                    item {
                        Button(
                            onClick = {
                                if (applicantName.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Please enter applicant name!") }
                                    return@Button
                                }
                                val appToAnalyze = LoanApplication(
                                    applicationId = "APP-TEMP",
                                    applicantName = applicantName,
                                    mobileNumber = mobileNumber,
                                    aadhaarMasked = if (aadhaarNumber.length >= 4) "XXXX-XXXX-${aadhaarNumber.takeLast(4)}" else "XXXX-XXXX-4921",
                                    mandal = selectedMandal,
                                    village = villageName,
                                    requestedAmount = requestedAmount,
                                    loanPurpose = loanPurpose,
                                    annualIncome = annualIncome,
                                    monthlyIncome = monthlyIncome,
                                    existingMonthlyDebt = existingDebt,
                                    foirPercentage = foirPercentage,
                                    loanCycle = selectedCycle
                                )
                                isAnalyzingWithAi = true
                                onEvaluateAi(appToAnalyze) { assessment ->
                                    aiAssessmentResult = assessment
                                    isAnalyzingWithAi = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("AI Underwriter evaluation complete: ${assessment.status}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_run_ai_underwriter"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isAnalyzingWithAi) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing with Gemini 3.5 Flash...", fontSize = 13.sp)
                            } else {
                                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isTelugu) "AI అండర్‌రైటర్ విశ్లేషణను ప్రారంభించండి" else "Run Gemini AI Credit Underwriter",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // AI Credit Risk Assessment Score Card
                    if (aiAssessmentResult != null) {
                        item {
                            CreditRiskScoreCard(
                                assessment = aiAssessmentResult!!,
                                applicantName = if (applicantName.isNotBlank()) applicantName else "Applicant"
                            )
                        }
                    }

                    // Save to Room DB CTA
                    item {
                        Button(
                            onClick = {
                                if (applicantName.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Please enter applicant name!") }
                                    return@Button
                                }
                                val appId = "APP-WNP-${SimpleDateFormat("yyMMdd", Locale.ENGLISH).format(Date())}-${(1000..9999).random()}"
                                val newApp = LoanApplication(
                                    applicationId = appId,
                                    applicantName = applicantName,
                                    mobileNumber = if (mobileNumber.isNotBlank()) mobileNumber else "+91 98480 00000",
                                    aadhaarMasked = if (aadhaarNumber.length >= 4) "XXXX-XXXX-${aadhaarNumber.takeLast(4)}" else "XXXX-XXXX-4921",
                                    mandal = selectedMandal,
                                    village = villageName,
                                    requestedAmount = requestedAmount,
                                    loanPurpose = loanPurpose,
                                    annualIncome = annualIncome,
                                    monthlyIncome = monthlyIncome,
                                    existingMonthlyDebt = existingDebt,
                                    foirPercentage = foirPercentage,
                                    loanCycle = selectedCycle,
                                    status = aiAssessmentResult?.status ?: "UNDER_REVIEW",
                                    aiRiskScore = aiAssessmentResult?.riskScore ?: 80,
                                    aiAssessmentSummary = aiAssessmentResult?.summary,
                                    aiRecommendation = aiAssessmentResult?.clusterViabilityNotes,
                                    syncPending = true
                                )
                                onSubmitApplication(newApp)
                                selectedTabIndex = 1 // Switch to saved list
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_save_loan_application"),
                            colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTelugu) "దరఖాస్తును రూమ్ డాటాబేస్‌లో భద్రపరచండి" else "Save Application to Room Database",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            } else {
                // Saved Applications Tab (Room DB)
                val filteredList = state.loanApplications.filter {
                    searchQuery.isBlank() ||
                            it.applicantName.contains(searchQuery, ignoreCase = true) ||
                            it.village.contains(searchQuery, ignoreCase = true) ||
                            it.applicationId.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name, village, or ID...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_search_saved_apps"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No loan applications saved yet.", color = Color.Gray)
                                }
                            }
                        }
                    }

                    items(filteredList) { app ->
                        var isExpanded by remember { mutableStateOf(false) }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .testTag("saved_app_card_${app.applicationId}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = app.applicantName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${app.applicationId} • ${app.village} (${app.mandal.displayName})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = FieldBankingRepository.formatInr(app.requestedAmount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (app.status) {
                                                "APPROVED" -> Color(0xFFDCFCE7)
                                                "CONDITIONALLY_APPROVED" -> Color(0xFFFEF3C7)
                                                else -> Color(0xFFFEE2E2)
                                            }
                                        ) {
                                            Text(
                                                text = app.status.replace("_", " "),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (app.status) {
                                                    "APPROVED" -> Color(0xFF166534)
                                                    "CONDITIONALLY_APPROVED" -> Color(0xFFB45309)
                                                    else -> Color(0xFF991B1B)
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Purpose: ${app.loanPurpose}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isExpanded) 10 else 2
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI Score: ${app.aiRiskScore}/100 • FOIR: ${app.foirPercentage}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (app.aiRiskScore >= 75) RupeeGreen else WanaparthyGold
                                    )

                                    Text(
                                        text = if (isExpanded) "Show Less ▲" else "View Details ▼",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Annual Income: INR ₹${app.annualIncome.toInt()} (Monthly: ₹${app.monthlyIncome.toInt()})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Mobile: ${app.mobileNumber} • Aadhaar: ${app.aadhaarMasked}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (app.aiAssessmentSummary != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF3E8FF),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "Gemini AI Underwriter Notes:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF6B21A8)
                                                )
                                                Text(
                                                    text = app.aiAssessmentSummary,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF4A044E)
                                                )
                                            }
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
    }
}
