package id.dinusrealm.app.core.network

import id.dinusrealm.app.core.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Complete API facade matching all live DINUSVERSE / SiAdin endpoints.
 */
class SiadinApi(private val client: DinusHttpClient) {

    suspend fun login(nim: String, password: String): ApiResult<String> =
        client.login(nim, password)

    suspend fun logout(): ApiResult<Unit> =
        client.postSiadin("logout", JSONObject()).mapUnit()

    suspend fun getMaintenanceStatus(): ApiResult<MaintenanceStatus> {
        val maint = client.getSiadin("is-maintenance?os=android", requiresAuth = false)
        val ver = client.getSiadin("version-mobile", requiresAuth = false)
        val mObj = (maint as? ApiResult.Success)?.data?.optJSONObject("data") ?: (maint as? ApiResult.Success)?.data
        val vObj = (ver as? ApiResult.Success)?.data?.optJSONObject("data") ?: (ver as? ApiResult.Success)?.data
        return ApiResult.Success(
            MaintenanceStatus(
                isMaintenance = SiadinParsers.optBoolFlex(mObj, "is_maintenance"),
                message = SiadinParsers.optStr(mObj, "message", "msg"),
                latestVersion = SiadinParsers.optStr(vObj, "version_name", "version_number"),
                requiredUpdate = SiadinParsers.optBoolFlex(vObj, "required_update")
            )
        )
    }

    suspend fun getMiniProfile(): ApiResult<StudentMiniProfile> =
        client.getSiadin("get-mini-profile").mapSuccess { SiadinParsers.parseMiniProfile(it) }

    suspend fun getProfile(): ApiResult<StudentProfile> =
        client.getSiadin("profile").mapSuccess { SiadinParsers.parseFullProfile(it) }

    suspend fun updateProfile(fields: Map<String, String>): ApiResult<String> {
        val body = JSONObject()
        fields.forEach { (k, v) -> body.put(k, v) }
        return client.postSiadin("update-profile", body).mapSuccess {
            it.optString("message").ifBlank { "Profil berhasil diperbarui" }
        }
    }

    suspend fun changePassword(oldPw: String, newPw: String, confirmPw: String): ApiResult<String> {
        val body = JSONObject()
            .put("old_password", oldPw)
            .put("new_password", newPw)
            .put("retype_password", confirmPw)
        return client.postSiadin("change-password", body).mapSuccess {
            it.optString("message").ifBlank { "Kata sandi berhasil diubah" }
        }
    }

    suspend fun getTodaySchedule(): ApiResult<List<ScheduleSlot>> =
        client.getSiadin("get-jadwal-hari-ini").mapSuccess { SiadinParsers.parseTodaySchedule(it) }

    suspend fun getWeeklySchedule(): ApiResult<List<ScheduleSlot>> =
        client.getSiadin("get-jadwal").mapSuccess { SiadinParsers.parseWeeklySchedule(it) }

    suspend fun getKrsOnline(): ApiResult<List<OnlineAttendanceItem>> =
        client.getSiadin("get-krs-online").mapSuccess { SiadinParsers.parseKrsOnline(it) }

    suspend fun submitOnlineAttendance(idMaster: String, kdds: String, comment: String = ""): ApiResult<String> {
        val body = JSONObject()
            .put("comment", comment)
            .put("id_master", idMaster)
            .put("kdds", kdds)
        return client.postSiadin("absensi-online", body).mapSuccess {
            it.optString("message").ifBlank { "Presensi berhasil dicatat" }
        }
    }

    suspend fun confirmRps(masterId: String, sesuai: Boolean): ApiResult<String> {
        val body = JSONObject()
            .put("id_master", masterId)
            .put("flag_konfirmasi", 1)
            .put("flag_kesesuaian_rps", if (sesuai) 1 else 0)
        return client.postSiadin("konfirmasi-rps", body).mapSuccess {
            it.optString("message").ifBlank { "Konfirmasi RPS tersimpan" }
        }
    }

    suspend fun getAttendanceSummary(): ApiResult<List<AttendanceCourse>> =
        client.getSiadin("get-presensi").mapSuccess { SiadinParsers.parseAttendanceCourses(it) }

    suspend fun getAttendanceDetail(kdmk: String): ApiResult<List<AttendanceMeeting>> =
        client.getSiadin("get-detail-presensi?kdmk=$kdmk").mapSuccess { SiadinParsers.parseAttendanceDetail(it) }

    suspend fun getKrsHeader(): ApiResult<KrsSummary> =
        client.getSiadin("header-krs").mapSuccess { SiadinParsers.parseKrsSummary(it) }

