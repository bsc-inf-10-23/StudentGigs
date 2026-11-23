@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.studentgigs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.studentgigs.data.Gig
import com.example.studentgigs.data.JobWorkflow
import java.util.Locale

fun money(p: Double): String =
    "MK " + if (p % 1.0 == 0.0) String.format(Locale.US, "%,.0f", p) else String.format(Locale.US, "%,.2f", p)

@Composable
fun AppField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leading: ImageVector? = null,
    tag: String = label
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier.testTag(tag),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        leadingIcon = leading?.let { { Icon(it, null) } },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Show or hide password"
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboard),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun StatusChip(status: String) {
    val c = statusColor(status)
    Surface(color = c.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
        Text(
            JobWorkflow.label(status), color = c,
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun Stars(rating: Int, modifier: Modifier = Modifier, size: Dp = 20.dp, onSelect: ((Int) -> Unit)? = null) {
    Row(modifier) {
        for (i in 1..5) {
            var m = Modifier.size(size).testTag("star_$i")
            if (onSelect != null) m = m.clickable { onSelect(i) }
            Icon(
                if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Star $i", tint = Gold, modifier = m
            )
        }
    }
}

@Composable
fun RatingBadge(rating: Double, count: Int) {
    Text(
        if (count == 0) "New" else String.format(Locale.US, "★ %.1f (%d)", rating, count),
        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Inbox, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotEmpty()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GigCard(gig: Gig, onClick: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().testTag("gig_card")
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                    Text(
                        gig.category, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                if (!gig.isActive) {
                    Spacer(Modifier.width(6.dp))
                    Text("Inactive", color = Red, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.weight(1f))
                Text(money(gig.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(gig.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                gig.description, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(gig.studentName, style = MaterialTheme.typography.bodySmall)
                if (gig.location.isNotBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(gig.location, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                actions()
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }, modifier = Modifier.testTag("confirm_yes")) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
