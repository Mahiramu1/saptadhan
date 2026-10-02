package com.example.ui.screens.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BankPrimary
import com.example.ui.theme.BankPrimaryContainer
import com.example.ui.theme.RupeeGreen
import com.example.ui.theme.WanaparthyGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isTelugu: Boolean,
    onToggleLanguage: () -> Unit,
    onLoginSuccess: (officerCode: String, roleName: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var officerCode by remember { mutableStateOf("FCO-WNP-08") }
    var password by remember { mutableStateOf("1234") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var branchSelected by remember { mutableStateOf("TG-WNP-01 (Wanaparthy Hub)") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar with Language Toggle & Domain
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BankPrimary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "saptadhan.com",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BankPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                FilterChip(
                    selected = isTelugu,
                    onClick = onToggleLanguage,
                    label = {
                        Text(
                            text = if (isTelugu) "తెలుగు" else "English",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BankPrimaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("btn_login_language_toggle")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Brand Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BankPrimary, Color(0xFF004D40))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Emblem / Logo
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_saptadhan_logo),
                            contentDescription = "Saptadhan Logo",
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Saptadhan",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Saptadhan Finance Private Limited",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WanaparthyGold
                    ) {
                        Text(
                            text = "Branch: Wanaparthy District (TG-WNP-01)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isTelugu)
                            "సప్తధాన్ ఫీల్డ్ కలెక్షన్స్ & బ్యాంకింగ్ సిస్టమ్"
                        else
                            "Unified Field Collections & Banking Suite",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Login Form Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("card_login_form"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isTelugu) "ఫీల్డ్ ఆఫీసర్ లాగిన్" else "Field Officer Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Officer Code
                    OutlinedTextField(
                        value = officerCode,
                        onValueChange = { officerCode = it },
                        label = { Text(if (isTelugu) "ఆఫీసర్ కోడ్ / ఎంప్లాయ్ ID" else "Officer Code / Employee ID") },
                        placeholder = { Text("e.g. FCO-WNP-08") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = BankPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_officer_code"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Password / PIN
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isTelugu) "రహస్య పిన్ / పాస్‌వర్డ్" else "Security PIN / Password") },
                        placeholder = { Text("4 to 6 digit PIN") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = BankPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Branch info display
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = BankPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isTelugu) "కార్యాచరణ కేంద్రం" else "Assigned Branch Hub",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = branchSelected,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Remember Me & Forgot PIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = BankPrimary),
                                modifier = Modifier.testTag("chk_remember_me")
                            )
                            Text(
                                text = if (isTelugu) "గుర్తుంచుకో" else "Remember Device",
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = if (isTelugu) "పిన్ రీసెట్" else "Reset PIN?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BankPrimary,
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please contact Wanaparthy Branch Manager for PIN reset.")
                                    }
                                }
                                .padding(4.dp)
                        )
                    }

                    // Sign In Button
                    Button(
                        onClick = {
                            if (officerCode.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Please enter Officer Code!") }
                                return@Button
                            }
                            if (password.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Please enter PIN!") }
                                return@Button
                            }
                            onLoginSuccess(officerCode, "Senior Field Credit Officer")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_login_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "సురక్షితంగా ప్రవేశించండి" else "Sign In to Saptadhan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Biometric Authentication
                    OutlinedButton(
                        onClick = {
                            onLoginSuccess(officerCode, "Senior Field Credit Officer")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_login_biometric"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = BankPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "వేలిముద్రతో త్వరిత లాగిన్" else "Biometric Quick Sign In",
                            fontSize = 13.sp,
                            color = BankPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Demo Credentials Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = BankPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick Demo Auto-Fill (Wanaparthy Staff):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                officerCode = "FCO-WNP-08"
                                password = "1234"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_autofill_fco"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("K. Srinivas (FCO)", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                officerCode = "BM-WNP-01"
                                password = "5678"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_autofill_bm"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("S. Rajesh (BM)", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Regulatory Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Saptadhan Finance Private Limited",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Brand: Saptadhan • Web: www.saptadhan.com",
                    fontSize = 11.sp,
                    color = BankPrimary
                )
                Text(
                    text = "RBI Regd. NBFC-MFI • Wanaparthy District Hub (TG-WNP-01)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Telangana Microfinance Field Operations • Fortnightly JLG Collections",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