    suspend fun getKrsCurrent(): ApiResult<Pair<KrsSummary, List<KrsCourse>>> =
        client.getSiadin("get-krs").mapSuccess {
            val d = it.optJSONObject("data") ?: it
            val sum = KrsSummary(
                academicYear = SiadinParsers.optStr(d, "ta"),
                isVerified = SiadinParsers.optBoolFlex(d, "is_verif")
            )
            sum to SiadinParsers.parseKrsCourses(it, sum.academicYear)
        }

    suspend fun getKrsHistoryGroups(): ApiResult<List<SemesterKrsGroup>> =
        client.getSiadin("get-krs-lalu").mapSuccess { SiadinParsers.parseKrsHistoryGroups(it) }

    suspend fun getKhsHeader(): ApiResult<KhsSummary> =
        client.getSiadin("header-khs").mapSuccess { SiadinParsers.parseKhsHeader(it) }

    suspend fun getKhsCurrent(): ApiResult<Pair<KhsSummary, List<KhsCourse>>> =
        client.getSiadin("get-khs").mapSuccess { SiadinParsers.parseKhsCurrent(it) }

    suspend fun getKhsHistoryGroups(): ApiResult<List<SemesterKhsGroup>> =
        client.getSiadin("get-khs-lalu").mapSuccess { SiadinParsers.parseKhsHistoryGroups(it) }

    suspend fun getIpChart(): ApiResult<List<IpChartPoint>> =
        client.getSiadin("get-grafik").mapSuccess { SiadinParsers.parseIpChart(it) }

    suspend fun getTranscript(): ApiResult<TranscriptSummary> =
        client.getSiadin("get-transkrip").mapSuccess { SiadinParsers.parseTranscript(it) }

    suspend fun getExamSchedule(type: String): ApiResult<ExamScheduleBundle> {
        val path = if (type.equals("UAS", ignoreCase = true)) "get-jadwal-uas" else "get-jadwal-uts"
        return client.getSiadin(path).mapSuccess { SiadinParsers.parseExamBundle(it, type.uppercase()) }
    }

    suspend fun getRegularBilling(): ApiResult<List<BillingItem>> =
        client.getSiadin("get-tagihan").mapSuccess { SiadinParsers.parseBilling(it, "Reguler") }

    suspend fun getNonRegularBilling(): ApiResult<List<BillingItem>> =
        client.getSiadin("get-tagihan-nonreg").mapSuccess { SiadinParsers.parseBilling(it, "Non-Reguler") }

    suspend fun getPaymentInfo(): ApiResult<PaymentInfoBundle> =
        client.getSiadin("get-info-pembayaranweb").mapSuccess { SiadinParsers.parsePaymentInfo(it) }

