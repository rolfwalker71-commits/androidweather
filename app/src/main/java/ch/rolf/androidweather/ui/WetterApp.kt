package ch.rolf.androidweather.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.rolf.androidweather.ui.components.AppHeader
import ch.rolf.androidweather.ui.components.MehrMenu
import ch.rolf.androidweather.ui.screens.EinstellungenScreen
import ch.rolf.androidweather.ui.screens.FavoritenScreen
import ch.rolf.androidweather.ui.screens.JetztScreen
import ch.rolf.androidweather.ui.screens.LuftScreen
import ch.rolf.androidweather.ui.screens.MehrScreen
import ch.rolf.androidweather.ui.screens.PendelnScreen
import ch.rolf.androidweather.ui.screens.RadarScreen
import ch.rolf.androidweather.ui.screens.TopicScreens
import ch.rolf.androidweather.ui.screens.VergleichScreen
import ch.rolf.androidweather.ui.screens.VerlaufScreen
import ch.rolf.androidweather.ui.screens.WocheScreen
import ch.rolf.androidweather.ui.theme.WetterTheme
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("jetzt", "Jetzt", Icons.Outlined.WbSunny),
    Tab("verlauf", "Verlauf", Icons.Outlined.Schedule),
    Tab("woche", "Woche", Icons.Outlined.CalendarMonth),
    Tab("luft", "Luft", Icons.Outlined.Air),
    Tab("mehr", "Mehr", Icons.Outlined.MoreHoriz)
)

private val mehrRoutes = setOf(
    "mehr", "radar", "favoriten", "vergleich", "pendeln",
    "einstellungen", "wind", "berge", "seen", "draussen"
)

@Composable
fun WetterApp(vm: WetterViewModel = viewModel()) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val ui by vm.state.collectAsStateWithLifecycle()
    WetterTheme(theme) {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val current = backStack?.destination
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        fun openMehr(route: String) {
            scope.launch { drawerState.close() }
            nav.navigate(route)
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        Text("Mehr", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                        MehrMenu(onOpen = ::openMehr)
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        AppHeader(
                            ui = ui,
                            onMenu = { scope.launch { drawerState.open() } },
                            onLocate = vm::locate,
                            onRefresh = { vm.refresh() },
                            onSearch = vm::search,
                            onSelectPlace = vm::selectPlace
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEach { tab ->
                            val selected = current?.hierarchy?.any { dest ->
                                dest.route == tab.route || (tab.route == "mehr" && dest.route in mehrRoutes && dest.route !in tabs.map { it.route })
                            } == true || (tab.route == "mehr" && current?.route in mehrRoutes && current?.route != "jetzt" && current?.route != "verlauf" && current?.route != "woche" && current?.route != "luft")
                            TabItem(tab, selected) {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = nav,
                    startDestination = "jetzt",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    composable("jetzt") { JetztScreen(vm) }
                    composable("verlauf") { VerlaufScreen(vm) }
                    composable("woche") { WocheScreen(vm) }
                    composable("luft") { LuftScreen(vm) }
                    composable("mehr") { MehrScreen(onOpen = { nav.navigate(it) }) }
                    composable("radar") { RadarScreen(vm) }
                    composable("favoriten") {
                        FavoritenScreen(vm) { place ->
                            vm.selectPlace(place)
                            nav.navigate("jetzt") {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    composable("vergleich") { VergleichScreen(vm) }
                    composable("pendeln") { PendelnScreen(vm) }
                    composable("einstellungen") { EinstellungenScreen(vm) }
                    composable("wind") { TopicScreens.Wind(vm) }
                    composable("berge") { TopicScreens.Berge(vm) }
                    composable("seen") { TopicScreens.Seen(vm) }
                    composable("draussen") { TopicScreens.Draussen(vm) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.height(64.dp),
        icon = {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        label = {
            Text(
                tab.label,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
    )
}
