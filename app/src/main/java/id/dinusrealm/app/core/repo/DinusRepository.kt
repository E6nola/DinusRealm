package id.dinusrealm.app.core.repo

import android.content.Context
import id.dinusrealm.app.core.crypto.KeystoreVault
import id.dinusrealm.app.core.db.DinusDbHelper
import id.dinusrealm.app.core.engine.ClassScheduler
import id.dinusrealm.app.core.engine.NotificationHelper
import id.dinusrealm.app.core.engine.ScheduleTimeUtil
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.*
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.network.SiadinApi
import id.dinusrealm.app.core.network.SiadinParsers
import id.dinusrealm.app.widget.DinusScheduleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline-first repository backed by SQLite + AndroidKeyStore vault.
 * Emits cached data immediately on launch, then synchronizes silently in the background.
 */
class DinusRepository(
    private val appContext: Context,
    val vault: KeystoreVault,
    val db: DinusDbHelper,
    val api: SiadinApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isLoggedIn = MutableStateFlow(vault.hasCredentials())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _sessionExpiredNotice = MutableStateFlow(false)
    val sessionExpiredNotice: StateFlow<Boolean> = _sessionExpiredNotice.asStateFlow()

    private val _settings = MutableStateFlow(vault.getSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _miniProfile = MutableStateFlow<StudentMiniProfile?>(null)
    val miniProfile: StateFlow<StudentMiniProfile?> = _miniProfile.asStateFlow()

    private val _krsSummary = MutableStateFlow(KrsSummary())
    val krsSummary: StateFlow<KrsSummary> = _krsSummary.asStateFlow()

    private val _khsHeader = MutableStateFlow(KhsSummary())
    val khsHeader: StateFlow<KhsSummary> = _khsHeader.asStateFlow()

    private val _todaySlots = MutableStateFlow<List<ScheduleSlot>>(emptyList())
    val todaySlots: StateFlow<List<ScheduleSlot>> = _todaySlots.asStateFlow()

    private val _weeklySlots = MutableStateFlow<List<ScheduleSlot>>(emptyList())
    val weeklySlots: StateFlow<List<ScheduleSlot>> = _weeklySlots.asStateFlow()

    private val _onlineAttendance = MutableStateFlow<List<OnlineAttendanceItem>>(emptyList())
    val onlineAttendance: StateFlow<List<OnlineAttendanceItem>> = _onlineAttendance.asStateFlow()

    private val _billingItems = MutableStateFlow<List<BillingItem>>(emptyList())
    val billingItems: StateFlow<List<BillingItem>> = _billingItems.asStateFlow()

    private val _paymentMethods = MutableStateFlow<List<PaymentBankMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentBankMethod>> = _paymentMethods.asStateFlow()

    private val _attendanceLogs = MutableStateFlow<List<AttendanceLogEntry>>(emptyList())
    val attendanceLogs: StateFlow<List<AttendanceLogEntry>> = _attendanceLogs.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        loadFromCache()
    }

    fun onSessionInvalidated() {
        _sessionExpiredNotice.value = true
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun showBanner(msg: String) {
        _bannerMessage.value = msg
    }

    fun loadFromCache() {
        scope.launch {
            val cachedMini = db.getKv(KV_MINI_PROFILE)?.let { raw ->
                runCatching { SiadinParsers.parseMiniProfile(JSONObject(raw)) }.getOrNull()
            }
            if (cachedMini != null) _miniProfile.value = cachedMini

            db.getKv(KV_HEADER_KRS)?.let { raw ->
                runCatching { SiadinParsers.parseKrsSummary(JSONObject(raw)) }.getOrNull()?.let {
                    _krsSummary.value = it
                }
            }
            db.getKv(KV_HEADER_KHS)?.let { raw ->
                runCatching { SiadinParsers.parseKhsHeader(JSONObject(raw)) }.getOrNull()?.let {
                    _khsHeader.value = it
                }
            }

            val allSlots = db.getScheduleSlots()
            val weekly = allSlots.filter { it.dayIndex > 0 }
            val resolvedToday = ScheduleTimeUtil.resolveTodaySlots(
                allSlots = allSlots,
                savedTodayDateKey = db.getKv(KV_TODAY_SLOTS_DATE)
            )

            _weeklySlots.value = weekly
            _todaySlots.value = resolvedToday
            _billingItems.value = db.getBillingSnapshots()
            db.getKv(KV_PAYMENT_INFO)?.let { raw ->
                runCatching { SiadinParsers.parsePaymentInfo(JSONObject(raw)) }.getOrNull()?.let {
                    _paymentMethods.value = it.methods
                }
            }
            _attendanceLogs.value = db.getRecentAttendanceLogs()
        }
    }

    suspend fun login(nim: String, password: String): ApiResult<String> {
        val res = api.login(nim, password)
        if (res is ApiResult.Success) {
            _isLoggedIn.value = true
            _sessionExpiredNotice.value = false
            NotificationHelper.ensureChannels(appContext)
            ClassScheduler.schedulePeriodicJobs(appContext)
            refreshCoreData()
        }
        return res
    }

    fun logout() {
        scope.launch {
            runCatching { api.logout() }
            ClassScheduler.cancelAll(appContext)
            vault.clearSession()
            _settings.value = vault.getSettings()
            db.clearAllUserCache()
            _isLoggedIn.value = false
            _sessionExpiredNotice.value = false
            _miniProfile.value = null
            _krsSummary.value = KrsSummary()
            _khsHeader.value = KhsSummary()
            _todaySlots.value = emptyList()
            _weeklySlots.value = emptyList()
            _onlineAttendance.value = emptyList()
            _billingItems.value = emptyList()
            _paymentMethods.value = emptyList()
            _attendanceLogs.value = emptyList()
            DinusScheduleWidget.updateAll(appContext)
        }
    }

    fun refreshCoreDataAsync() {
        scope.launch { refreshCoreData() }
    }

    suspend fun refreshCoreData() {
        if (!vault.hasCredentials()) return
        _isRefreshing.value = true
        try {
            coroutineScope {
                val existing = db.getScheduleSlots()

                // Wave 1: lightweight profile + summary headers + schedules so UI unlocks fast
                val miniJob = async { api.getMiniProfile() }
                val krsHdrJob = async { api.getKrsHeader() }
                val khsHdrJob = async { api.getKhsHeader() }
                val onlineJob = async { api.getKrsOnline() }
                val todayJob = async { api.getTodaySchedule() }
                val weeklyJob = async { api.getWeeklySchedule() }

                val miniRes = miniJob.await()
                if (miniRes is ApiResult.Success) {
                    _miniProfile.value = miniRes.data
                    val json = JSONObject()
                        .put("nim", miniRes.data.nim)
                        .put("nama", miniRes.data.name)
                        .put("email_mhs", miniRes.data.emailMhs)
                        .put("gender", miniRes.data.gender)
                        .put("foto", miniRes.data.photoUrl)
                    db.putKv(KV_MINI_PROFILE, json.toString())
                }

                (krsHdrJob.await() as? ApiResult.Success)?.let {
                    _krsSummary.value = it.data
                    db.putKv(
                        KV_HEADER_KRS,
                        JSONObject()
                            .put("ta", it.data.academicYear)
                            .put("ganjilgenap", it.data.semester)
                            .put("jumlah_matkul", it.data.courseCount)
                            .put("total_sks", it.data.currentSks)
                            .put("ipk", it.data.ipk)
                            .toString()
                    )
                }

                (khsHdrJob.await() as? ApiResult.Success)?.let {
                    _khsHeader.value = it.data
                    db.putKv(
                        KV_HEADER_KHS,
                        JSONObject()
                            .put("total_sks", it.data.totalSks)
                            .put("ipk", it.data.ipk)
                            .toString()
                    )
                }

                val onlineRes = onlineJob.await()
                val onlineList = (onlineRes as? ApiResult.Success)?.data.orEmpty()
                if (onlineRes is ApiResult.Success) {
                    _onlineAttendance.value = onlineList
                }

                val todayRes = todayJob.await()
                val weeklyRes = weeklyJob.await()
                val todayDay = ScheduleTimeUtil.currentSiadinDay()

                fun mergeTodayWithOnline(rawToday: List<ScheduleSlot>): List<ScheduleSlot> =
                    rawToday.map { slot ->
                        val onlineMatch = onlineList.firstOrNull {
                            (slot.kdmk.isNotBlank() && it.kdmk == slot.kdmk) || it.idMaster == slot.idMaster
                        }
                        slot.copy(
                            dayIndex = 0,
                            idMaster = onlineMatch?.idMaster?.ifBlank { null } ?: slot.idMaster,
                            kdds = slot.kdds.ifBlank { onlineMatch?.kdds.orEmpty() },
                            isAttended = slot.isAttended || (onlineMatch?.isAttended == true),
                            isOpenClass = slot.isOpenClass || (onlineMatch?.isOpenClass == true)
                        )
                    }

                val weeklyWave1 = if (weeklyRes is ApiResult.Success) {
                    SiadinParsers.preserveSlotMetadata(weeklyRes.data.filter { it.dayIndex > 0 }, existing)
                } else {
                    existing.filter { it.dayIndex > 0 }
                }
                val savedTodayDate = db.getKv(KV_TODAY_SLOTS_DATE)
                val todayFromCache = if (savedTodayDate == ScheduleTimeUtil.todayDateKey()) {
                    existing.filter { it.dayIndex == 0 }
                } else emptyList()
                val todayRawWave1 = if (todayRes is ApiResult.Success) {
                    mergeTodayWithOnline(SiadinParsers.preserveSlotMetadata(todayRes.data, existing))
                } else {
                    todayFromCache
                }
                val resolvedTodayWave1 = if (todayRawWave1.isNotEmpty()) {
                    todayRawWave1
                } else {
                    weeklyWave1.filter { it.dayIndex == todayDay }
                }
                _weeklySlots.value = weeklyWave1
                _todaySlots.value = resolvedTodayWave1

                // Wave 2: heavier KHS/KRS enrichment + billing & 5 Bank VAs
                val khsNowJob = async { api.getKhsCurrent() }
                val krsNowJob = async { api.getKrsCurrent() }
                val payInfoJob = async { api.getPaymentInfo() }
                val regBillJob = async { api.getRegularBilling() }
                val nonRegBillJob = async { api.getNonRegularBilling() }

                val freshRefs = buildList {
                    (khsNowJob.await() as? ApiResult.Success)?.data?.second?.forEach { c ->
                        add(
                            ScheduleSlot(
                                id = "", kdmk = c.kdmk, klpk = c.klpk, courseName = c.courseName,
                                dayIndex = 0, dayName = "", startTime = "", endTime = "", room = "",
                                lecturer = c.lecturer
                            )
                        )
                    }
                    (krsNowJob.await() as? ApiResult.Success)?.data?.second?.forEach { c ->
                        add(
                            ScheduleSlot(
                                id = "", kdmk = c.kdmk, klpk = c.klpk, courseName = c.courseName,
                                dayIndex = 0, dayName = "", startTime = "", endTime = "", room = "",
                                lecturer = c.lecturer
                            )
                        )
                    }
                }
                val metadataRef = freshRefs + existing

                val weekly = if (weeklyRes is ApiResult.Success) {
                    SiadinParsers.preserveSlotMetadata(weeklyRes.data.filter { it.dayIndex > 0 }, metadataRef)
                } else {
                    SiadinParsers.preserveSlotMetadata(weeklyWave1, freshRefs)
                }
                val todayRaw = if (todayRes is ApiResult.Success) {
                    mergeTodayWithOnline(SiadinParsers.preserveSlotMetadata(todayRes.data, metadataRef))
                } else {
                    SiadinParsers.preserveSlotMetadata(todayRawWave1, freshRefs)
                }
                val resolvedToday = if (todayRaw.isNotEmpty()) todayRaw else weekly.filter { it.dayIndex == todayDay }

                _weeklySlots.value = weekly
                _todaySlots.value = resolvedToday

                if (todayRes is ApiResult.Success || weeklyRes is ApiResult.Success) {
                    db.replaceScheduleSlots(todayRaw + weekly)
                    db.putKv(KV_TODAY_SLOTS_DATE, if (todayRaw.isNotEmpty()) ScheduleTimeUtil.todayDateKey() else "")
                    ClassScheduler.rescheduleTodayTimers(appContext, resolvedToday, _settings.value)
                    DinusScheduleWidget.updateAll(appContext)
                }

                val payBundle = (payInfoJob.await() as? ApiResult.Success)?.data
                val methods = payBundle?.methods.orEmpty()
                if (methods.isNotEmpty()) {
                    _paymentMethods.value = methods
                }
                if (payBundle != null && (methods.isNotEmpty() || payBundle.notesHtml.isNotBlank())) {
                    val metodeArr = JSONArray()
                    methods.forEach { m ->
                        metodeArr.put(
                            JSONObject()
                                .put("nama_bank", m.bankName)
                                .put("va", m.vaNumber)
                                .put("atas_nama", m.accountName)
                                .put("auto_validasi", m.validationNote)
                                .put("tutorial", m.tutorialUrl)
                        )
                    }
                    db.putKv(
                        KV_PAYMENT_INFO,
                        JSONObject()
                            .put("metode_bayar", metodeArr)
                            .put("catatan", payBundle.notesHtml)
                            .toString()
                    )
                }

                val regBill = regBillJob.await()
                val nonRegBill = nonRegBillJob.await()
                if (regBill is ApiResult.Success || nonRegBill is ApiResult.Success) {
                    val bca = methods.firstOrNull { it.bankName.contains("BCA", true) }?.vaNumber.orEmpty()
                    val bjb = methods.firstOrNull { it.bankName.contains("Jateng", true) }?.vaNumber.orEmpty()
                    val mandiri = methods.firstOrNull { it.bankName.contains("Mandiri", true) }?.vaNumber.orEmpty()

                    val mergedBills = buildList {
                        if (regBill is ApiResult.Success) addAll(regBill.data)
                        if (nonRegBill is ApiResult.Success) addAll(nonRegBill.data)
                    }.map {
                        it.copy(
                            vaBca = it.vaBca.ifBlank { bca },
                            vaBjb = it.vaBjb.ifBlank { bjb },
                            vaMandiri = it.vaMandiri.ifBlank { mandiri }
                        )
                    }
                    _billingItems.value = mergedBills
                    db.replaceBillingSnapshots(mergedBills)
                }

                _attendanceLogs.value = db.getRecentAttendanceLogs()
            }
        } finally {
            _isRefreshing.value = false
        }
    }

    suspend fun submitAttendance(idMaster: String, kdds: String, courseName: String): ApiResult<String> {
        val res = api.submitOnlineAttendance(idMaster, kdds)
        val ok = res is ApiResult.Success
        val msg = when (res) {
            is ApiResult.Success -> res.data
            is ApiResult.Error -> res.message
        }
        db.insertAttendanceLog(
            AttendanceLogEntry(
                idMaster = idMaster,
                courseName = courseName,
                mode = "MANUAL_APP",
                success = ok,
                message = msg,
                timestampMillis = System.currentTimeMillis()
            )
        )
        _attendanceLogs.value = db.getRecentAttendanceLogs()
        _bannerMessage.value = if (ok) {
            tr(_settings.value.language, "Attendance for $courseName recorded!", "Presensi $courseName tercatat!")
        } else msg
        if (ok) {
            refreshCoreData()
        }
        return res
    }

    suspend fun confirmRps(masterId: String, sesuai: Boolean): ApiResult<String> {
        val res = api.confirmRps(masterId, sesuai)
        _bannerMessage.value = when (res) {
            is ApiResult.Success -> res.data
            is ApiResult.Error -> res.message
        }
        if (res is ApiResult.Success) refreshCoreData()
        return res
    }

    fun updateSettings(newSettings: AppSettings) {
        val langChanged = _settings.value.language != newSettings.language
        vault.updateSettings(newSettings)
        _settings.value = newSettings
        if (langChanged) {
            NotificationHelper.ensureChannels(appContext)
            DinusScheduleWidget.updateAll(appContext)
        }
        if (_isLoggedIn.value) {
            ClassScheduler.rescheduleTodayTimers(appContext, _todaySlots.value, newSettings)
        }
    }

    companion object {
        const val KV_TODAY_SLOTS_DATE = "today_slots_date"
        private const val KV_MINI_PROFILE = "mini_profile"
        private const val KV_HEADER_KRS = "header_krs"
        private const val KV_HEADER_KHS = "header_khs"
        private const val KV_PAYMENT_INFO = "payment_info"
    }
}
