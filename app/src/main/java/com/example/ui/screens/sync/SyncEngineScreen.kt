package com.example.ui.screens.sync

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import com.example.ui.viewmodel.BankingUiState
import kotlinx.coroutines.launch

@Composable
fun SyncEngineScreen(
    state: BankingUiState,
    onToggleOnlineMode: () -> Unit,
    onMorningSync: () -> Unit,
    onEveningSync: () -> Unit
) {
    val isTelugu = state.isTeluguLanguage
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingReceipts = state.repayments.count { it.syncPending }
    val pendingBorrowers = state.borrowers.count { it.syncPending }
    val pendingGroups = state.groups.count { it.syncPending }
    val totalPending = pendingReceipts + pendingBorrowers + pendingGroups

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTelugu) "ఆఫ్‌లైన్-ఫస్ట్ సింక్రొనైజేషన్ ఇంజిన్" else "Offline-First Sync Engine",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTelugu) "క్రాఫ్ట్ సిలికాన్ BR.Net ESB రెండు-మార్గాల సింక్" else "Craft Silicon BR.Net ESB Two-Way Sync Gateway",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = BankPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Connectivity Toggle Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_network_mode"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (state.isOnlineMode) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (state.isOnlineMode) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.isOnlineMode) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (state.isOnlineMode) RupeeGreen else Color(0xFFB45309)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (state.isOnlineMode) "Online Gateway Active" else "Zero-Connectivity Offline Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (state.isOnlineMode)
                                        "Connected to Wanaparthy Hub (TLS 1.3)"
                                    else
                                        "Local Room SQLite with SQLCipher Encryption",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = state.isOnlineMode,
                            onCheckedChange = { onToggleOnlineMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = BankPrimary),
                            modifier = Modifier.testTag("switch_online_offline")
                        )
                    }
                }
            }

            // Sync Queue Stats
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTelugu) "స్థానిక సింక్ క్యూ (పెండింగ్ రికార్డులు)" else "Local Sync Queue (Offline Buffer)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (totalPending > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "$totalPending Pending",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalPending > 0) Color(0xFFB45309) else Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SyncMetricRow("Collection Receipts Buffer", "$pendingReceipts records", Icons.Default.CloudUpload)
                        SyncMetricRow("New Borrower e-KYC Submissions", "$pendingBorrowers records", Icons.Default.CloudUpload)
                        SyncMetricRow("New JLG Groups & CGT Milestones", "$pendingGroups records", Icons.Default.CloudUpload)
                    }
                }
            }

            // Morning Sync Action
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BankPrimaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isTelugu) "ఉదయం సింక్ (బ్రాంచ్ Wi-Fi)" else "Morning Sync (Branch Wi-Fi)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Downloads daily demand sheets, center rosters & borrower profiles",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onMorningSync()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Morning Sync: Rosters refreshed from BR.Net ESB.")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_morning_sync"),
                            colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Execute Morning Sync (Fetch Daily Demand)")
                        }
                    }
                }
            }

            // Evening Delta Sync Action
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFDCFCE7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = RupeeGreen)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isTelugu) "సాయంత్రం డెల్టా సింక్ (సెల్యులార్ / బ్రాంచ్)" else "Evening Sync (Cellular / Branch)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Pushes delta changes with idempotent retry to Craft Silicon BR.Net",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onEveningSync()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Evening Sync: Pushed offline buffer to BR.Net ESB.")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_evening_sync"),
                            colors = ButtonDefaults.buttonColors(containerColor = RupeeGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Push Evening Delta Queue ($totalPending records)")
                        }
                    }
                }
            }

            // Security & Encryption Card
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = BankPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AES-256 SQLCipher Local Storage",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Wanaparthy Branch Code TG-WNP-01 cryptographic key seals all offline borrower Aadhaar XML and collection slips.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

@Composable
fun SyncMetricRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
