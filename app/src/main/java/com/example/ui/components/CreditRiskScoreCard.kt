package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiLoanAssessment
import com.example.data.repository.FieldBankingRepository
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold

@Composable
fun CreditRiskScoreCard(
    assessment: AiLoanAssessment,
    applicantName: String,
    modifier: Modifier = Modifier
) {
    val score = assessment.riskScore
    val scoreColor = when {
        score >= 75 -> RupeeGreen
        score >= 60 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    val containerBg = when {
        score >= 75 -> Color(0xFFF0FDF4)
        score >= 60 -> Color(0xFFFFFBEB)
        else -> Color(0xFFFEF2F2)
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_credit_risk_assessment"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerBg)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with Gemini AI badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
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
                            text = "Credit Risk Assessment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Powered by Gemini 3.5 Flash • Wanaparthy Hub",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF9333EA)
                ) {
                    Text(
                        text = "AI VERIFIED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider()

            // Large Score Meter & Grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Gauge
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (score / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = scoreColor,
                        trackColor = Color(0xFFE2E8F0),
                        strokeWidth = 8.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = score.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = scoreColor
                        )
                        Text(
                            text = "/ 100",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Grade & Category Pills
                Column(
                    modifier = Modifier.weight(1f).padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = scoreColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = assessment.riskGrade,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = scoreColor
                        ) {
                            Text(
                                text = assessment.riskCategory,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Est. Probability of Default (PD): ${assessment.probabilityOfDefault}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Recommended Sanction: ${FieldBankingRepository.formatInr(assessment.recommendedAmount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BankPrimary
                    )
                }
            }

            // 4 Pillar Breakdown
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Underwriting Pillar Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    ScorePillarRow("1. Household Income Stability (RBI Cap ≤ ₹3L)", assessment.scoreBreakdown.incomeStabilityScore)
                    ScorePillarRow("2. Debt Service Ratio (FOIR ≤ 50%)", assessment.scoreBreakdown.debtServiceRatioScore)
                    ScorePillarRow("3. Loan Purpose Economic Feasibility", assessment.scoreBreakdown.purposeViabilityScore)
                    ScorePillarRow("4. Wanaparthy Cluster Agro-Economics", assessment.scoreBreakdown.clusterAgroEconomicsScore)
                }
            }

            // Summary
            Text(
                text = assessment.summary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Agro-economic Note
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BankPrimaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BankPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = assessment.clusterViabilityNotes,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Strengths and Risks
            if (assessment.keyStrengths.isNotEmpty() || assessment.riskFactors.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (assessment.keyStrengths.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Key Strengths", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            assessment.keyStrengths.take(2).forEach { s ->
                                Text("✓ $s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (assessment.riskFactors.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Risk Factors", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            assessment.riskFactors.take(2).forEach { r ->
                                Text("⚠ $r", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Prototype Caution Notice
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Notice: AI Credit Risk Assessment generated by Gemini 3.5 Flash prototype. Field Credit Officer verification required prior to Branch Manager sign-off.",
                    fontSize = 9.sp,
                    color = Color(0xFF92400E),
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ScorePillarRow(label: String, score: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$score/100", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = when {
                score >= 75 -> RupeeGreen
                score >= 60 -> WanaparthyGold
                else -> Color(0xFFDC2626)
            },
            trackColor = Color(0xFFE2E8F0)
        )
    }
}
