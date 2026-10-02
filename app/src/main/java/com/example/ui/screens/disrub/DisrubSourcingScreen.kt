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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiLoanAssessment
import com.example.data.ai.GeminiLoanService
import com.example.data.model.Borrower
import com.example.data.model.JlgGroup
import com.example.data.model.LoanApplication
import com.example.data.repository.FieldBankingRepository
import com.example.ui.components.CreditRiskScoreCard
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BankingUiState

@Composable
fun DisrubSourcingScreen(
    state: BankingUiState,
    onNavigate: (AppScreen) -> Unit,
    onSelectGroup: (String) -> Unit,
    onOpenKfs: (Borrower, JlgGroup) -> Unit,
    onRunBureauCheck: (Borrower) -> Unit
) {
    val isTelugu = state.isTeluguLanguage
    var expandedGroupId by remember { mutableStateOf<String?>(state.selectedGroupId ?: state.groups.firstOrNull()?.groupId) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Module Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTelugu) "డిస్రబ్ : గ్రూప్ లోన్ ఆరిజినేషన్" else "Disrub: JLG Loan Origination",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTelugu) "5 సభ్యుల మహిళా గ్రూప్ (JLG) సోర్సింగ్ & e-KYC" else "5-Member Joint Liability Group Sourcing & e-KYC",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onNavigate(AppScreen.LOAN_APPLICATION) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_loan_application_ai")
                        ) {
                            Text(text = if (isTelugu) "AI దరఖాస్తు" else "AI Loan App", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onNavigate(AppScreen.DISRUB_NEW_JLG) },
                            colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_new_jlg")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New JLG",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isTelugu) "కొత్త JLG" else "+ JLG", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Overview stats banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.groups.size.toString(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isTelugu) "JLG గ్రూపులు" else "Total JLGs",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.borrowers.size.toString(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = RupeeGreen
                            )
                            Text(
                                text = if (isTelugu) "మహిళా రుణగ్రహీతలు" else "Women Borrowers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.groups.count { it.grtApprovedByBm }.toString(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = WanaparthyGold
                            )
                            Text(
                                text = if (isTelugu) "GRT ఆమోదిత" else "GRT Sanctioned",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // JLG Group List
            items(state.groups) { group ->
                val groupBorrowers = state.borrowers.filter { it.groupId == group.groupId }
                val isExpanded = expandedGroupId == group.groupId

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_card_${group.groupId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedGroupId = if (isExpanded) null else group.groupId
                                    onSelectGroup(group.groupId)
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = group.groupName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = BankPrimaryContainer
                                    ) {
                                        Text(
                                            text = "Cycle ${group.loanCycle}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${group.mandal.displayName} • ${group.groupId} • 5 Members",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = FieldBankingRepository.formatInr(group.sanctionedAmountPerMember),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "/ member",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status badges (CGT 1, 2, 3 and GRT)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusChip("CGT 1", group.cgt1Completed)
                            StatusChip("CGT 2", group.cgt2Completed)
                            StatusChip("CGT 3", group.cgt3Completed)
                            StatusChip("GRT BM", group.grtApprovedByBm)
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isTelugu) "JLG మహిళా సభ్యులు (5 మంది):" else "JLG Female Members (5 Borrowers):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            groupBorrowers.forEachIndexed { index, borrower ->
                                BorrowerItemRow(
                                    index = index + 1,
                                    borrower = borrower,
                                    isTelugu = isTelugu,
                                    onKfsClick = { onOpenKfs(borrower, group) },
                                    onBureauClick = { onRunBureauCheck(borrower) }
                                )
                                if (index < groupBorrowers.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons for CGT / GRT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onSelectGroup(group.groupId)
                                        onNavigate(AppScreen.DISRUB_CGT_GRT)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_cgt_grt_${group.groupId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (group.grtApprovedByBm) "View Sanction" else "CGT & GRT Workflow",
                                        fontSize = 12.sp
                                    )
                                }
                            }
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

@Composable
fun StatusChip(label: String, isDone: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isDone) Color(0xFFD1FAE5) else Color(0xFFF1F5F9)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Shield,
                contentDescription = null,
                tint = if (isDone) RupeeGreen else Color(0xFF94A3B8),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDone) Color(0xFF065F46) else Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun BorrowerItemRow(
    index: Int,
    borrower: Borrower,
    isTelugu: Boolean,
    onKfsClick: () -> Unit,
    onBureauClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(BankPrimaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = borrower.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Aadhaar: ${borrower.aadhaarMasked} • PAN/60: ${borrower.panOrForm60}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // CRIF Score Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (borrower.crifCreditScore >= 700) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    modifier = Modifier.clickable { onBureauClick() }
                ) {
                    Text(
                        text = "CRIF ${borrower.crifCreditScore}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (borrower.crifCreditScore >= 700) Color(0xFF166534) else Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Income: ₹${(borrower.annualHouseholdIncome / 1000).toInt()}k/yr • FOIR: ${borrower.foirPercentage}%",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    Text(
                        text = "GPS Tagged",
                        fontSize = 10.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View KFS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onKfsClick() }
                    )
                }
            }
        }
    }
}
