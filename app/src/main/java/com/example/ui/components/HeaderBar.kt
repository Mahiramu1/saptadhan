package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserProfile
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.WanaparthyGold

@Composable
fun HeaderBar(
    isOnline: Boolean,
    isTelugu: Boolean,
    pendingSyncCount: Int,
    userProfile: UserProfile = UserProfile(),
    onToggleOnline: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenSync: () -> Unit,
    onLogout: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(BankPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_saptadhan_logo),
                            contentDescription = "Saptadhan Logo",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Saptadhan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = WanaparthyGold.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "TG-WNP-01",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isTelugu) "సప్తధాన్ ఫైనాన్స్ • saptadhan.com" else "Saptadhan Finance • saptadhan.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language switcher button
                    FilterChip(
                        selected = isTelugu,
                        onClick = onToggleLanguage,
                        label = {
                            Text(
                                text = if (isTelugu) "తెలుగు" else "ENG",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Change Language",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BankPrimaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("language_toggle_chip")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Sync Status Indicator with Badge
                    IconButton(
                        onClick = onOpenSync,
                        modifier = Modifier.testTag("sync_manager_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (pendingSyncCount > 0) {
                                    Badge {
                                        Text(text = pendingSyncCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "Sync Queue",
                                tint = if (isOnline) BankPrimary else Color(0xFFE53935)
                            )
                        }
                    }

                    // Logout Button
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("btn_header_logout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-status bar for Network connectivity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline) {
                            if (isTelugu) "ఆన్‌లైన్ (BR.Net గేట్‌వే క్రియాశీలం)" else "Online (BR.Net Gateway Live)"
                        } else {
                            if (isTelugu) "ఆఫ్‌లైన్ మోడ్ (లోకల్ ఎన్‌క్రిప్టెడ్ క్యాష్)" else "Offline-First Mode (Local Encrypted DB)"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isOnline) "Tap to go Offline" else "Tap to go Online",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("network_toggle_text")
                )
            }
        }
    }
}
