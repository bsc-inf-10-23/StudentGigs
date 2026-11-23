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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studentgigs.AppViewModel
import com.example.studentgigs.data.*

// ---------------------------------------------------------------- MY JOBS
@Composable
fun JobsScreen(vm: AppViewModel, modifier: Modifier, onJob: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("Pending", "Active", "Completed")
    val isStudent = vm.user?.role == Role.STUDENT
    val list = vm.jobs.filter { JobWorkflow.bucket(it.status) == tab }

    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { i, t ->
                Tab(
                    selected = tab == i, onClick = { tab = i },
                    text = { Text("$t (${vm.jobs.count { JobWorkflow.bucket(it.status) == i }})") },
                    modifier = Modifier.testTag("jobs_tab_$i")
                )
            }
        }
        if (list.isEmpty()) {
            EmptyState(
                "Nothing here",
                if (isStudent) "Requests from customers will appear here." else "Services you request will appear here."
            )
        } else LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(list, key = { it.jobId }) { j ->
                Card(
                    onClick = { onJob(j.jobId) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("job_card")
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(j.gigTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            StatusChip(j.status)
                        }
                        Text(
                            if (isStudent) "Customer: ${j.customerName}" else "Student: ${j.studentName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(money(j.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- JOB DETAILS
@Composable
fun JobDetailsScreen(vm: AppViewModel, jobId: String, onBack: () -> Unit) {
    val job = vm.findJob(jobId)
    val role = vm.user?.role.orEmpty()

    Scaffold(topBar = { BackTopBar("Job details", onBack) }) { pad ->
        if (job == null) {
            EmptyState("Job not found", "", Modifier.padding(pad))
        } else Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(job.gigTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                StatusChip(job.status)
            }
            Text(money(job.price), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold)

            ProgressTrack(job.status)

            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Student: ${job.studentName}")
                    Text("Customer: ${job.customerName}")
                    Spacer(Modifier.height(6.dp))
                    Text("Request note", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(job.note)
                }
            }

            JobWorkflow.actionsFor(job.status, role).forEach { a ->
                val decline = a == JobAction.DECLINE
                Button(
                    onClick = { vm.changeStatus(job, a) },
                    enabled = !vm.loading,
                    colors = if (decline) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else ButtonDefaults.buttonColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("action_${a.name}")
                ) { Text(a.label, fontWeight = FontWeight.Bold) }
            }

            if (JobWorkflow.canReview(job, role)) {
                ReviewForm { rating, text -> vm.submitReview(job, rating, text) }
            } else if (job.rating > 0) {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Customer review", style = MaterialTheme.typography.labelLarge)
                        Stars(job.rating)
                        if (job.review.isNotBlank()) Text(job.review)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressTrack(status: String) {
    if (status == JobStatus.DECLINED) {
        Text("This request was declined.", color = Red, fontWeight = FontWeight.SemiBold)
        return
    }
    val steps = listOf(JobStatus.PENDING, JobStatus.ACCEPTED, JobStatus.COMPLETED, JobStatus.CONFIRMED)
    val labels = listOf("Requested", "Accepted", "Done", "Confirmed")
    val idx = steps.indexOf(status)
    Row(Modifier.fillMaxWidth()) {
        steps.indices.forEach { i ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(16.dp).background(
                        if (i <= idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
                )
                Text(labels[i], style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ReviewForm(onSubmit: (Int, String) -> Unit) {
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var text by rememberSaveable { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("How was the service?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Stars(rating, size = 36.dp, onSelect = { rating = it; err = null })
            err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            AppField(text, { text = it }, "Write a review (optional)", singleLine = false, minLines = 3, tag = "review_text")
            Button(
                onClick = { err = Validators.rating(rating); if (err == null) onSubmit(rating, text) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("submit_review")
            ) { Text("Submit review") }
        }
    }
}
