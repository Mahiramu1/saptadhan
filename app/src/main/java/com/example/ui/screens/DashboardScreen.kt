package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MandalCluster
import com.example.data.repository.FieldBankingRepository
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.BankSecondary
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BankingUiState

@Composable
fun DashboardScreen(
    state: BankingUiState,
    onNavigate: (AppScreen) -> Unit,
    onSelectCenter: (String) -> Unit
) {
    val totalDemand = state.centers.sumOf { it.expectedDemand }
    val totalCollected = state.centers.sumOf { it.collectedAmount }
    val progress = if (totalDemand > 0) (totalCollected / totalDemand).toFloat().coerceIn(0f, 1f) else 0f
    val efficiencyPercent = if (totalDemand > 0) (totalCollected / totalDemand * 100).toInt() else 0

    val isTelugu = state.isTeluguLanguage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Hero Field Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_officer_card"),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = BankPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isTelugu) "ఫీల్డ్ క్రెడిట్ అధికారి" else "Field Credit Officer (FCO)",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "K. Srinivas (FCO-WNP-08)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = WanaparthyGold
                        ) {
                            Text(
                                text = "TG-WNP-01",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isTelugu) "ఈ రోజు డిమాండ్" else "Today's Demand",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = FieldBankingRepository.formatInr(totalDemand),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isTelugu) "వసూలైన మొత్తం" else "Total Collected",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = FieldBankingRepository.formatInr(totalCollected),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF78F8D4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF78F8D4),
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isTelugu) "కలెక్షన్ సామర్థ్యం: $efficiencyPercent%" else "Efficiency: $efficiencyPercent%",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${state.centers.size} Kendra Centers",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Primary Field Collections Hero Spotlight Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AppScreen.FIELD_COLLECTIONS) }
                    .testTag("action_open_field_collections"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF0FDF4))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(RupeeGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = "Collections",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isTelugu) "ఫీల్డ్ వసూళ్ల డ్యాష్‌బోర్డ్" else "Field Collections Dashboard",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF14532D)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RupeeGreen
                                ) {
                                    Text(
                                        text = "LIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Wanaparthy Outstanding: ${FieldBankingRepository.formatInr(state.totalPendingDemand)} • ${state.pendingRepayments.count { it.pendingAmount > 0 }} Pending",
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = RupeeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Primary Actions
        item {
            Text(
                text = if (isTelugu) "ప్రధాన ఫీల్డ్ కార్యకలాపాలు" else "Field Banking Modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Module A: Disrub
                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AppScreen.DISRUB_SOURCING) }
                        .testTag("action_disrub_sourcing"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE0F2FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Disrub",
                                tint = BankSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Disrub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isTelugu) "JLG సోర్సింగ్ & e-KYC" else "JLG Sourcing & e-KYC",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BankPrimaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${state.groups.size} Groups Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Module B: Collections Dashboard
                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AppScreen.FIELD_COLLECTIONS) }
                        .testTag("action_collect_servicing"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Collect",
                                tint = RupeeGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isTelugu) "ఫీల్డ్ వసూళ్లు" else "Collections",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isTelugu) "బాకీ వాయిదాలు & రసీదులు" else "Pending Repayments",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "${state.pendingRepayments.count { it.pendingAmount > 0 }} Pending",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Module C: Reconciliation
                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AppScreen.RECONCILIATION) }
                        .testTag("action_cash_reconcile"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = "Reconciliation",
                                tint = Color(0xFFB45309)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isTelugu) "క్యాష్ లెక్క" else "Reconcile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isTelugu) "నోట్ల లెక్క & బ్రాంచ్ హాండోవర్" else "Denominations & Handover",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Module D: Sync
                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(AppScreen.SYNC_ENGINE) }
                        .testTag("action_sync_engine"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFF3E8FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = Color(0xFF7E22CE)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isTelugu) "సింక్ ఇంజిన్" else "Offline Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isTelugu) "BR.Net ESB డెల్టా సింక్" else "BR.Net Delta Sync",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Feature Spotlight Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AppScreen.LOAN_APPLICATION) }
                    .testTag("action_loan_application_ai"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFBF5FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF9333EA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI Loan Underwriting",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isTelugu) "రుణ దరఖాస్తు & AI అండర్‌రైటర్" else "Loan Application & AI Underwriter",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF581C87)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF9333EA)
                                ) {
                                    Text(
                                        text = "GEMINI 3.5 FLASH",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isTelugu) "దరఖాస్తు వివరాలు, ఉద్దేశం & రూమ్ డాటాబేస్ స్టోరేజ్" else "Applicant details, purpose & Room database persistence",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF9333EA)
                    )
                }
            }
        }

        // Today's Kendra Schedule List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTelugu) "ఈ రోజు కేంద్ర షెడ్యూల్" else "Today's Kendra Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isTelugu) "అన్నీ చూడండి" else "View All",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigate(AppScreen.COLLECT_CENTERS) }
                )
            }
        }

        items(state.centers.take(3)) { center ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectCenter(center.centerId)
                        onNavigate(AppScreen.COLLECT_MEETING)
                    }
                    .testTag("center_card_${center.centerId}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (center.collectedAmount >= center.expectedDemand && center.expectedDemand > 0)
                                        Color(0xFFD1FAE5)
                                    else
                                        Color(0xFFE2E8F0),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (center.collectedAmount >= center.expectedDemand && center.expectedDemand > 0)
                                    Icons.Default.CheckCircle
                                else
                                    Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (center.collectedAmount >= center.expectedDemand && center.expectedDemand > 0)
                                    RupeeGreen
                                else
                                    BankPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = center.centerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${center.villageName} • ${center.meetingTime}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Leader: ${center.centerLeaderName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = FieldBankingRepository.formatInr(center.expectedDemand),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (center.collectedAmount > 0)
                                "Paid: ${FieldBankingRepository.formatInr(center.collectedAmount)}"
                            else
                                "Pending",
                            fontSize = 11.sp,
                            color = if (center.collectedAmount > 0) RupeeGreen else Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Meeting",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Wanaparthy Mandal Clusters Row
        item {
            Text(
                text = if (isTelugu) "వనపర్తి మండల్ క్లస్టర్లు" else "Wanaparthy Mandal Clusters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MandalCluster.values()) { cluster ->
                    Card(
                        modifier = Modifier.width(190.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = cluster.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cluster.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // RBI Compliance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0FDF4)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "RBI Regulatory Framework",
                        tint = Color(0xFF166534),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "RBI మైక్రోఫైనాన్స్ నిబంధనలు (2022 మార్గదర్శకాలు)" else "RBI Regulatory Framework (2022 MFI Master Directions)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF166534)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTelugu)
                                "• కుటుంబ వార్షిక ఆదాయ పరిమితి: ≤ ₹3,00,000\n• FOIR నిష్పత్తి: గరిష్టంగా 50% మాత్రమే\n• ముందస్తు చెల్లింపు జరిమానా: సున్నా (0%)\n• పారదర్శక KFS పత్రం (తెలుగు & ఇంగ్లీష్)"
                            else
                                "• Household Income Cap: ≤ ₹3,00,000\n• FOIR Obligation: Max 50% of monthly income\n• Prepayment Penalty: 0% NIL\n• Transparent KFS (English & Telugu)",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
