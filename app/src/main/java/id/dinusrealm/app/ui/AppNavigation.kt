package id.dinusrealm.app.ui

enum class BottomTab(val label: String) {
    HOME("Beranda"),
    SCHEDULE("Jadwal"),
    ACADEMIC("Akademik"),
    MORE("Lainnya")
}

sealed interface SubScreen {
    data object Presence : SubScreen
    data object Krs : SubScreen
    data object Khs : SubScreen
    data object Exams : SubScreen
    data object Billing : SubScreen
    data object Heregistrasi : SubScreen
    data object Questionnaire : SubScreen
    data object Announcements : SubScreen
    data object Profile : SubScreen
    data object Settings : SubScreen
    data object AttendanceLogs : SubScreen
}
