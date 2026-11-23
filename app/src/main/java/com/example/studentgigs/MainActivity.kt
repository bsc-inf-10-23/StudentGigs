@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.studentgigs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studentgigs.data.RepositoryProvider
import com.example.studentgigs.data.Role
import com.example.studentgigs.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudentGigsTheme { StudentGigsApp() }
        }
    }
}

@Composable
fun StudentGigsApp() {
    val vm: AppViewModel = viewModel(factory = viewModelFactory { initializer<AppViewModel> { AppViewModel(RepositoryProvider.get()) } })
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }

    // Show one-off messages (errors / confirmations)
    LaunchedEffect(vm.message) {
        vm.message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(nav, startDestination = "splash") {
            composable("splash") { SplashScreen() }
            composable("welcome") {
                WelcomeScreen(onLogin = { nav.navigate("login") }, onRegister = { nav.navigate("register") })
            }
            composable("login") {
                LoginScreen(
                    loading = vm.loading,
                    onSubmit = { e, p -> vm.login(e, p) },
                    onRegister = { nav.navigate("register") { popUpTo("welcome") } },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("register") {
                RegisterScreen(
                    loading = vm.loading,
                    onSubmit = { n, e, p, r -> vm.register(n, e, p, r) },
                    onLogin = { nav.navigate("login") { popUpTo("welcome") } },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("main") { MainShell(vm, nav) }
            composable("gig/{id}") { e ->
                GigDetailsScreen(vm, e.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() }, onEdit = { nav.navigate("gigForm/$it") })
            }
            composable("gigForm/{id}") { e ->
                GigFormScreen(vm, e.arguments?.getString("id")?.takeIf { it != "new" }, onDone = { nav.popBackStack() })
            }
            composable("job/{id}") { e ->
                JobDetailsScreen(vm, e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
        }

        if (vm.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter).statusBarsPadding())
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp)
        )
    }

    // Session handling: go to Home when logged in, back to Welcome when logged out
    LaunchedEffect(vm.sessionChecked, vm.user == null) {
        if (!vm.sessionChecked) return@LaunchedEffect
        val target = if (vm.user != null) "main" else "welcome"
        nav.navigate(target) { popUpTo(nav.graph.id) { inclusive = true } }
    }
}

private enum class MainTab(val title: String, val icon: ImageVector) {
    HOME("Discover", Icons.Default.Home),
    MY_GIGS("My Gigs", Icons.Default.Build),
    JOBS("My Jobs", Icons.Default.Work),
    USERS("Users", Icons.Default.People),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
private fun MainShell(vm: AppViewModel, nav: NavController) {
    val me = vm.user ?: return
    val tabs = when (me.role) {
        Role.STUDENT -> listOf(MainTab.HOME, MainTab.MY_GIGS, MainTab.JOBS, MainTab.PROFILE)
        Role.CUSTOMER -> listOf(MainTab.HOME, MainTab.JOBS, MainTab.PROFILE)
        else -> listOf(MainTab.HOME, MainTab.USERS, MainTab.PROFILE)
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tab = tabs[selected.coerceIn(0, tabs.lastIndex)]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.title) },
                actions = {
                    IconButton(onClick = { vm.refresh() }, modifier = Modifier.testTag("refresh")) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == t, onClick = { selected = i },
                        icon = { Icon(t.icon, t.title) }, label = { Text(t.title) },
                        modifier = Modifier.testTag("tab_${t.name}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == MainTab.MY_GIGS) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate("gigForm/new") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New gig") },
                    modifier = Modifier.testTag("fab_new_gig")
                )
            }
        }
    ) { pad ->
        val m = Modifier.padding(pad)
        when (tab) {
            MainTab.HOME -> HomeScreen(vm, m) { nav.navigate("gig/$it") }
            MainTab.MY_GIGS -> MyGigsScreen(vm, m, onGig = { nav.navigate("gig/$it") }, onEdit = { nav.navigate("gigForm/$it") })
            MainTab.JOBS -> JobsScreen(vm, m) { nav.navigate("job/$it") }
            MainTab.USERS -> AdminUsersScreen(vm, m)
            MainTab.PROFILE -> ProfileScreen(vm, m)
        }
    }
}
