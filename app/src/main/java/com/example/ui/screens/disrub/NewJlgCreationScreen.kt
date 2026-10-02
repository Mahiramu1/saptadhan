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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Borrower
import com.example.data.model.LoanCycle
import com.example.data.model.MandalCluster
import com.example.data.repository.FieldBankingRepository
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BankingUiState
import kotlinx.coroutines.launch

data class BorrowerDraft(
    var name: String = "",
    var spouseName: String = "",
    var mobile: String = "",
    var aadhaar: String = "",
    var panOrForm60: String = "Form-60 Verified",
    var annualIncome: String = "185000",
    var existingMonthlyDebt: String = "2500",
    var bankAccount: String = "982341098231",
    var bankIfsc: String = "SBIN0020141",
    var bankName: String = "State Bank of India",
    var geoLat: Double = 16.3815,
    var geoLong: Double = 78.0219,
    var isAadhaarOtpVerified: Boolean = false,
    var isGeoTagged: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJlgCreationScreen(
    state: BankingUiState,
    onNavigateBack: () -> Unit,
    onCreateJlg: (String, String, MandalCluster, Int, List<Borrower>) -> Unit
) {
    val isTelugu = state.isTeluguLanguage
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var groupName by remember { mutableStateOf("Sri Bhavani Mahila Sangham") }
    var selectedMandal by remember { mutableStateOf(MandalCluster.KOTHAKOTA) }
    var selectedCenterId by remember { mutableStateOf(state.centers.firstOrNull()?.centerId ?: "KND-KTH-04") }
    var selectedCycle by remember { mutableIntStateOf(1) }
    var crossGuaranteeAgreed by remember { mutableStateOf(false) }

    // Dropdown expanded states
    var mandalDropdownExpanded by remember { mutableStateOf(false) }
    var centerDropdownExpanded by remember { mutableStateOf(false) }
    var cycleDropdownExpanded by remember { mutableStateOf(false) }

    // 5 Female Borrowers Draft list
    val borrowerDrafts = remember {
        mutableStateListOf(
            BorrowerDraft(name = "K. Suvarnamma", spouseName = "K. Narayana", mobile = "9848077112", aadhaar = "8891-2341-9981", isAadhaarOtpVerified = true, isGeoTagged = true),
            BorrowerDraft(name = "V. Bhagyalakshmi", spouseName = "V. Ramu", mobile = "9848077113", aadhaar = "4512-8821-3312", isAadhaarOtpVerified = true, isGeoTagged = true),
            BorrowerDraft(name = "T. Sharadamma", spouseName = "T. Anji", mobile = "9848077114", aadhaar = "6712-9901-2218", isAadhaarOtpVerified = true, isGeoTagged = true),
            BorrowerDraft(name = "M. Sridevi", spouseName = "M. Srinivas", mobile = "9848077115", aadhaar = "3319-5541-7712", isAadhaarOtpVerified = true, isGeoTagged = true),
            BorrowerDraft(name = "B. Parvathamma", spouseName = "B. Yadagiri", mobile = "9848077116", aadhaar = "9901-4412-6631", isAadhaarOtpVerified = true, isGeoTagged = true)
        )
    }

    var showOtpDialogForIndex by remember { mutableStateOf<Int?>(null) }
    var inputOtp by remember { mutableStateOf("482910") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isTelugu) "కొత్త JLG నమోదు (5 సభ్యులు)" else "New JLG Sourcing (5 Members)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Wanaparthy Branch • TG-WNP-01",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_to_sourcing")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Spacer(modifier = Modifier.height(4.dp))
                // Regulatory Banner
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu)
                                "RBI నియమావళి ప్రకారం 5 మంది అర్హులైన మహిళా సభ్యులు మాత్రమే ఉండాలి. కుటుంబ వార్షిక ఆదాయం ₹3,00,000 లోపు ఉండాలి."
                            else
                                "RBI 2022 Guidelines: Exactly 5 eligible female borrowers required. Annual household income must be ≤ ₹3,00,000.",
                            fontSize = 11.sp,
                            color = Color(0xFF1E40AF),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Group Metadata
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isTelugu) "గ్రూప్ వివరాలు" else "JLG Group Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Group Name (సంఘం పేరు)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_group_name")
                        )

                        // Cluster dropdown
                        ExposedDropdownMenuBox(
                            expanded = mandalDropdownExpanded,
                            onExpandedChange = { mandalDropdownExpanded = !mandalDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedMandal.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mandal Cluster (మండలం)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mandalDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = mandalDropdownExpanded,
                                onDismissRequest = { mandalDropdownExpanded = false }
                            ) {
                                MandalCluster.values().forEach { cluster ->
                                    DropdownMenuItem(
                                        text = { Text("${cluster.displayName} - ${cluster.description.take(24)}...") },
                                        onClick = {
                                            selectedMandal = cluster
                                            mandalDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Loan Cycle Selector
                        ExposedDropdownMenuBox(
                            expanded = cycleDropdownExpanded,
                            onExpandedChange = { cycleDropdownExpanded = !cycleDropdownExpanded }
                        ) {
                            val cycleObj = LoanCycle.forCycle(selectedCycle)
                            OutlinedTextField(
                                value = "Cycle $selectedCycle: ₹${cycleObj.minAmount / 1000}k - ₹${cycleObj.maxAmount / 1000}k",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Loan Cycle (రుణ సైకిల్)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = cycleDropdownExpanded,
                                onDismissRequest = { cycleDropdownExpanded = false }
                            ) {
                                (1..4).forEach { cycleNum ->
                                    val c = LoanCycle.forCycle(cycleNum)
                                    DropdownMenuItem(
                                        text = { Text("Cycle $cycleNum: ₹${c.minAmount / 1000}k to ₹${c.maxAmount / 1000}k per member") },
                                        onClick = {
                                            selectedCycle = cycleNum
                                            cycleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5 Borrowers Form
            item {
                Text(
                    text = if (isTelugu) "5 మంది సభ్యుల e-KYC & వివరాలు" else "5 Female Borrowers e-KYC & Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            items(5) { index ->
                val draft = borrowerDrafts[index]
                val roleTitle = when (index) {
                    0 -> "Member #1 (Designated Leader)"
                    1 -> "Member #2 (Designated Co-Leader)"
                    else -> "Member #${index + 1}"
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrower_form_$index"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = roleTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (index < 2) BankPrimary else MaterialTheme.colorScheme.onSurface
                            )

                            // Geo-tag badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (draft.isGeoTagged) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                modifier = Modifier.clickable {
                                    draft.isGeoTagged = true
                                    draft.geoLat = 16.3812 + (index * 0.001)
                                    draft.geoLong = 78.0214 + (index * 0.001)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("GPS lat/long tagged: ${draft.geoLat}, ${draft.geoLong}")
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (draft.isGeoTagged) Color(0xFF166534) else Color(0xFFB45309),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (draft.isGeoTagged) "Geo-Tagged" else "Tag GPS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (draft.isGeoTagged) Color(0xFF166534) else Color(0xFFB45309)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.name,
                                onValueChange = { draft.name = it },
                                label = { Text("Borrower Full Name") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draft.mobile,
                                onValueChange = { draft.mobile = it },
                                label = { Text("Mobile (+91)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.aadhaar,
                                onValueChange = { draft.aadhaar = it },
                                label = { Text("Aadhaar Number") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draft.panOrForm60,
                                onValueChange = { draft.panOrForm60 = it },
                                label = { Text("PAN / Form 60") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.annualIncome,
                                onValueChange = { draft.annualIncome = it },
                                label = { Text("Annual Income (≤ ₹3L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draft.existingMonthlyDebt,
                                onValueChange = { draft.existingMonthlyDebt = it },
                                label = { Text("Monthly Debt") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // e-KYC Aadhaar XML / OTP Verify Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (draft.isAadhaarOtpVerified) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = RupeeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "UIDAI Aadhaar XML Verified with OTP",
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        draft.isAadhaarOtpVerified = true
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Aadhaar OTP 482910 Verified successfully!")
                                        }
                                    },
                                    modifier = Modifier.testTag("btn_verify_aadhaar_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify Aadhaar OTP", fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "CRIF Green Score: 735",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            // Cross-Guarantee Agreement
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = WanaparthyGold.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = crossGuaranteeAgreed,
                            onCheckedChange = { crossGuaranteeAgreed = it },
                            modifier = Modifier.testTag("checkbox_cross_guarantee")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTelugu)
                                "JLG ఉమ్మడి బాధ్యతా ఒప్పందం: 5 మంది సభ్యులు పరస్పర గ్యారెంటీని అంగీకరించారు. ఒక సభ్యురాలు చెల్లించకపోతే మిగిలిన 4 మంది బాధ్యత వహిస్తారు."
                            else
                                "JLG Cross-Guarantee Digital Consent: All 5 members accept mutual liability. In case of default during Kendra meeting, group mutual guarantee protocol will be triggered.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Final Submit Button
            item {
                Button(
                    onClick = {
                        val incomeCapExceeded = borrowerDrafts.any { (it.annualIncome.toDoubleOrNull() ?: 0.0) > 300000.0 }
                        if (incomeCapExceeded) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Error: RBI limit is max ₹3,00,000 annual income!")
                            }
                            return@Button
                        }
                        if (!crossGuaranteeAgreed) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please check Cross-Guarantee consent!")
                            }
                            return@Button
                        }

                        val borrowersToCreate = borrowerDrafts.mapIndexed { i, draft ->
                            val inc = draft.annualIncome.toDoubleOrNull() ?: 180000.0
                            val monthly = inc / 12.0
                            val debt = draft.existingMonthlyDebt.toDoubleOrNull() ?: 2000.0
                            val foir = if (monthly > 0) ((debt + 1500.0) / monthly) * 100.0 else 25.0
                            Borrower(
                                borrowerId = "TEMP-$i",
                                groupId = "",
                                fullName = draft.name,
                                spouseOrFatherName = draft.spouseName,
                                aadhaarMasked = "XXXX-XXXX-${draft.aadhaar.takeLast(4)}",
                                panOrForm60 = draft.panOrForm60,
                                mobileNumber = "+91 ${draft.mobile}",
                                village = "Wanaparthy Cluster",
                                mandal = selectedMandal,
                                annualHouseholdIncome = inc,
                                monthlyHouseholdIncome = monthly,
                                existingMonthlyDebtObligations = debt,
                                crifCreditScore = 720 + (i * 8),
                                activeMfiCount = 1 + (i % 2),
                                geoLatitude = draft.geoLat,
                                geoLongitude = draft.geoLong,
                                bankAccountNumberMasked = "XXXX-XXXX-${draft.bankAccount.takeLast(4)}",
                                bankIfsc = draft.bankIfsc,
                                bankName = draft.bankName,
                                isKycVerified = true,
                                isBureauPassed = true,
                                foirPercentage = Math.round(foir * 10.0) / 10.0,
                                syncPending = true
                            )
                        }

                        onCreateJlg(groupName, selectedCenterId, selectedMandal, selectedCycle, borrowersToCreate)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_submit_jlg"),
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTelugu) "JLG గ్రూపును నమోదు చేయండి" else "Register JLG & Proceed to CGT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
