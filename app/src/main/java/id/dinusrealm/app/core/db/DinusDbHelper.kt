package id.dinusrealm.app.core.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import id.dinusrealm.app.core.model.AttendanceLogEntry
import id.dinusrealm.app.core.model.BillingItem
import id.dinusrealm.app.core.model.ScheduleSlot

/**
 * Native SQLiteOpenHelper cache (zero Room / KSP / annotation processor overhead).
 * Stores schedule slots, key-value JSON cache, billing snapshots, and attendance action logs.
 */
class DinusDbHelper(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE kv_cache (
                cache_key TEXT PRIMARY KEY,
                payload TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE schedule_slots (
                id TEXT PRIMARY KEY,
                kdmk TEXT NOT NULL,
                klpk TEXT NOT NULL,
                course_name TEXT NOT NULL,
                sks INTEGER NOT NULL,
                day_index INTEGER NOT NULL,
                day_name TEXT NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT NOT NULL,
                room TEXT NOT NULL,
                lecturer TEXT NOT NULL,
                id_master TEXT,
                kdds TEXT NOT NULL,
                is_open_class INTEGER NOT NULL,
                is_attended INTEGER NOT NULL,
                meeting_number INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE billing_snapshots (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                total_amount INTEGER NOT NULL,
                paid_amount INTEGER NOT NULL,
                remaining_amount INTEGER NOT NULL,
                is_paid INTEGER NOT NULL,
                status_text TEXT NOT NULL,
                due_date TEXT NOT NULL,
                va_bca TEXT NOT NULL,
                va_bjb TEXT NOT NULL,
                va_mandiri TEXT NOT NULL,
                snapshot_hash TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE attendance_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_master TEXT NOT NULL,
                course_name TEXT NOT NULL,
                mode TEXT NOT NULL,
                success INTEGER NOT NULL,
                message TEXT NOT NULL,
                timestamp_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS kv_cache")
        db.execSQL("DROP TABLE IF EXISTS schedule_slots")
        db.execSQL("DROP TABLE IF EXISTS billing_snapshots")
        db.execSQL("DROP TABLE IF EXISTS attendance_logs")
        onCreate(db)
    }

    fun putKv(key: String, payload: String) {
        val cv = ContentValues().apply {
            put("cache_key", key)
            put("payload", payload)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("kv_cache", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getKv(key: String): String? {
        readableDatabase.rawQuery("SELECT payload FROM kv_cache WHERE cache_key = ?", arrayOf(key)).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    fun replaceScheduleSlots(slots: List<ScheduleSlot>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("schedule_slots", null, null)
            for (s in slots) {
                val cv = ContentValues().apply {
                    put("id", s.id)
                    put("kdmk", s.kdmk)
                    put("klpk", s.klpk)
                    put("course_name", s.courseName)
                    put("sks", s.sks)
                    put("day_index", s.dayIndex)
                    put("day_name", s.dayName)
                    put("start_time", s.startTime)
                    put("end_time", s.endTime)
                    put("room", s.room)
                    put("lecturer", s.lecturer)
                    put("id_master", s.idMaster)
                    put("kdds", s.kdds)
                    put("is_open_class", if (s.isOpenClass) 1 else 0)
                    put("is_attended", if (s.isAttended) 1 else 0)
                    put("meeting_number", s.meetingNumber)
                }
                db.insertWithOnConflict("schedule_slots", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getScheduleSlots(dayIndex: Int? = null): List<ScheduleSlot> {
        val sql = if (dayIndex != null) {
            "SELECT * FROM schedule_slots WHERE day_index = ? ORDER BY start_time ASC"
        } else {
            "SELECT * FROM schedule_slots ORDER BY day_index ASC, start_time ASC"
        }
        val args = if (dayIndex != null) arrayOf(dayIndex.toString()) else null
        val list = mutableListOf<ScheduleSlot>()
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                list += ScheduleSlot(
                    id = c.getString(c.getColumnIndexOrThrow("id")),
                    kdmk = c.getString(c.getColumnIndexOrThrow("kdmk")),
                    klpk = c.getString(c.getColumnIndexOrThrow("klpk")),
                    courseName = c.getString(c.getColumnIndexOrThrow("course_name")),
                    sks = c.getInt(c.getColumnIndexOrThrow("sks")),
                    dayIndex = c.getInt(c.getColumnIndexOrThrow("day_index")),
                    dayName = c.getString(c.getColumnIndexOrThrow("day_name")),
                    startTime = c.getString(c.getColumnIndexOrThrow("start_time")),
                    endTime = c.getString(c.getColumnIndexOrThrow("end_time")),
                    room = c.getString(c.getColumnIndexOrThrow("room")),
                    lecturer = c.getString(c.getColumnIndexOrThrow("lecturer")),
                    idMaster = c.getString(c.getColumnIndexOrThrow("id_master")),
                    kdds = c.getString(c.getColumnIndexOrThrow("kdds")),
                    isOpenClass = c.getInt(c.getColumnIndexOrThrow("is_open_class")) != 0,
                    isAttended = c.getInt(c.getColumnIndexOrThrow("is_attended")) != 0,
                    meetingNumber = c.getInt(c.getColumnIndexOrThrow("meeting_number"))
                )
            }
        }
        return list
    }

    fun getBillingSnapshots(): List<BillingItem> {
        val list = mutableListOf<BillingItem>()
        readableDatabase.rawQuery("SELECT * FROM billing_snapshots", null).use { c ->
            while (c.moveToNext()) {
                list += BillingItem(
                    id = c.getString(c.getColumnIndexOrThrow("id")),
                    title = c.getString(c.getColumnIndexOrThrow("title")),
                    category = c.getString(c.getColumnIndexOrThrow("category")),
                    totalAmount = c.getLong(c.getColumnIndexOrThrow("total_amount")),
                    paidAmount = c.getLong(c.getColumnIndexOrThrow("paid_amount")),
                    remainingAmount = c.getLong(c.getColumnIndexOrThrow("remaining_amount")),
                    isPaid = c.getInt(c.getColumnIndexOrThrow("is_paid")) != 0,
                    statusText = c.getString(c.getColumnIndexOrThrow("status_text")),
                    dueDate = c.getString(c.getColumnIndexOrThrow("due_date")),
                    vaBca = c.getString(c.getColumnIndexOrThrow("va_bca")),
                    vaBjb = c.getString(c.getColumnIndexOrThrow("va_bjb")),
                    vaMandiri = c.getString(c.getColumnIndexOrThrow("va_mandiri")),
                    rawSnapshotHash = c.getString(c.getColumnIndexOrThrow("snapshot_hash"))
                )
            }
        }
        return list
    }

    fun replaceBillingSnapshots(items: List<BillingItem>) {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            db.delete("billing_snapshots", null, null)
            for (b in items) {
                val cv = ContentValues().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("category", b.category)
                    put("total_amount", b.totalAmount)
                    put("paid_amount", b.paidAmount)
                    put("remaining_amount", b.remainingAmount)
                    put("is_paid", if (b.isPaid) 1 else 0)
                    put("status_text", b.statusText)
                    put("due_date", b.dueDate)
                    put("va_bca", b.vaBca)
                    put("va_bjb", b.vaBjb)
                    put("va_mandiri", b.vaMandiri)
                    put("snapshot_hash", b.rawSnapshotHash)
                    put("updated_at", now)
                }
                db.insertWithOnConflict("billing_snapshots", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertAttendanceLog(entry: AttendanceLogEntry) {
        val cv = ContentValues().apply {
            put("id_master", entry.idMaster)
            put("course_name", entry.courseName)
            put("mode", entry.mode)
            put("success", if (entry.success) 1 else 0)
            put("message", entry.message)
            put("timestamp_ms", entry.timestampMillis)
        }
        writableDatabase.insert("attendance_logs", null, cv)
    }

    fun getRecentAttendanceLogs(limit: Int = 30): List<AttendanceLogEntry> {
        val list = mutableListOf<AttendanceLogEntry>()
        readableDatabase.rawQuery(
            "SELECT * FROM attendance_logs ORDER BY timestamp_ms DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list += AttendanceLogEntry(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    idMaster = c.getString(c.getColumnIndexOrThrow("id_master")),
                    courseName = c.getString(c.getColumnIndexOrThrow("course_name")),
                    mode = c.getString(c.getColumnIndexOrThrow("mode")),
                    success = c.getInt(c.getColumnIndexOrThrow("success")) != 0,
                    message = c.getString(c.getColumnIndexOrThrow("message")),
                    timestampMillis = c.getLong(c.getColumnIndexOrThrow("timestamp_ms"))
                )
            }
        }
        return list
    }

    fun clearAllUserCache() {
        writableDatabase.apply {
            delete("kv_cache", null, null)
            delete("schedule_slots", null, null)
            delete("billing_snapshots", null, null)
            delete("attendance_logs", null, null)
        }
    }

    companion object {
        private const val DB_NAME = "dinusrealm_cache.db"
        private const val DB_VERSION = 1
    }
}