    suspend fun getHeregistrasi(): ApiResult<HeregistrasiInfo> {
        val dataRes = client.getSiadin("heregistrasi")
        val docsRes = client.getSiadin("get-berkas-pendukung")
        return dataRes.mapSuccess { root ->
            val outer = root.optJSONObject("data") ?: root
            val isShow = SiadinParsers.optBoolFlex(outer, "is_show")
            val d = outer.optJSONObject("data_heregistrasi") ?: outer
            val map = linkedMapOf<String, String>()
            val keys = d.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = d.opt(k)
                if (k != "is_show" && v != null && v !is JSONObject && v !is JSONArray && v.toString() != "null") {
                    map[k] = v.toString()
                }
            }
            val docs = mutableListOf<SupportingDoc>()
            if (docsRes is ApiResult.Success) {
                val docObj = docsRes.data.optJSONObject("data") ?: docsRes.data
                listOf(
                    "ijazah_depan" to "Ijazah Depan",
                    "ijazah_belakang" to "Ijazah Belakang",
                    "ktp" to "Kartu Tanda Penduduk (KTP)",
                    "kk" to "Kartu Keluarga (KK)"
                ).forEach { (key, label) ->
                    val url = SiadinParsers.optStr(docObj, key)
                    docs += SupportingDoc(key = key, label = label, uploaded = url.isNotBlank(), fileUrl = url)
                }
            }
            HeregistrasiInfo(
                isShow = isShow,
                academicStatus = SiadinParsers.optStr(d, "akdm_stat", "status"),
                wave = SiadinParsers.optStr(d, "gel_daf"),
                isSubmitted = SiadinParsers.optBoolFlex(d, "berkas_uploaded", "is_submitted"),
                fields = map,
                docs = docs
            )
        }
    }

    suspend fun submitHeregistrasi(fields: Map<String, String>): ApiResult<String> {
        val body = JSONObject()
        fields.forEach { (k, v) -> body.put(k, v) }
        return client.postSiadin("heregistrasi", body).mapSuccess {
            it.optString("message").ifBlank { "Data heregistrasi berhasil disimpan" }
        }
    }

    suspend fun getQuestionnaires(type: String): ApiResult<QuestionnaireOverview> {
        val path = if (type.equals("UAS", ignoreCase = true)) "get-kuisioner-uas" else "get-kuisioner-uts"
        return client.getSiadin(path).mapSuccess { root ->
            val d = root.optJSONObject("data") ?: root
            val arr = d.optJSONArray("detail") ?: d.optJSONArray("kuisioner") ?: JSONArray()
            val items = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val qArr = o.optJSONArray("list_pernyataan") ?: o.optJSONArray("pertanyaan") ?: JSONArray()
                val questions = (0 until qArr.length()).mapNotNull { qi ->
                    val qo = qArr.optJSONObject(qi) ?: return@mapNotNull null
                    QuestionnaireQuestion(
                        id = SiadinParsers.optStr(qo, "id", "id_pertanyaan").ifBlank { "$qi" },
                        text = SiadinParsers.optStr(qo, "pernyataan", "pertanyaan", "text")
                    )
                }
                QuestionnaireItem(
                    id = SiadinParsers.optStr(o, "id", "id_master", "id_jadwal").ifBlank { "$i" },
                    title = SiadinParsers.optStr(o, "judul", "nmmk", "nama_matkul").ifBlank { "Kuisioner $type #${i + 1}" },
                    courseName = SiadinParsers.optStr(o, "nmmk", "nama_matkul"),
                    lecturer = SiadinParsers.optStr(o, "nama_dosen", "dosen"),
                    isFilled = SiadinParsers.optBoolFlex(o, "is_filled"),
                    questions = questions
                )
            }
            QuestionnaireOverview(
                isShow = SiadinParsers.optBoolFlex(d, "is_show_kuisioner"),
                allFilled = SiadinParsers.optBoolFlex(d, "all_filled", default = true),
                total = SiadinParsers.optIntFlex(d, "total"),
                filled = SiadinParsers.optIntFlex(d, "filled"),
                items = items
            )
        }
    }

    suspend fun submitQuestionnaire(type: String, answersPayload: JSONObject): ApiResult<String> {
        val path = if (type.equals("UAS", ignoreCase = true)) "create-kuisioner-uas" else "create-kuisioner-uts"
        return client.postSiadin(path, answersPayload).mapSuccess {
            it.optString("message").ifBlank { "Kuisioner berhasil dikirim" }
        }
    }

    suspend fun checkFilledQuiz(): ApiResult<QuizGateStatus> =
        client.postRoot(
            "udinus/check-filled-quiz",
            JSONObject().put("actor", "mahasiswa")
        ).mapSuccess { root ->
            val d = root.optJSONObject("data") ?: root
            val quizArr = d.optJSONArray("quiz")
            val hasPending = (quizArr?.length() ?: 0) > 0
            val firstQuizObj = quizArr?.optJSONObject(0)
            QuizGateStatus(
                isShow = hasPending || SiadinParsers.optBoolFlex(d, "is_show", "is_show_kuisioner"),
                isFilled = !hasPending && SiadinParsers.optBoolFlex(d, "is_filled", default = true),
                allFilled = !hasPending && SiadinParsers.optBoolFlex(d, "all_filled", default = true),
                quizLink = SiadinParsers.optStr(firstQuizObj ?: d, "quiz_link", "link", "url")
            )
        }

    suspend fun getAnnouncements(): ApiResult<List<AcademicAnnouncement>> =
        client.getSiadin("get-list-infoakdmk").mapSuccess { SiadinParsers.parseAnnouncements(it) }

    suspend fun getAnnouncementDetail(id: String): ApiResult<AcademicAnnouncement> =
        client.getSiadin("get-infoakdmk?id=$id").mapSuccess { root ->
            val d = root.optJSONObject("data") ?: root
            AcademicAnnouncement(
                id = SiadinParsers.optStr(d, "id").ifBlank { id },
                title = SiadinParsers.optStr(d, "judul", "title"),
                date = SiadinParsers.optStr(d, "tanggal", "created_at"),
                category = SiadinParsers.optStr(d, "kategori"),
                summary = SiadinParsers.optStr(d, "ringkasan"),
                htmlBody = SiadinParsers.optStr(d, "isi", "content", "konten"),
                imageUrl = SiadinParsers.optStr(d, "img", "gambar")
            )
        }

    suspend fun getReligions(): ApiResult<List<LookupOption>> =
        client.getSiadin("get-agama").mapSuccess { root ->
            val arr = SiadinParsers.extractArray(root, "agama", "data")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                LookupOption(
                    id = SiadinParsers.optStr(o, "id", "agama_num"),
                    label = SiadinParsers.optStr(o, "n_agama", "agama")
                )
            }
        }

    private inline fun <T, R> ApiResult<T>.mapSuccess(transform: (T) -> R): ApiResult<R> = when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.Error -> this
    }

    private fun <T> ApiResult<T>.mapUnit(): ApiResult<Unit> = mapSuccess { }
}
