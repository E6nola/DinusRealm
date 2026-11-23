package id.dinusrealm.app.core.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import id.dinusrealm.app.DinusRealmApp
import id.dinusrealm.app.core.model.AppLanguage

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun tr(en: String, id: String): String =
    if (LocalAppLanguage.current == AppLanguage.INDONESIAN) id else en

fun tr(lang: AppLanguage, en: String, id: String): String =
    if (lang == AppLanguage.INDONESIAN) id else en

fun dayName(dayIndex: Int, lang: AppLanguage): String = when (dayIndex) {
    1 -> tr(lang, "Monday", "Senin")
    2 -> tr(lang, "Tuesday", "Selasa")
    3 -> tr(lang, "Wednesday", "Rabu")
    4 -> tr(lang, "Thursday", "Kamis")
    5 -> tr(lang, "Friday", "Jumat")
    6 -> tr(lang, "Saturday", "Sabtu")
    7 -> tr(lang, "Sunday", "Minggu")
    else -> tr(lang, "Today", "Hari Ini")
}

fun shortDayName(dayIndex: Int, lang: AppLanguage): String = when (dayIndex) {
    1 -> tr(lang, "Mon", "Sen")
    2 -> tr(lang, "Tue", "Sel")
    3 -> tr(lang, "Wed", "Rab")
    4 -> tr(lang, "Thu", "Kam")
    5 -> tr(lang, "Fri", "Jum")
    6 -> tr(lang, "Sat", "Sab")
    7 -> tr(lang, "Sun", "Min")
    else -> tr(lang, "Today", "Hari Ini")
}

fun currentLanguage(context: Context): AppLanguage =
    (context.applicationContext as? DinusRealmApp)?.container?.vault?.getSettings()?.language
        ?: AppLanguage.ENGLISH
