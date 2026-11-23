package id.dinusrealm.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import id.dinusrealm.app.DinusRealmApp
import id.dinusrealm.app.MainActivity
import id.dinusrealm.app.R
import id.dinusrealm.app.core.engine.ScheduleTimeUtil
import id.dinusrealm.app.core.i18n.currentLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.ScheduleSlot
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.network.SiadinParsers
import id.dinusrealm.app.core.repo.DinusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Zero-bloat Home Screen Widget built on native AppWidgetProvider + RemoteViews.
 * Reads directly from SQLite cache so parking-lot schedule checks load instantaneously offline.
 */
class DinusScheduleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val container = (context.applicationContext as DinusRealmApp).container
                    if (container.vault.hasCredentials()) {
                        val todayRes = container.api.getTodaySchedule()
                        if (todayRes is ApiResult.Success) {
                            val existing = container.db.getScheduleSlots()
                            val existingWeekly = existing.filter { it.dayIndex > 0 }
                            val preservedToday = SiadinParsers.preserveSlotMetadata(
                                todayRes.data.map { it.copy(dayIndex = 0) },
                                existing
                            )
                            container.db.replaceScheduleSlots(preservedToday + existingWeekly)
                            container.db.putKv(
                                DinusRepository.KV_TODAY_SLOTS_DATE,
                                if (preservedToday.isNotEmpty()) ScheduleTimeUtil.todayDateKey() else ""
                            )
                        }
                    }
                    updateAll(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val ACTION_WIDGET_REFRESH = "id.dinusrealm.app.WIDGET_REFRESH"

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, DinusScheduleWidget::class.java))
            for (id in ids) {
                updateWidget(context, mgr, id)
            }
        }

        private fun updateWidget(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val app = context.applicationContext as? DinusRealmApp
            val todaySlots = if (app != null) {
                ScheduleTimeUtil.resolveTodaySlots(
                    allSlots = app.container.db.getScheduleSlots(),
                    savedTodayDateKey = app.container.db.getKv(DinusRepository.KV_TODAY_SLOTS_DATE)
                )
            } else emptyList()

            val lang = currentLanguage(context)
            val (nowMin, _) = ScheduleTimeUtil.currentMinutesAndSecond()
            val hero = ScheduleTimeUtil.findHeroSlot(todaySlots, nowMin)
            val views = RemoteViews(context.packageName, R.layout.widget_schedule)

            if (hero != null) {
                val statusTag = when {
                    hero.isAttended -> tr(lang, " · Present", " · Hadir")
                    hero.dayIndex == 0 && hero.isOpenClass && !hero.idMaster.isNullOrBlank() ->
                        tr(lang, " · Attend", " · Bisa Absen")
                    else -> ""
                }
                views.setTextViewText(
                    R.id.widget_header,
                    tr(
                        lang,
                        "Today's Classes (${todaySlots.size})$statusTag",
                        "Kelas Hari Ini (${todaySlots.size})$statusTag"
                    )
                )
                views.setTextViewText(R.id.widget_room_pill, hero.room.ifBlank { tr(lang, "Room -", "Ruang -") })
                views.setTextViewText(R.id.widget_hero_title, hero.courseName)
                val lecturerPart = if (hero.lecturer.isNotBlank()) " · ${hero.lecturer}" else ""
                views.setTextViewText(R.id.widget_hero_meta, "${hero.startTime}–${hero.endTime}$lecturerPart")

                val others = todaySlots.filter { it.id != hero.id }.take(2)
                if (others.isNotEmpty()) {
                    val nextText = others.joinToString("\n") {
                        "${it.startTime} · ${it.room.ifBlank { "-" }} · ${it.courseName}"
                    }
                    views.setTextViewText(R.id.widget_next_list, nextText)
                    views.setViewVisibility(R.id.widget_next_list, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_next_list, View.GONE)
                }
            } else if (todaySlots.isNotEmpty()) {
                views.setTextViewText(
                    R.id.widget_header,
                    tr(lang, "Classes Done Today", "Kuliah Hari Ini Selesai")
                )
                views.setTextViewText(
                    R.id.widget_room_pill,
                    tr(lang, "${todaySlots.size} Classes", "${todaySlots.size} MK")
                )
                views.setTextViewText(
                    R.id.widget_hero_title,
                    tr(lang, "All classes for today have ended", "Semua kelas hari ini telah berakhir")
                )
                views.setTextViewText(
                    R.id.widget_hero_meta,
                    tr(lang, "Tap to check tomorrow's schedule", "Ketuk untuk cek jadwal besok")
                )
                views.setViewVisibility(R.id.widget_next_list, View.GONE)
            } else {
                val loggedIn = app?.container?.vault?.hasCredentials() == true
                views.setTextViewText(R.id.widget_header, tr(lang, "DinusRealm · Today", "DinusRealm · Hari Ini"))
                views.setTextViewText(R.id.widget_room_pill, "Free")
                views.setTextViewText(
                    R.id.widget_hero_title,
                    if (loggedIn) {
                        tr(lang, "No classes scheduled today", "Tidak ada jadwal kuliah hari ini")
                    } else {
                        tr(lang, "Not signed in", "Belum login")
                    }
                )
                views.setTextViewText(
                    R.id.widget_hero_meta,
                    if (loggedIn) {
                        tr(lang, "Tap to view weekly schedule", "Ketuk untuk lihat jadwal mingguan")
                    } else {
                        tr(lang, "Open DinusRealm to sync", "Buka DinusRealm untuk sinkronisasi")
                    }
                )
                views.setViewVisibility(R.id.widget_next_list, View.GONE)
            }

            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            val refreshIntent = Intent(context, DinusScheduleWidget::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                appWidgetId + 100,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

            mgr.updateAppWidget(appWidgetId, views)
        }
    }
}
