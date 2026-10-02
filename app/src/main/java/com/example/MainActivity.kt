package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.FieldBankingRepository
import com.example.ui.components.HeaderBar
import com.example.ui.components.NavigationBottomBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.collect.FieldCollectionsScreen
import com.example.ui.screens.collect.KendraCentersScreen
import com.example.ui.screens.collect.KendraMeetingScreen
import com.example.ui.screens.disrub.CgtGrtMilestonesScreen
import com.example.ui.screens.disrub.DisrubSourcingScreen
import com.example.ui.screens.disrub.KfsRow
import com.example.ui.screens.disrub.LoanApplicationScreen
import com.example.ui.screens.disrub.NewJlgCreationScreen
import com.example.ui.screens.reconciliation.DayEndCashReconciliationScreen
import com.example.ui.screens.sync.SyncEngineScreen
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RupeeGreen
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FieldBankingViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FieldBankingViewModel = viewModel()
                FieldBankingApp(viewModel)
            }
        }
    }
}

@Composable
fun FieldBankingApp(viewModel: FieldBankingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeKfs by viewModel.activeKfs.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show sync message toast
    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    // Unauthenticated state: Login Screen
    if (!uiState.isLoggedIn || uiState.currentScreen == AppScreen.LOGIN) {
        LoginScreen(
            isTelugu = uiState.isTeluguLanguage,
            onToggleLanguage = { viewModel.toggleLanguage() },
            onLoginSuccess = { officerCode, role ->
                viewModel.login(officerCode, role)
            }
        )
        return
    }

    // Comprehensive BackHandler
    BackHandler(enabled = uiState.currentScreen != AppScreen.DASHBOARD && uiState.currentScreen != AppScreen.LOGIN) {
        when (uiState.currentScreen) {
            AppScreen.DISRUB_NEW_JLG, AppScreen.DISRUB_CGT_GRT, AppScreen.LOAN_APPLICATION -> viewModel.navigateTo(AppScreen.DISRUB_SOURCING)
            AppScreen.COLLECT_MEETING -> viewModel.navigateTo(AppScreen.COLLECT_CENTERS)
            AppScreen.FIELD_COLLECTIONS -> viewModel.navigateTo(AppScreen.DASHBOARD)
            else -> viewModel.navigateTo(AppScreen.DASHBOARD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HeaderBar(
                isOnline = uiState.isOnlineMode,
                isTelugu = uiState.isTeluguLanguage,
                pendingSyncCount = uiState.pendingSyncCount,
                userProfile = uiState.currentUser,
                onToggleOnline = { viewModel.toggleOnlineMode() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onOpenSync = { viewModel.navigateTo(AppScreen.SYNC_ENGINE) },
                onLogout = { viewModel.logout() }
            )
        },
        bottomBar = {
            NavigationBottomBar(
                currentScreen = uiState.currentScreen,
                isTelugu = uiState.isTeluguLanguage,
                onNavigate = { screen -> viewModel.navigateTo(screen) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                AppScreen.LOGIN -> {
                    // Fallback if inside scaffold
                    LoginScreen(
                        isTelugu = uiState.isTeluguLanguage,
                        onToggleLanguage = { viewModel.toggleLanguage() },
                        onLoginSuccess = { officerCode, role ->
                            viewModel.login(officerCode, role)
                        }
                    )
                }

                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        state = uiState,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onSelectCenter = { centerId -> viewModel.selectCenter(centerId) }
                    )
                }

                AppScreen.FIELD_COLLECTIONS -> {
                    FieldCollectionsScreen(
                        state = uiState,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        onCollectPayment = { item, amt, mode, reason, guar, lat, lng, acc, addr ->
                            viewModel.collectPendingRepayment(item, amt, mode, reason, guar, lat, lng, acc, addr)
                        },
                        onOpenDynamicUpi = { item ->
                            val borrower = uiState.borrowers.find { it.borrowerId == item.borrowerId }
                            if (borrower != null) viewModel.prepareDynamicUpi(borrower)
                        },
                        onDismissReceipt = { viewModel.dismissReceipt() }
                    )
                }

                AppScreen.DISRUB_SOURCING -> {
                    DisrubSourcingScreen(
                        state = uiState,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onSelectGroup = { groupId -> viewModel.selectGroup(groupId) },
                        onOpenKfs = { borrower, group -> viewModel.openKfs(borrower, group) },
                        onRunBureauCheck = { borrower -> viewModel.runCreditBureauCheck(borrower) }
                    )
                }

                AppScreen.DISRUB_NEW_JLG -> {
                    NewJlgCreationScreen(
                        state = uiState,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.DISRUB_SOURCING) },
                        onCreateJlg = { name, centerId, mandal, cycle, borrowers ->
                            viewModel.createJlgGroup(name, centerId, mandal, cycle, borrowers)
                        }
                    )
                }

                AppScreen.DISRUB_CGT_GRT -> {
                    CgtGrtMilestonesScreen(
                        state = uiState,
                        groupId = uiState.selectedGroupId,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.DISRUB_SOURCING) },
                        onUpdateCgt = { gId, milestone -> viewModel.updateCgtStatus(gId, milestone) },
                        onApproveGrt = { gId, bm -> viewModel.approveGrt(gId, bm) },
                        onOpenKfs = { borrower, group -> viewModel.openKfs(borrower, group) }
                    )
                }

                AppScreen.LOAN_APPLICATION -> {
                    LoanApplicationScreen(
                        state = uiState,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.DISRUB_SOURCING) },
                        onSubmitApplication = { application ->
                            viewModel.submitLoanApplication(application)
                        },
                        onEvaluateAi = { application, onResult ->
                            viewModel.evaluateLoanApplicationWithAi(application, onResult)
                        },
                        onEnhancePurposeAi = { name, mandal, purpose, amt, onResult ->
                            viewModel.enhancePurposeWithAi(name, mandal, purpose, amt, onResult)
                        }
                    )
                }

                AppScreen.COLLECT_CENTERS -> {
                    KendraCentersScreen(
                        state = uiState,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onSelectCenter = { centerId -> viewModel.selectCenter(centerId) }
                    )
                }

                AppScreen.COLLECT_MEETING -> {
                    KendraMeetingScreen(
                        state = uiState,
                        centerId = uiState.selectedCenterId,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.COLLECT_CENTERS) },
                        onCollectCash = { cId, gId, b, amt, att ->
                            viewModel.collectCashRepayment(cId, gId, b, amt, att)
                        },
                        onOpenDynamicUpi = { b -> viewModel.prepareDynamicUpi(b) },
                        onCompleteUpiPayment = { cId, gId, b, amt, app ->
                            viewModel.completeUpiPayment(cId, gId, b, amt, app)
                        },
                        onRecordDelinquency = { cId, gId, b, amt, reason, guarantee ->
                            viewModel.recordDelinquency(cId, gId, b, amt, reason, guarantee)
                        },
                        onDismissReceipt = { viewModel.dismissReceipt() }
                    )
                }

                AppScreen.RECONCILIATION -> {
                    DayEndCashReconciliationScreen(
                        state = uiState,
                        onCalculateDenominations = { n5, n2, n1, n50, n20, n10, sysCash ->
                            viewModel.calculateDenominations(n5, n2, n1, n50, n20, n10, sysCash)
                        },
                        onCompleteHandover = { otp, recon ->
                            viewModel.handoverToBranchTeller(otp, recon)
                        }
                    )
                }

                AppScreen.SYNC_ENGINE -> {
                    SyncEngineScreen(
                        state = uiState,
                        onToggleOnlineMode = { viewModel.toggleOnlineMode() },
                        onMorningSync = { viewModel.performMorningSync() },
                        onEveningSync = { viewModel.performEveningSync() }
                    )
                }
            }
        }
    }

    // Global Key Fact Statement (KFS) Modal
    if (activeKfs != null) {
        val kfs = activeKfs!!
        var showTelugu by remember { mutableStateOf(uiState.isTeluguLanguage) }

        AlertDialog(
            onDismissRequest = { viewModel.closeKfs() },
            title = {
                Text(
                    text = if (showTelugu) "ముఖ్య సమాచార పత్రం (KFS)" else "Key Fact Statement (KFS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    KfsRow("Borrower", kfs.borrowerName)
                    KfsRow("JLG Group", kfs.groupName)
                    KfsRow("Sanction Amount", FieldBankingRepository.formatInr(kfs.loanAmount), isBold = true)
                    KfsRow("Net Disbursed", FieldBankingRepository.formatInr(kfs.netDisbursedAmount), isBold = true, valueColor = RupeeGreen)
                    KfsRow("Fortnightly Due", FieldBankingRepository.formatInr(kfs.installmentAmount), isBold = true)
                    KfsRow("Reducing Rate", "${kfs.reducingInterestRatePa}% p.a.")
                    KfsRow("Processing Fee", "₹${(kfs.processingFeeAmount + kfs.gstOnProcessingFee).toInt()} (1.5% + GST)")
                    KfsRow("Effective APR", "${kfs.aprPercentage}%", isBold = true)
                    KfsRow("Prepayment Fee", kfs.prepaymentPenalty)
                    HorizontalDivider()
                    Text(
                        text = if (showTelugu) "తెలుగులో లేదా ఇంగ్లీషులో వీక్షించండి:" else "Change language:",
                        fontSize = 11.sp
                    )
                    OutlinedButton(
                        onClick = { showTelugu = !showTelugu },
                        modifier = Modifier.testTag("btn_kfs_modal_lang")
                    ) {
                        Text(if (showTelugu) "English" else "తెలుగు", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeKfs()
                        scope.launch {
                            snackbarHostState.showSnackbar("KFS dispatched to borrower's phone via SMS.")
                        }
                    }
                ) {
                    Text("OK & Dispatch SMS")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.closeKfs() }) {
                    Text("Close")
                }
            }
        )
    }
}
