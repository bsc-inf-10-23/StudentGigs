@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.studentgigs.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.studentgigs.AppViewModel
import com.example.studentgigs.data.*

/** Opens the gig location in Google Maps (falls back to the browser version). */
fun openInMaps(context: Context, place: String) {
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(place)}"))
    val web = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(place)}")
    )
    try { context.startActivity(geo) } catch (e: Exception) { runCatching { context.startActivity(web) } }
}

// ---------------------------------------------------------------- HOME
@Composable
fun HomeScreen(vm: AppViewModel, modifier: Modifier, onGig: (String) -> Unit) {
    var toRemove by remember { mutableStateOf<Gig?>(null) }
    val isAdmin = vm.user?.role == Role.ADMIN
    val list = vm.visibleGigs

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Hi, ${vm.user?.name?.substringBefore(' ').orEmpty()}",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
        )
        Text("Find affordable student services", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        AppField(vm.query, { vm.query = it }, "Search services", leading = Icons.Default.Search, tag = "search_field")
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("All") + Categories.all) { c ->
                FilterChip(selected = vm.category == c, onClick = { vm.category = c }, label = { Text(c) })
            }
        }
        if (list.isEmpty()) {
            EmptyState("No services found", "Try a different search or category.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(list, key = { it.gigId }) { g ->
                    GigCard(g, onClick = { onGig(g.gigId) }) {
                        if (isAdmin) IconButton(onClick = { toRemove = g }, modifier = Modifier.testTag("admin_remove")) {
                            Icon(Icons.Default.Delete, "Remove gig", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    toRemove?.let { g ->
        ConfirmDialog("Remove gig?", "\"${g.title}\" will be removed for everyone.", "Remove",
            onConfirm = { vm.deleteGig(g.gigId) }, onDismiss = { toRemove = null })
    }
}

// ---------------------------------------------------------------- MY GIGS (student)
@Composable
fun MyGigsScreen(vm: AppViewModel, modifier: Modifier, onGig: (String) -> Unit, onEdit: (String) -> Unit) {
    var toDelete by remember { mutableStateOf<Gig?>(null) }
    if (vm.myGigs.isEmpty()) {
        EmptyState("No gigs yet", "Tap + to advertise your first service.", modifier)
    } else {
        LazyColumn(
            modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(vm.myGigs, key = { it.gigId }) { g ->
                GigCard(g, onClick = { onGig(g.gigId) }) {
                    IconButton(onClick = { onEdit(g.gigId) }, modifier = Modifier.testTag("edit_gig")) {
                        Icon(Icons.Default.Edit, "Edit gig", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { toDelete = g }, modifier = Modifier.testTag("delete_gig")) {
                        Icon(Icons.Default.Delete, "Delete gig", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    toDelete?.let { g ->
        ConfirmDialog("Delete gig?", "\"${g.title}\" will be permanently deleted.", "Delete",
            onConfirm = { vm.deleteGig(g.gigId) }, onDismiss = { toDelete = null })
    }
}

// ---------------------------------------------------------------- GIG DETAILS
@Composable
fun GigDetailsScreen(vm: AppViewModel, gigId: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val gig = vm.findGig(gigId)
    val me = vm.user
    val context = LocalContext.current
    var confirmRemove by remember { mutableStateOf(false) }

    LaunchedEffect(gigId, gig?.studentId) {
        vm.loadReviews(gigId)
        gig?.studentId?.let { vm.loadStudent(it) }
    }

    Scaffold(topBar = { BackTopBar("Gig details", onBack) }) { pad ->
        if (gig == null || me == null) {
            EmptyState("Gig not found", "It may have been removed.", Modifier.padding(pad))
        } else Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(gig.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(gig.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(money(gig.price), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            Text("About this service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(gig.description)

            if (gig.location.isNotBlank()) {
                OutlinedButton(
                    onClick = { openInMaps(context, gig.location) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("open_map")
                ) {
                    Icon(Icons.Default.LocationOn, null)
                    Spacer(Modifier.width(8.dp))
                    Text("View on Google Maps · ${gig.location}")
                }
            }

            // Student information
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Offered by", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val s = vm.viewedStudent
                    Text(s?.name ?: gig.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (s != null) {
                        RatingBadge(s.rating, s.ratingCount)
                        Text("${s.completedJobs} completed jobs", style = MaterialTheme.typography.bodySmall)
                        if (s.skills.isNotBlank()) Text("Skills: ${s.skills}", style = MaterialTheme.typography.bodySmall)
                        if (s.bio.isNotBlank()) Text(s.bio, style = MaterialTheme.typography.bodySmall)
                        if (s.phone.isNotBlank()) Text("Phone: ${s.phone}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Role-specific actions
            when (me.role) {
                Role.CUSTOMER -> RequestForm(onSend = { note -> vm.requestService(gig, note, onDone = onBack) })
                Role.STUDENT -> if (gig.studentId == me.uid) {
                    Button(onClick = { onEdit(gig.gigId) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp)); Text("Edit this gig")
                    }
                }
                Role.ADMIN -> Button(
                    onClick = { confirmRemove = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("admin_remove_details")
                ) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Remove inappropriate gig") }
            }

            // Reviews
            Text("Reviews (${vm.reviews.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (vm.reviews.isEmpty()) {
                Text("No reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else vm.reviews.forEach { r ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Stars(r.rating, size = 16.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(r.customerName, style = MaterialTheme.typography.labelLarge)
                        }
                        if (r.review.isNotBlank()) Text(r.review, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (confirmRemove && gig != null) {
        ConfirmDialog("Remove gig?", "\"${gig.title}\" will be removed for everyone.", "Remove",
            onConfirm = { vm.deleteGig(gig.gigId); onBack() }, onDismiss = { confirmRemove = false })
    }
}

@Composable
private fun RequestForm(onSend: (String) -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Request this service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AppField(note, { note = it; err = null }, "Short note for the student", error = err,
                singleLine = false, minLines = 3, tag = "note_field")
            Button(
                onClick = { err = Validators.note(note); if (err == null) onSend(note) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("send_request")
            ) { Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text("Send request") }
        }
    }
}

// ---------------------------------------------------------------- CREATE / EDIT GIG
@Composable
fun GigFormScreen(vm: AppViewModel, gigId: String?, onDone: () -> Unit) {
    val existing = gigId?.let { vm.findGig(it) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: Categories.all.first()) }
    var description by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var price by rememberSaveable {
        mutableStateOf(existing?.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }
    var active by rememberSaveable { mutableStateOf(existing?.isActive ?: true) }
    var tErr by remember { mutableStateOf<String?>(null) }
    var dErr by remember { mutableStateOf<String?>(null) }
    var pErr by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar(if (existing == null) "Create gig" else "Edit gig", onDone) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppField(title, { title = it; tErr = null }, "Gig title (e.g. Poster Design)", error = tErr, tag = "gig_title")

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = category, onValueChange = {}, readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("gig_category")
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Categories.all.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { category = c; expanded = false })
                    }
                }
            }

            AppField(description, { description = it; dErr = null }, "Description", error = dErr,
                singleLine = false, minLines = 4, tag = "gig_description")
            AppField(price, { price = it; pErr = null }, "Price (MK)", error = pErr,
                keyboard = KeyboardType.Decimal, tag = "gig_price")
            AppField(location, { location = it }, "Location (e.g. Chichiri Campus, Blantyre)",
                leading = Icons.Default.LocationOn, tag = "gig_location")

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Available for requests", fontWeight = FontWeight.SemiBold)
                    Text("Turn off to hide this gig from customers", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = active, onCheckedChange = { active = it })
            }

            Button(
                onClick = {
                    tErr = Validators.title(title); dErr = Validators.description(description); pErr = Validators.price(price)
                    if (tErr == null && dErr == null && pErr == null) {
                        vm.saveGig(
                            (existing ?: Gig()).copy(
                                title = title.trim(), category = category, description = description.trim(),
                                price = price.trim().toDouble(), location = location.trim(), isActive = active
                            ), onDone
                        )
                    }
                },
                enabled = !vm.loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_gig")
            ) { Text("Save gig", fontWeight = FontWeight.Bold) }
        }
    }
}
