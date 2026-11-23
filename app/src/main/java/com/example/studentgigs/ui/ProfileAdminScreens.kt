@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.studentgigs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.studentgigs.AppViewModel
import com.example.studentgigs.data.*
import java.util.Locale

@Composable
fun ProfileScreen(vm: AppViewModel, modifier: Modifier) {
    val u = vm.user ?: return
    var name by remember(u) { mutableStateOf(u.name) }
    var bio by remember(u) { mutableStateOf(u.bio) }
    var skills by remember(u) { mutableStateOf(u.skills) }
    var phone by remember(u) { mutableStateOf(u.phone) }
    var nameErr by remember { mutableStateOf<String?>(null) }
    var phoneErr by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(84.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Text(u.name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        Text(u.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AssistChip(onClick = {}, label = { Text(u.role.lowercase().replaceFirstChar { it.uppercase() }) })

        if (u.role == Role.STUDENT) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Rating", if (u.ratingCount == 0) "New" else String.format(Locale.US, "%.1f ★", u.rating), Modifier.weight(1f))
                StatCard("Completed jobs", u.completedJobs.toString(), Modifier.weight(1f))
            }
        }

        AppField(name, { name = it; nameErr = null }, "Full name", error = nameErr, leading = Icons.Default.Person, tag = "profile_name")
        AppField(phone, { phone = it; phoneErr = null }, "Phone (optional)", error = phoneErr,
            keyboard = KeyboardType.Phone, leading = Icons.Default.Phone, tag = "profile_phone")
        if (u.role == Role.STUDENT) {
            AppField(skills, { skills = it }, "Skills (e.g. Typing, Photoshop)", leading = Icons.Default.Build, tag = "profile_skills")
            AppField(bio, { bio = it }, "Short bio", singleLine = false, minLines = 3, tag = "profile_bio")
        }

        Button(
            onClick = {
                nameErr = Validators.name(name); phoneErr = Validators.phone(phone)
                if (nameErr == null && phoneErr == null)
                    vm.saveProfile(u.copy(name = name.trim(), phone = phone.trim(), skills = skills.trim(), bio = bio.trim()))
            },
            enabled = !vm.loading,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_profile")
        ) { Text("Save profile", fontWeight = FontWeight.Bold) }

        OutlinedButton(
            onClick = { confirmLogout = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("logout_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out")
        }
    }

    if (confirmLogout) {
        ConfirmDialog("Log out?", "You will need to log in again.", "Log out",
            onConfirm = { vm.logout() }, onDismiss = { confirmLogout = false })
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

/** Admin: suspend or reinstate users. */
@Composable
fun AdminUsersScreen(vm: AppViewModel, modifier: Modifier) {
    val others = vm.allUsers.filter { it.role != Role.ADMIN }
    var target by remember { mutableStateOf<User?>(null) }

    if (others.isEmpty()) {
        EmptyState("No users yet", "Registered students and customers appear here.", modifier)
    } else LazyColumn(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(others, key = { it.uid }) { u ->
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.testTag("user_card")) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.name, fontWeight = FontWeight.Bold)
                        Text("${u.email} · ${u.role.lowercase()}", style = MaterialTheme.typography.bodySmall)
                        if (u.suspended) Text("Suspended", color = Red, style = MaterialTheme.typography.labelMedium)
                    }
                    if (u.suspended) {
                        FilledTonalButton(onClick = { vm.setSuspended(u.uid, false) }) { Text("Reinstate") }
                    } else {
                        OutlinedButton(
                            onClick = { target = u },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("suspend_button")
                        ) { Icon(Icons.Default.Block, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Suspend") }
                    }
                }
            }
        }
    }
    target?.let { u ->
        ConfirmDialog("Suspend ${u.name}?", "They will no longer be able to log in.", "Suspend",
            onConfirm = { vm.setSuspended(u.uid, true) }, onDismiss = { target = null })
    }
}
