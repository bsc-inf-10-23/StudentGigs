@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.studentgigs.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.studentgigs.data.Role
import com.example.studentgigs.data.Validators

@Composable
fun AppLogoBadge(modifier: Modifier = Modifier, size: Dp = 110.dp) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // Outer glowing ring with subtle border
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(2.dp, Gold.copy(alpha = 0.6f)),
            modifier = Modifier.size(size)
        ) {}
        // Inner badge container with vibrant gradient background
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .size(size - 16.dp)
                .background(Brush.radialGradient(listOf(Gold, TealDark)), CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size((size.value * 0.42).dp)
                        .offset(y = (-8).dp)
                )
                Icon(
                    Icons.Default.Work,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier
                        .size((size.value * 0.32).dp)
                        .offset(y = 10.dp)
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TealDark, Teal, TealDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogoBadge(size = 110.dp)
            Spacer(Modifier.height(20.dp))
            Text("Student Gigs", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Gold, strokeWidth = 3.dp)
        }
    }
}

@Composable
fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TealDark, Teal, TealDark)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))
            AppLogoBadge(size = 120.dp)
            Spacer(Modifier.height(20.dp))

            Surface(
                color = Gold.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f))
            ) {
                Text(
                    "CAMPUS SERVICES MARKETPLACE",
                    color = Gold,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Student Gigs",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect skilled campus students with local customers for fast, affordable services.",
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(28.dp))

            // Feature highlight pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeaturePill("Verified Students", Icons.Default.Verified)
                FeaturePill("Quick Requests", Icons.Default.FlashOn)
                FeaturePill("Ratings", Icons.Default.Star)
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = onRegister,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_register")
            ) {
                Icon(Icons.Default.PersonAdd, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create an account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLogin,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.5.dp, Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_login")
            ) {
                Icon(Icons.Default.Login, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("I already have an account", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeaturePill(label: String, icon: ImageVector) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun LoginScreen(loading: Boolean, onSubmit: (String, String) -> Unit, onRegister: () -> Unit, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var pwErr by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BackTopBar("Login", onBack) }) { pad ->
        Column(
            Modifier.padding(pad).padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Log in to continue", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppField(email, { email = it; emailErr = null }, "Email", error = emailErr,
                keyboard = KeyboardType.Email, leading = Icons.Default.Email, tag = "login_email")
            AppField(password, { password = it; pwErr = null }, "Password", error = pwErr,
                isPassword = true, leading = Icons.Default.Lock, tag = "login_password")
            Button(
                onClick = {
                    emailErr = Validators.email(email)
                    pwErr = Validators.password(password)
                    if (emailErr == null && pwErr == null) onSubmit(email.trim(), password)
                },
                enabled = !loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("login_button")
            ) { Text(if (loading) "Please wait…" else "Login", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("New here? Create an account")
            }
        }
    }
}

@Composable
fun RegisterScreen(loading: Boolean, onSubmit: (String, String, String, String) -> Unit, onLogin: () -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(Role.STUDENT) }
    var nameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var pwErr by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BackTopBar("Create account", onBack) }) { pad ->
        Column(
            Modifier.padding(pad).padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("I want to…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = role == Role.STUDENT, onClick = { role = Role.STUDENT },
                    label = { Text("Offer my skills") },
                    leadingIcon = { Icon(Icons.Default.School, null, Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("role_student")
                )
                FilterChip(
                    selected = role == Role.CUSTOMER, onClick = { role = Role.CUSTOMER },
                    label = { Text("Find a service") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("role_customer")
                )
            }
            AppField(name, { name = it; nameErr = null }, "Full name", error = nameErr,
                leading = Icons.Default.Person, tag = "reg_name")
            AppField(email, { email = it; emailErr = null }, "Email", error = emailErr,
                keyboard = KeyboardType.Email, leading = Icons.Default.Email, tag = "reg_email")
            AppField(password, { password = it; pwErr = null }, "Password (min 6 characters)", error = pwErr,
                isPassword = true, leading = Icons.Default.Lock, tag = "reg_password")
            Button(
                onClick = {
                    nameErr = Validators.name(name)
                    emailErr = Validators.email(email)
                    pwErr = Validators.password(password)
                    if (nameErr == null && emailErr == null && pwErr == null)
                        onSubmit(name.trim(), email.trim(), password, role)
                },
                enabled = !loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("register_button")
            ) { Text(if (loading) "Please wait…" else "Register", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Already registered? Login")
            }
        }
    }
}
