package id.dinusrealm.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.BottomTab
import id.dinusrealm.app.ui.SubScreen
import id.dinusrealm.app.ui.screens.AcademicHubScreen
import id.dinusrealm.app.ui.screens.AnnouncementsScreen
import id.dinusrealm.app.ui.screens.AttendanceLogsScreen
import id.dinusrealm.app.ui.screens.BillingScreen
import id.dinusrealm.app.ui.screens.ExamsScreen
import id.dinusrealm.app.ui.screens.HeregistrasiScreen
import id.dinusrealm.app.ui.screens.HomeScreen
import id.dinusrealm.app.ui.screens.KhsScreen
import id.dinusrealm.app.ui.screens.KrsScreen
import id.dinusrealm.app.ui.screens.LoginScreen
import id.dinusrealm.app.ui.screens.MoreHubScreen
import id.dinusrealm.app.ui.screens.PresenceScreen
import id.dinusrealm.app.ui.screens.ProfileScreen
import id.dinusrealm.app.ui.screens.QuestionnaireScreen
import id.dinusrealm.app.ui.screens.ScheduleScreen
import id.dinusrealm.app.ui.screens.SettingsScreen
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusRealmTheme
import id.dinusrealm.app.ui.theme.DinusTheme

class MainActivity : ComponentActivity() {

    private val pendingSubScreen = mutableStateOf<SubScreen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)
        val repo = (application as DinusRealmApp).container.repo

        setContent {
            val settings by repo.settings.collectAsState()
            val isLoggedIn by repo.isLoggedIn.collectAsState()
            val sessionExpired by repo.sessionExpiredNotice.collectAsState()
            val pending by pendingSubScreen

            DinusRealmTheme(
                themeMode = settings.themeMode,
                language = settings.language
            ) {
                if (!isLoggedIn) {
                    LoginScreen(repo = repo, sessionExpiredNotice = sessionExpired)
                } else {
                    MainAppShell(
                        repo = repo,
                        pendingScreen = pending,
                        onConsumePendingScreen = { pendingSubScreen.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_OPEN_SCREEN)) {
            SCREEN_PRESENCE -> pendingSubScreen.value = SubScreen.Presence
        }
    }

    companion object {
        const val EXTRA_OPEN_SCREEN = "extra_open_screen"
        const val SCREEN_PRESENCE = "presence"
    }
}

@Composable
private fun MainAppShell(
    repo: DinusRepository,
    pendingScreen: SubScreen? = null,
    onConsumePendingScreen: () -> Unit = {}
) {
    val p = DinusTheme.palette
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    val backStack = remember { mutableStateListOf<SubScreen>() }

    LaunchedEffect(Unit) {
        repo.refreshCoreData()
    }

    LaunchedEffect(pendingScreen) {
        if (pendingScreen != null) {
            backStack.clear()
            backStack.add(pendingScreen)
            onConsumePendingScreen()
        }
    }

    BackHandler(enabled = backStack.isNotEmpty() || currentTab != BottomTab.HOME) {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            currentTab = BottomTab.HOME
        }
    }

    val currentSub = backStack.lastOrNull()

    Scaffold(
        containerColor = p.background,
        bottomBar = {
            if (currentSub == null) {
                SorbetBottomBar(
                    currentTab = currentTab,
                    onSelectTab = { currentTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentSub != null) {
                val pop: () -> Unit = {
                    if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
                }
                when (currentSub) {
                    SubScreen.Settings -> SettingsScreen(
                        repo = repo,
                        onBack = pop,
                        onNavigateSub = { backStack.add(it) }
                    )
                    SubScreen.Presence -> PresenceScreen(repo = repo, onBack = pop)
                    SubScreen.Krs -> KrsScreen(repo = repo, onBack = pop)
                    SubScreen.Khs -> KhsScreen(repo = repo, onBack = pop)
                    SubScreen.Exams -> ExamsScreen(repo = repo, onBack = pop)
                    SubScreen.Billing -> BillingScreen(repo = repo, onBack = pop)
                    SubScreen.Heregistrasi -> HeregistrasiScreen(repo = repo, onBack = pop)
                    SubScreen.Questionnaire -> QuestionnaireScreen(repo = repo, onBack = pop)
                    SubScreen.Announcements -> AnnouncementsScreen(repo = repo, onBack = pop)
                    SubScreen.Profile -> ProfileScreen(repo = repo, onBack = pop)
                    SubScreen.AttendanceLogs -> AttendanceLogsScreen(repo = repo, onBack = pop)
                }
            } else {
                Crossfade(targetState = currentTab, label = "tab") { tab ->
                    when (tab) {
                        BottomTab.HOME -> HomeScreen(
                            repo = repo,
                            onNavigateSub = { backStack.add(it) },
                            onOpenScheduleTab = { currentTab = BottomTab.SCHEDULE }
                        )
                        BottomTab.SCHEDULE -> ScheduleScreen(
                            repo = repo,
                            onNavigateSub = { backStack.add(it) }
                        )
                        BottomTab.ACADEMIC -> AcademicHubScreen(
                            onNavigateSub = { backStack.add(it) }
                        )
                        BottomTab.MORE -> MoreHubScreen(
                            onNavigateSub = { backStack.add(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SorbetBottomBar(
    currentTab: BottomTab,
    onSelectTab: (BottomTab) -> Unit
) {
    val p = DinusTheme.palette
    Surface(
        color = p.background,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = p.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    val icon = when (tab) {
                        BottomTab.HOME -> DinusIcons.Home
                        BottomTab.SCHEDULE -> DinusIcons.Calendar
                        BottomTab.ACADEMIC -> DinusIcons.Academic
                        BottomTab.MORE -> DinusIcons.Wallet
                    }
                    val tabLabel = when (tab) {
                        BottomTab.HOME -> tr("Home", "Beranda")
                        BottomTab.SCHEDULE -> tr("Schedule", "Jadwal")
                        BottomTab.ACADEMIC -> tr("Academic", "Akademik")
                        BottomTab.MORE -> tr("More", "Lainnya")
                    }
                    Surface(
                        onClick = { onSelectTab(tab) },
                        color = Color.Transparent,
                        shape = CircleShape
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tabLabel,
                                tint = if (selected) p.ink else p.inkMuted,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) p.ink else p.inkMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) p.lime else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}
