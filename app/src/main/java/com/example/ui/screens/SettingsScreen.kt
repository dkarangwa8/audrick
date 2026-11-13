package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.model.DistributorSettings
import com.example.data.model.OtpResetRecord
import com.example.data.model.StaffProfile
import com.example.data.model.StaffRole
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.BralirwaCrimson
import com.example.ui.theme.EmeraldFresh
import com.example.ui.theme.EmeraldSoft
import com.example.ui.viewmodel.DistributorViewModel
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    viewModel: DistributorViewModel,
    settings: DistributorSettings,
    staffList: List<StaffProfile>,
    latestOtp: OtpResetRecord?,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val activeRole = StaffRole.fromCode(settings.activeStaffRole)

    var bizName by remember(settings) { mutableStateOf(settings.businessName) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var email by remember(settings) { mutableStateOf(settings.email) }
    var tin by remember(settings) { mutableStateOf(settings.tin) }
    var bralirwaEmail by remember(settings) { mutableStateOf(settings.bralirwaEmail) }
    var vatRate by remember(settings) { mutableStateOf(settings.vatRate.toString()) }
    var lowStockEmail by remember(settings) { mutableStateOf(settings.lowStockAutoEmail) }
    var recurringBilling by remember(settings) { mutableStateOf(settings.recurringBillingEnabled) }
    var paperMm by remember(settings) { mutableStateOf(settings.thermalPaperMm) }

    var pinTargetStaff by remember { mutableStateOf<StaffProfile?>(null) }
    var showOtpResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ROLE-AWARE STAFF SHIFT SWITCHER
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Badge, contentDescription = null, tint = BralirwaCrimson)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Role-Aware Staff Shift", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Active: ${settings.activeStaffName} (${activeRole.label})", style = MaterialTheme.typography.bodySmall, color = EmeraldFresh, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    staffList.forEach { st ->
                        val isCurrent = st.role == settings.activeStaffRole && st.fullName == settings.activeStaffName
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pinTargetStaff = st }
                                .testTag("switch_staff_${st.role}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) EmeraldSoft else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${st.fullName} • ${st.role}", fontWeight = FontWeight.Bold)
                                    Text("${st.email} (Default PIN: ${st.pinHash})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(if (isCurrent) "ACTIVE" else "Switch", color = if (isCurrent) EmeraldFresh else BralirwaCrimson, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showOtpResetDialog = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("forgot_pin_otp_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Forgot Staff PIN? Reset via 6-Digit Email OTP")
                    }
                }
            }
        }

        // 2. BRANDED A4 INVOICE & AUTOMATION SETTINGS
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = BralirwaCrimson)
                        Spacer(Modifier.width(8.dp))
                        Text("Branded A4 Invoice & Automation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (!activeRole.canAccessSettings()) {
                        Surface(color = AmberAlertSoft, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                "Read-only for ${activeRole.label}. Switch to Admin role to save business settings.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Business / Depot Name") }, enabled = activeRole.canAccessSettings(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Depot Address") }, enabled = activeRole.canAccessSettings(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Business Phone") }, enabled = activeRole.canAccessSettings(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Business Email") }, enabled = activeRole.canAccessSettings(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tin, onValueChange = { tin = it }, label = { Text("RRA TIN Number") }, enabled = activeRole.canAccessSettings(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bralirwaEmail, onValueChange = { bralirwaEmail = it }, label = { Text("Bralirwa Order Email") }, enabled = activeRole.canAccessSettings(), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = vatRate, onValueChange = { vatRate = it }, label = { Text("VAT %") }, enabled = activeRole.canAccessSettings(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Thermal Receipt:", style = MaterialTheme.typography.bodyMedium)
                        FilterChip(selected = paperMm == 80, onClick = { if (activeRole.canAccessSettings()) paperMm = 80 }, label = { Text("80mm") })
                        FilterChip(selected = paperMm == 58, onClick = { if (activeRole.canAccessSettings()) paperMm = 58 }, label = { Text("58mm") })
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto Low-Stock Email Alerts", modifier = Modifier.weight(1f))
                        Switch(checked = lowStockEmail, onCheckedChange = { if (activeRole.canAccessSettings()) lowStockEmail = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Recurring Customer Billing Alerts", modifier = Modifier.weight(1f))
                        Switch(checked = recurringBilling, onCheckedChange = { if (activeRole.canAccessSettings()) recurringBilling = it })
                    }

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                settings.copy(
                                    businessName = bizName,
                                    address = address,
                                    phone = phone,
                                    email = email,
                                    tin = tin,
                                    bralirwaEmail = bralirwaEmail,
                                    vatRate = vatRate.toDoubleOrNull() ?: 18.0,
                                    lowStockAutoEmail = lowStockEmail,
                                    recurringBillingEnabled = recurringBilling,
                                    thermalPaperMm = paperMm
                                )
                            )
                        },
                        enabled = activeRole.canAccessSettings(),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_save_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Branding & Automation")
                    }
                }
            }
        }

        // 3. SIGN OUT
        item {
            OutlinedButton(
                onClick = {
                    signOutDistributor(credentialManager, scope, onSignedOut)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = BralirwaCrimson)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out Google Account", color = BralirwaCrimson, fontWeight = FontWeight.Bold)
            }
        }
    }

    pinTargetStaff?.let { target ->
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pinTargetStaff = null },
            title = { Text("Enter PIN for ${target.fullName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Role: ${target.role} (Hint: ${target.pinHash})", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("Staff PIN / Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.switchStaffRole(target, pinInput)
                        pinTargetStaff = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
                ) { Text("Verify & Switch") }
            },
            dismissButton = { TextButton(onClick = { pinTargetStaff = null }) { Text("Cancel") } }
        )
    }

    if (showOtpResetDialog) {
        OtpResetDialog(
            staffEmailDefault = staffList.firstOrNull()?.email ?: settings.email,
            latestOtp = latestOtp,
            onRequestOtp = { targetEmail -> viewModel.requestOtpReset(context, targetEmail) },
            onConfirmReset = { targetEmail, otpCode, newPin ->
                viewModel.verifyOtpAndResetPin(targetEmail, otpCode, newPin)
                showOtpResetDialog = false
            },
            onDismiss = { showOtpResetDialog = false }
        )
    }
}

@Composable
private fun OtpResetDialog(
    staffEmailDefault: String,
    latestOtp: OtpResetRecord?,
    onRequestOtp: (String) -> Unit,
    onConfirmReset: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(staffEmailDefault) }
    var otpCode by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var secondsLeft by remember(latestOtp) { mutableLongStateOf(600L) }

    LaunchedEffect(latestOtp) {
        if (latestOtp != null) {
            while (true) {
                val rem = ((latestOtp.expiresAtMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                secondsLeft = rem
                if (rem <= 0L) break
                delay(1000L)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("6-Digit OTP PIN Reset (10m Expiry)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Staff Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onRequestOtp(email) },
                    modifier = Modifier.fillMaxWidth().testTag("send_otp_email_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BralirwaCrimson)
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Send 6-Digit OTP via Email")
                }
                if (latestOtp != null) {
                    val mins = secondsLeft / 60
                    val secs = secondsLeft % 60
                    Surface(color = EmeraldSoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "OTP Code: ${latestOtp.otpCode} • Expires in ${String.format("%02d:%02d", mins, secs)}",
                            color = EmeraldFresh,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Enter 6-Digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("otp_code_input")
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New Staff PIN / Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReset(email, otpCode, newPin) },
                enabled = otpCode.length == 6 && newPin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldFresh),
                modifier = Modifier.testTag("verify_otp_reset_btn")
            ) { Text("Reset PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
