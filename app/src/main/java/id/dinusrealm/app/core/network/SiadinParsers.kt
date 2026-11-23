package id.dinusrealm.app.core.network

import id.dinusrealm.app.core.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Pure `org.json` parsers verified against live DINUSVERSE `api.dinus.ac.id` payloads.
 */
object SiadinParsers {

    fun optStr(obj: JSONObject?, vararg keys: String): String {
        if (obj == null) return ""
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            val raw = obj.opt(k)?.toString()?.trim() ?: ""
            if (raw.isNotEmpty() && raw != "null") return raw
        }
        return ""
    }

    fun optIntFlex(obj: JSONObject?, vararg keys: String, default: Int = 0): Int {
        if (obj == null) return default
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            when (val raw = obj.opt(k)) {
                is Number -> return raw.toInt()
                is String -> raw.trim().toIntOrNull()?.let { return it }
            }
        }
        return default
    }

    fun optLongFlex(obj: JSONObject?, vararg keys: String, default: Long = 0L): Long {
        if (obj == null) return default
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            when (val raw = obj.opt(k)) {
                is Number -> return raw.toLong()
                is String -> raw.replace(Regex("[^0-9-]"), "").toLongOrNull()?.let { return it }
            }
        }
        return default
    }

    fun optDoubleFlex(obj: JSONObject?, vararg keys: String, default: Double = 0.0): Double {
        if (obj == null) return default
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            when (val raw = obj.opt(k)) {
                is Number -> return raw.toDouble()
                is String -> raw.trim().replace(",", ".").toDoubleOrNull()?.let { return it }
            }
        }
        return default
    }

    fun optBoolFlex(obj: JSONObject?, vararg keys: String, default: Boolean = false): Boolean {
        if (obj == null) return default
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            when (val raw = obj.opt(k)) {
                is Boolean -> return raw
                is Number -> return raw.toInt() != 0
                is String -> {
                    val s = raw.trim().lowercase()
                    if (s in setOf("true", "1", "yes", "ya", "y", "lunas")) return true
                    if (s in setOf("false", "0", "no", "tidak", "n")) return false
                }
            }
        }
        return default
    }

    fun parseTimePair(raw: String): Pair<String, String> {
        val normalized = raw.trim().replace('.', ':')
        val match = Regex("""(\d{1,2}:\d{2})\s*[-–]\s*(\d{1,2}:\d{2})""").find(normalized)
        return if (match != null) {
            padTime(match.groupValues[1]) to padTime(match.groupValues[2])
        } else {
            "" to ""
        }
    }

    private fun padTime(t: String): String {
        val parts = t.split(":")
        return if (parts.size == 2) {
            "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
        } else t
    }

    fun dayNameToIndex(day: String): Int = when (day.trim().lowercase()) {
        "senin", "monday", "1" -> 1
        "selasa", "tuesday", "2" -> 2
        "rabu", "wednesday", "3" -> 3
        "kamis", "thursday", "4" -> 4
        "jumat", "jum'at", "friday", "5" -> 5
        "sabtu", "saturday", "6" -> 6
        "minggu", "ahad", "sunday", "7" -> 7
        else -> 0
    }

    fun dayIndexToName(index: Int): String = when (index) {
        1 -> "Senin"
        2 -> "Selasa"
        3 -> "Rabu"
        4 -> "Kamis"
        5 -> "Jumat"
        6 -> "Sabtu"
        7 -> "Minggu"
        else -> "Hari Ini"
    }

    fun extractArray(root: JSONObject, vararg keys: String): JSONArray {
        val dataObj = root.optJSONObject("data")
        for (k in keys) {
            dataObj?.optJSONArray(k)?.let { return it }
            root.optJSONArray(k)?.let { return it }
        }
        return root.optJSONArray("data") ?: JSONArray()
    }

    fun parseTodaySchedule(root: JSONObject): List<ScheduleSlot> {
        val arr = extractArray(root, "jadwal", "data")
        val list = mutableListOf<ScheduleSlot>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val openObj = item.optJSONObject("open_class")
            val sesi = optStr(item, "sesi1", "sesi", "jam_kul", "jam")
            val (parsedStart, parsedEnd) = parseTimePair(sesi)
            val start = optStr(item, "jam_mulai").replace('.', ':').ifBlank { parsedStart }
            val end = optStr(item, "jam_selesai").replace('.', ':').ifBlank { parsedEnd }
            val kdmk = optStr(item, "kdmk", "kode_mk")
            val klpk = optStr(item, "klpk", "kelompok")
            val idMaster = optStr(openObj, "id", "id_master").ifBlank {
                optStr(item, "id_master").ifBlank { null }
            }
            val dayStr = optStr(item, "hari", "hari1")
            val dayIdx = dayNameToIndex(dayStr)
            list += ScheduleSlot(
                id = "today_${kdmk}_${klpk}_$i",
                kdmk = kdmk,
                klpk = klpk,
                courseName = optStr(item, "nmmk", "nama_matkul", "mata_kuliah"),
                sks = optIntFlex(item, "sks"),
                dayIndex = dayIdx,
                dayName = if (dayIdx > 0) dayIndexToName(dayIdx) else "Hari Ini",
                startTime = start,
                endTime = end,
                room = optStr(item, "ruang1", "ruang"),
                lecturer = optStr(item, "nama_dosen", "dosen"),
                idMaster = idMaster,
                kdds = optStr(item, "kdds"),
                isOpenClass = openObj != null,
                isAttended = optBoolFlex(item, "is_attend", "sudah_absen"),
                meetingNumber = optIntFlex(openObj ?: item, "pertemuan_ke")
            )
        }
        return list.sortedBy { it.startTime }
    }

    /**
     * Parses `get-jadwal` live structure:
     * `data: [{"hari": "SENIN", "jadwal": [{"nmmk":..., "klpk":..., "sks":..., "hari":..., "ruang":..., "sesi":..., "open_class":1}]}]`
     * Also keeps compatibility with flat `hari1..3` structures.
     */
    fun parseWeeklySchedule(root: JSONObject): List<ScheduleSlot> {
        val result = mutableListOf<ScheduleSlot>()
        val dataArr = root.optJSONArray("data")
        if (dataArr != null) {
            for (i in 0 until dataArr.length()) {
                val entry = dataArr.optJSONObject(i) ?: continue
                val nestedJadwal = entry.optJSONArray("jadwal")
                if (nestedJadwal != null) {
                    val groupDay = optStr(entry, "hari")
                    for (j in 0 until nestedJadwal.length()) {
                        val item = nestedJadwal.optJSONObject(j) ?: continue
                        val hari = optStr(item, "hari").ifBlank { groupDay }
                        val dIdx = dayNameToIndex(hari)
                        val (start, end) = parseTimePair(optStr(item, "sesi", "sesi1", "jam_kul"))
                        val kdmk = optStr(item, "kdmk", "kode_mk")
                        val klpk = optStr(item, "klpk", "kelompok")
                        val nmmk = optStr(item, "nmmk", "nama_matkul", "mata_kuliah")
                        val openObj = item.optJSONObject("open_class")
                        val openBool = openObj != null
                        result += ScheduleSlot(
                            id = "w_${dIdx}_${klpk}_${nmmk.hashCode()}_$j",
                            kdmk = kdmk,
                            klpk = klpk,
                            courseName = nmmk,
                            sks = optIntFlex(item, "sks"),
                            dayIndex = dIdx,
                            dayName = if (dIdx > 0) dayIndexToName(dIdx) else hari,
                            startTime = start.ifBlank { optStr(item, "jam_mulai") },
                            endTime = end.ifBlank { optStr(item, "jam_selesai") },
                            room = optStr(item, "ruang", "ruang1"),
                            lecturer = optStr(item, "nama_dosen", "dosen"),
                            idMaster = optStr(openObj, "id", "id_master").ifBlank { optStr(item, "id_master").ifBlank { null } },
                            isOpenClass = openBool
                        )
                    }
                    continue
                }

                // Fallback: flat course object with hari1..3 / sesi1..3 / ruang1..3
                val kdmk = optStr(entry, "kdmk", "kode_mk")
                val klpk = optStr(entry, "klpk", "kelompok")
                val name = optStr(entry, "nmmk", "nama_matkul", "mata_kuliah")
                val sks = optIntFlex(entry, "sks")
                val lecturer = optStr(entry, "nama_dosen", "dosen")
                var addedIndexed = false
                for (slotIdx in 1..3) {
                    val hari = optStr(entry, "hari$slotIdx")
                    val sesi = optStr(entry, "sesi$slotIdx", "jadwal$slotIdx")
                    val ruang = optStr(entry, "ruang$slotIdx")
                    if (hari.isNotBlank() || sesi.isNotBlank()) {
                        addedIndexed = true
                        val (start, end) = parseTimePair(sesi)
                        val dIdx = dayNameToIndex(hari)
                        result += ScheduleSlot(
                            id = "${kdmk}_${klpk}_$slotIdx",
                            kdmk = kdmk,
                            klpk = klpk,
                            courseName = name,
                            sks = sks,
                            dayIndex = dIdx,
                            dayName = if (dIdx > 0) dayIndexToName(dIdx) else hari,
                            startTime = start,
                            endTime = end,
                            room = ruang,
                            lecturer = lecturer
                        )
                    }
                }
                if (!addedIndexed && name.isNotBlank()) {
                    val hari = optStr(entry, "hari", "day")
                    val (start, end) = parseTimePair(optStr(entry, "sesi", "jam_kul", "jadwal"))
                    val dIdx = dayNameToIndex(hari)
                    result += ScheduleSlot(
                        id = "${kdmk}_${klpk}_$i",
                        kdmk = kdmk,
                        klpk = klpk,
                        courseName = name,
                        sks = sks,
                        dayIndex = dIdx,
                        dayName = if (dIdx > 0) dayIndexToName(dIdx) else hari,
                        startTime = start.ifBlank { optStr(entry, "jam_mulai") },
                        endTime = end.ifBlank { optStr(entry, "jam_selesai") },
                        room = optStr(entry, "ruang"),
                        lecturer = lecturer
                    )
                }
            }
        }
        return result.sortedWith(compareBy({ it.dayIndex }, { it.startTime }))
    }

    fun parseKrsOnline(root: JSONObject): List<OnlineAttendanceItem> {
        val arr = extractArray(root, "jadwal", "data")
        val list = mutableListOf<OnlineAttendanceItem>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val (pStart, pEnd) = parseTimePair(optStr(item, "sesi", "sesi1", "jam_kul"))
            val start = optStr(item, "jam_mulai").replace('.', ':').ifBlank { pStart }
            val end = optStr(item, "jam_selesai").replace('.', ':').ifBlank { pEnd }
            val openObj = item.optJSONObject("open_class")
            val idMaster = optStr(item, "id_master").ifBlank { optStr(openObj, "id") }
            list += OnlineAttendanceItem(
                idMaster = idMaster,
                kdds = optStr(item, "kdds"),
                kdmk = optStr(item, "kdmk"),
                klpk = optStr(item, "klpk"),
                courseName = optStr(item, "nmmk", "nama_matkul"),
                startTime = start,
                endTime = end,
                room = optStr(item, "ruang", "ruang1"),
                lecturer = optStr(item, "nama_dosen", "dosen"),
                isAttended = optBoolFlex(item, "is_attend"),
                isOpenClass = openObj != null || idMaster.isNotBlank(),
                meetingNumber = optIntFlex(item, "pertemuan_ke"),
                rpsPlan = optStr(item, "rencana_rps"),
                rpsRealization = optStr(item, "realisasi_rps"),
                isRpsConfirmed = optBoolFlex(item, "flag_konfirmasi")
            )
        }
        return list
    }

    fun parseMiniProfile(root: JSONObject): StudentMiniProfile {
        val d = root.optJSONObject("data") ?: root
        return StudentMiniProfile(
            nim = optStr(d, "nim", "username"),
            name = optStr(d, "nama", "nama_mhs", "name"),
            programStudy = optStr(d, "prodi", "nama_prodi", "jurusan"),
            faculty = optStr(d, "fakultas", "nama_fakultas"),
            semester = optStr(d, "semester", "smt"),
            academicStatus = optStr(d, "akdm_stat", "status"),
            photoUrl = optStr(d, "foto", "photo", "avatar", "img"),
            emailMhs = optStr(d, "email_mhs"),
            gender = optStr(d, "gender", "sex"),
            dosenWali = optStr(d, "dosen", "dosen_wali", "nama_doswal")
        )
    }

    fun parseFullProfile(root: JSONObject): StudentProfile {
        val d = root.optJSONObject("data") ?: root
        return StudentProfile(
            nim = optStr(d, "nim"),
            name = optStr(d, "nama", "nama_mhs"),
            emailMhs = optStr(d, "email_mhs"),
            emailLain = optStr(d, "email_lain", "email_lain_1"),
            hpMhs = optStr(d, "hp_mhs"),
            academicStatus = optStr(d, "akdm_stat"),
            kelas = optStr(d, "kelas"),
            nisn = optStr(d, "nisn"),
            nik = optStr(d, "nik"),
            alamatSmg = optStr(d, "alamat_smg", "alm_smg"),
            telpSmg = optStr(d, "telp_smg"),
            kdposSmg = optStr(d, "kodepos_smg", "kdpos_smg"),
            alamatAsal = optStr(d, "alamat_asal", "alm_asal"),
            telpAsal = optStr(d, "telp_asal"),
            kdposAsal = optStr(d, "kodepos_asal", "kdpos_asal"),
            alamatOrtu = optStr(d, "alamat_ortu", "alm_ortu"),
            telpOrtu = optStr(d, "telp_ortu"),
            nmAyah = optStr(d, "nm_ayah"),
            nmIbuKandung = optStr(d, "nm_ibu_kandung"),
            nmWali = optStr(d, "nm_wali"),
            tmpLahir = optStr(d, "tmp_lahir"),
            tglLahir = optStr(d, "tgl_lahir"),
            golDarah = optStr(d, "gol_darah"),
            agama = optStr(d, "agama", "n_agama"),
            agamaNum = optStr(d, "agama_num"),
            wargaNegara = optStr(d, "warga_negara"),
            urlBlog = optStr(d, "url_blog"),
            idWilayah = optStr(d, "id_wilayah", "kecamatan"),
            nmKecamatan = optStr(d, "nm_kecamatan"),
            dosenWaliName = optStr(d, "dosen", "dosen_wali"),
            dosenWaliNpp = optStr(d, "npp_dosen"),
            dosenWaliEmail = optStr(d, "email_dosen"),
            dosenWaliPhone = optStr(d, "hp_dosen", "telp_dosen"),
            dosenWaliWa = optStr(d, "wa_dosen")
        )
    }

    fun parseBilling(root: JSONObject, category: String): List<BillingItem> {
        val items = mutableListOf<JSONObject>()
        val dataArr = root.optJSONArray("data")
        if (dataArr != null) {
            for (i in 0 until dataArr.length()) {
                dataArr.optJSONObject(i)?.let { items += it }
            }
        } else {
            val dataObj = root.optJSONObject("data")
            if (dataObj != null) {
                val nested = dataObj.optJSONArray("tagihan") ?: dataObj.optJSONArray("list")
                if (nested != null) {
                    for (i in 0 until nested.length()) {
                        nested.optJSONObject(i)?.let { items += it }
                    }
                } else {
                    items += dataObj
                }
            }
        }

        val breakdownMap = listOf(
            "SPP_sekarang" to "SPP Semester Ini",
            "SKS_sekarang" to "SKS Semester Ini",
            "GDG_sekarang" to "Gedung Semester Ini",
            "MOD_sekarang" to "Modul Semester Ini",
            "POLI_sekarang" to "Poliklinik Semester Ini",
            "BK_sekarang" to "Biaya Kemahasiswaan",
            "SPP_lalu" to "Tunggakan SPP",
            "SKS_lalu" to "Tunggakan SKS",
            "GDG_lalu" to "Tunggakan Gedung",
            "MOD_lalu" to "Tunggakan Modul",
            "POLI_lalu" to "Tunggakan Poliklinik",
            "BK_lalu" to "Tunggakan BK"
        )

        return items.mapIndexedNotNull { idx, obj ->
            val hasTagihan = if (obj.has("is_adatagihan")) optBoolFlex(obj, "is_adatagihan") else true
            val ta = optStr(obj, "tahun_ajaran")
            val title = optStr(obj, "nama_tagihan", "keterangan", "ket").ifBlank {
                if (ta.isNotBlank()) "Tagihan $category $ta" else "Tagihan $category"
            }
            val total = optLongFlex(obj, "total_tagih", "tot_wajib", "total")
            val paid = optLongFlex(obj, "terbayar", "paid")
            val remaining = optLongFlex(obj, "tot_kurang", "sisa", default = (total - paid).coerceAtLeast(0L))
            if (!hasTagihan && total == 0L && remaining == 0L && paid == 0L) return@mapIndexedNotNull null

            val statusRaw = optStr(obj, "status", "status_pembayaran")
            val statusIndicatesPaid = statusRaw.contains("lunas", ignoreCase = true) &&
                !statusRaw.contains("belum", ignoreCase = true)
            val isPaid = optBoolFlex(
                obj,
                "is_bayar",
                default = (remaining <= 0L && (total > 0L || paid > 0L)) || statusIndicatesPaid
            )
            val status = statusRaw.ifBlank { if (isPaid) "Lunas" else "Belum Lunas" }
            val breakdown = breakdownMap.mapNotNull { (k, lbl) ->
                val v = optLongFlex(obj, k)
                if (v > 0L) lbl to v else null
            }

            val vaBca = optStr(obj, "va_bca")
            val vaBjb = optStr(obj, "va_bj")
            val vaMandiri = optStr(obj, "va_mandiri")
            val due = optStr(obj, "tgl_expired", "tanggal")
            val id = optStr(obj, "id").ifBlank { "${category}_${idx}_$ta" }
            val hashInput = "$id|$title|$total|$remaining|$isPaid|$due|$vaBca|$vaBjb|$vaMandiri"

            BillingItem(
                id = id,
                title = title,
                category = category,
                totalAmount = total,
                paidAmount = paid,
                remainingAmount = remaining,
                isPaid = isPaid,
                statusText = status,
                infoText = optStr(obj, "informasi"),
                academicYear = ta,
                viaText = optStr(obj, "via"),
                dueDate = due,
                vaBca = vaBca,
                vaBjb = vaBjb,
                vaMandiri = vaMandiri,
                breakdown = breakdown,
                rawSnapshotHash = sha256Short(hashInput)
            )
        }
    }

    fun parsePaymentInfo(root: JSONObject): PaymentInfoBundle {
        val d = root.optJSONObject("data") ?: root
        val arr = d.optJSONArray("metode_bayar") ?: JSONArray()
        val methods = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            PaymentBankMethod(
                bankName = optStr(o, "nama_bank"),
                vaNumber = optStr(o, "va"),
                accountName = optStr(o, "atas_nama"),
                validationNote = optStr(o, "auto_validasi"),
                tutorialUrl = optStr(o, "tutorial")
            )
        }
        return PaymentInfoBundle(
            methods = methods,
            notesHtml = optStr(d, "catatan", "html", "content")
        )
    }

    fun detectBillingChanges(previous: List<BillingItem>, current: List<BillingItem>): List<BillingItem> {
        val prevMap = previous.associateBy { it.id }
        return current.filter { item ->
            val old = prevMap[item.id]
            !item.isPaid && item.remainingAmount > 0L && (old == null || old.rawSnapshotHash != item.rawSnapshotHash)
        }
    }

    fun parseAttendanceCourses(root: JSONObject): List<AttendanceCourse> {
        val arr = extractArray(root, "presensi", "data")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val sched = listOfNotNull(
                optStr(o, "jadwal1").takeIf { it.isNotBlank() },
                optStr(o, "jadwal2").takeIf { it.isNotBlank() },
                optStr(o, "jadwal3").takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            AttendanceCourse(
                idJadwal = optStr(o, "id_jadwal", "kdmk", "id"),
                kdmk = optStr(o, "kdmk"),
                klpk = optStr(o, "klpk"),
                courseName = optStr(o, "nama_matkul", "nmmk"),
                hadir = optIntFlex(o, "jml_hadir", "hadir"),
                sakit = optIntFlex(o, "jml_sakit", "sakit"),
                izin = optIntFlex(o, "jml_izin", "izin"),
                alpha = optIntFlex(o, "jml_bolos", "alpha"),
                percentage = optDoubleFlex(o, "prsn_hadir", "percentage"),
                scheduleSummary = sched
            )
        }
    }

    fun parseAttendanceDetail(root: JSONObject): List<AttendanceMeeting> {
        val arr = extractArray(root, "detail_absensi", "data")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val rawLabel = optStr(o, "pertemuan_ke", "pertemuan")
            val num = Regex("""(\d+)""").find(rawLabel)?.groupValues?.get(1)?.toIntOrNull() ?: (i + 1)
            val label = rawLabel.ifBlank { "Pertemuan ke-$num" }
            val statusText = optStr(o, "status", "ket", "keterangan").ifBlank {
                if (o.has("berangkat")) {
                    when (optIntFlex(o, "berangkat", default = 0)) {
                        1 -> "Hadir"
                        2 -> "Izin"
                        3 -> "Sakit"
                        else -> "Alpha"
                    }
                } else "Alpha"
            }
            AttendanceMeeting(
                meetingNumber = num,
                label = label,
                date = optStr(o, "tanggal", "tgl_kuliah"),
                status = statusText,
                topic = optStr(o, "realisasi_rps", "materi")
            )
        }
    }

    fun parseKrsSummary(root: JSONObject): KrsSummary {
        val d = root.optJSONObject("data") ?: root
        return KrsSummary(
            academicYear = optStr(d, "ta", "tahun_ajaran"),
            semester = optStr(d, "ganjilgenap", "semester"),
            courseCount = optIntFlex(d, "jumlah_matkul"),
            currentSks = optIntFlex(d, "total_sks", "SKS_sekarang", "sks"),
            maxSks = optIntFlex(d, "max_sks", default = 24),
            ipk = optStr(d, "ipk").ifBlank { "0.00" },
            isVerified = optBoolFlex(d, "is_verif")
        )
    }

    fun parseKrsCourses(root: JSONObject, taFallback: String = ""): List<KrsCourse> {
        val dataObj = root.optJSONObject("data")
        val arr = dataObj?.optJSONArray("krs") ?: extractArray(root, "krs", "data")
        val ta = optStr(dataObj, "ta").ifBlank { taFallback }
        return mapKrsArray(arr, ta)
    }

    fun parseKrsHistoryGroups(root: JSONObject): List<SemesterKrsGroup> {
        val arr = root.optJSONArray("data") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val group = arr.optJSONObject(i) ?: return@mapNotNull null
            val ta = optStr(group, "ta", "tahun_ajaran")
            val kodeTa = optStr(group, "kode_ta")
            val krsArr = group.optJSONArray("krs") ?: JSONArray()
            SemesterKrsGroup(
                academicYear = ta.ifBlank { kodeTa },
                kodeTa = kodeTa,
                courses = mapKrsArray(krsArr, ta)
            )
        }
    }

    private fun mapKrsArray(arr: JSONArray, taLabel: String): List<KrsCourse> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val slots = (1..3).mapNotNull { s ->
                val h = optStr(o, if (s == 1) "hari1" else "hari$s", if (s == 1) "hari" else "")
                val sesi = optStr(o, if (s == 1) "sesi1" else "sesi$s", if (s == 1) "sesi" else "")
                val ruang = optStr(o, if (s == 1) "ruang1" else "ruang$s", if (s == 1) "ruang" else "")
                if (h.isBlank() || h == "-" || sesi.isBlank()) null
                else "$h $sesi" + (if (ruang.isNotBlank() && ruang != "-") " ($ruang)" else "")
            }
            KrsCourse(
                kdmk = optStr(o, "kdmk"),
                klpk = optStr(o, "klpk"),
                courseName = optStr(o, "nmmk", "nama_matkul"),
                sks = optIntFlex(o, "sks"),
                statusTag = listOfNotNull(
                    optStr(o, "sts").takeIf { it.isNotBlank() },
                    optStr(o, "tp").takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                scheduleText = slots.joinToString(" | "),
                room = optStr(o, "ruang1", "ruang"),
                lecturer = optStr(o, "nama_dosen", "dosen"),
                semesterLabel = taLabel.ifBlank { optStr(o, "semester", "tahun_ajaran") },
                attendancePercent = if (o.has("prsn_hadir") && !o.isNull("prsn_hadir")) optDoubleFlex(o, "prsn_hadir") else null
            )
        }

    fun parseKhsHeader(root: JSONObject): KhsSummary {
        val d = root.optJSONObject("data") ?: root
        val distArr = d.optJSONArray("total_nilai") ?: JSONArray()
        val dist = (0 until distArr.length()).mapNotNull { i ->
            val o = distArr.optJSONObject(i) ?: return@mapNotNull null
            val g = optStr(o, "nilai")
            val cnt = optIntFlex(o, "jumlah")
            if (g.isNotBlank()) g to cnt else null
        }
        return KhsSummary(
            ipk = optStr(d, "ipk").ifBlank { "0.00" },
            totalSks = optIntFlex(d, "total_sks"),
            gradeDistribution = dist
        )
    }

    fun parseKhsCurrent(root: JSONObject): Pair<KhsSummary, List<KhsCourse>> {
        val d = root.optJSONObject("data") ?: root
        val summary = KhsSummary(
            academicYear = optStr(d, "ta", "tahun_ajaran"),
            semester = optStr(d, "kode_ta", "semester"),
            ips = optStr(d, "ips").ifBlank { "0.00" },
            ipk = optStr(d, "ipk").ifBlank { "0.00" },
            currentSks = optIntFlex(d, "max_sks", "SKS_sekarang"),
            isPaid = optBoolFlex(d, "is_bayar", default = true)
        )
        val arr = d.optJSONArray("mata_kuliah") ?: extractArray(root, "khs", "data")
        return summary to mapKhsArray(arr, summary.academicYear)
    }

    fun parseKhsHistoryGroups(root: JSONObject): List<SemesterKhsGroup> {
        val arr = root.optJSONArray("data") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val g = arr.optJSONObject(i) ?: return@mapNotNull null
            val ta = optStr(g, "ta", "tahun_ajaran")
            val pay = g.optJSONObject("data_pembayaran")
            val isPaid = optBoolFlex(pay, "is_bayar", default = true)
            val mkArr = g.optJSONArray("mata_kuliah") ?: JSONArray()
            SemesterKhsGroup(
                academicYear = ta.ifBlank { optStr(g, "kode_ta") },
                ips = optStr(g, "ips").ifBlank { "0.00" },
                maxSks = optIntFlex(g, "max_sks"),
                isPaid = isPaid,
                courses = mapKhsArray(mkArr, ta)
            )
        }
    }

    private fun mapKhsArray(arr: JSONArray, taLabel: String): List<KhsCourse> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val rawNakh = optStr(o, "nakh", "nilai", "huruf")
            val cleanGrade = rawNakh.split("|").firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "-"
            KhsCourse(
                kdmk = optStr(o, "kdmk"),
                klpk = optStr(o, "klpk"),
                courseName = optStr(o, "nmmk", "nama_matkul"),
                sks = optIntFlex(o, "sks"),
                grade = cleanGrade,
                lecturer = optStr(o, "nama_dosen", "dosen").trim(),
                tgsScore = optStr(o, "n_tgs"),
                utsScore = optStr(o, "n_uts", "nmid"),
                uasScore = optStr(o, "n_uas"),
                finalScore = rawNakh,
                semesterLabel = taLabel.ifBlank { optStr(o, "semester", "tahun_ajaran") }
            )
        }

    fun parseIpChart(root: JSONObject): List<IpChartPoint> {
        val arr = extractArray(root, "grafik", "data")
        var sumIps = 0.0
        var count = 0
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val ips = optDoubleFlex(o, "ips")
            if (ips > 0.0) {
                sumIps += ips
                count++
            }
            val rawIpk = optDoubleFlex(o, "ipk", default = -1.0)
            val computedIpk = if (rawIpk >= 0.0) rawIpk else if (count > 0) sumIps / count else 0.0
            IpChartPoint(
                semesterLabel = optStr(o, "semester", "smt").ifBlank { "Smt ${i + 1}" },
                ips = ips,
                ipk = computedIpk
            )
        }
    }

    fun parseTranscript(root: JSONObject): TranscriptSummary {
        val arr = root.optJSONArray("data") ?: extractArray(root, "transkrip", "mata_kuliah")
        val courses = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            KhsCourse(
                kdmk = optStr(o, "kdmk"),
                courseName = optStr(o, "nmmk", "nama_matkul"),
                sks = optIntFlex(o, "sks"),
                grade = optStr(o, "nl", "nilai", "huruf").ifBlank { "-" }
            )
        }
        val d = root.optJSONObject("data")
        return TranscriptSummary(
            totalSks = optIntFlex(d ?: root, "total_sks", default = courses.sumOf { it.sks }),
            totalPoints = optStr(d ?: root, "total_nilai"),
            ipk = optStr(d ?: root, "ipk").ifBlank { "0.00" },
            courses = courses
        )
    }

    fun parseExamBundle(root: JSONObject, type: String): ExamScheduleBundle {
        val d = root.optJSONObject("data") ?: root
        val isPaid = optBoolFlex(d, "is_bayar", default = true)
        val isVerified = optBoolFlex(d, "is_krs_verified", default = true)
        val arr = d.optJSONArray(if (type == "UAS") "jadwal_uas" else "jadwal_uts") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val allow = if (o.has("allow_uji") && !o.isNull("allow_uji")) optBoolFlex(o, "allow_uji") else isPaid
            ExamScheduleItem(
                type = type,
                kdmk = optStr(o, "kdmk"),
                klpk = optStr(o, "klpk"),
                courseName = optStr(o, "nmmk", "nama_matkul"),
                sks = optIntFlex(o, "sks"),
                date = optStr(o, "t_tglhari", "tanggal_uas", "tanggal", "tgl_ujian"),
                time = optStr(o, "t_jam", "jam", "sesi", "waktu"),
                room = optStr(o, "ruang"),
                seatNumber = optStr(o, "no_kursi"),
                practicalDate = optStr(o, "p_tglhari"),
                practicalTime = optStr(o, "p_jam"),
                note = optStr(o, "ket_uas", "ket_uts", "ket"),
                paymentEligible = allow,
                statusLabel = optStr(o, "status_pembayaran", "status_uas", "via_uas")
            )
        }
        return ExamScheduleBundle(
            isKrsVerified = isVerified,
            isPaid = isPaid,
            paymentStatusMessage = optStr(d, "status_pembayaran"),
            quizFilled = optIntFlex(d, "quiz_terisi"),
            quizTotal = optIntFlex(d, "listed_quiz"),
            items = items
        )
    }

    fun parseAnnouncements(root: JSONObject): List<AcademicAnnouncement> {
        val arr = extractArray(root, "informasi", "data")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            AcademicAnnouncement(
                id = optStr(o, "id"),
                title = optStr(o, "judul", "title"),
                date = optStr(o, "tanggal", "created_at"),
                category = optStr(o, "kategori", "category"),
                summary = optStr(o, "ringkasan", "ket"),
                htmlBody = optStr(o, "isi", "content", "konten"),
                imageUrl = optStr(o, "img", "gambar", "foto")
            )
        }
    }

    /**
     * Fills missing `lecturer` and `kdmk` on freshly fetched schedule slots from reference slots
     * (e.g. KHS/KRS/existing cache) matched by normalized `courseName|klpk` or `courseName`.
     */
    fun preserveSlotMetadata(
        fresh: List<ScheduleSlot>,
        reference: List<ScheduleSlot>
    ): List<ScheduleSlot> {
        if (fresh.isEmpty() || reference.isEmpty()) return fresh
        val lecturerByKey = mutableMapOf<String, String>()
        val kdmkByKey = mutableMapOf<String, String>()
        reference.forEach { s ->
            val nameKey = s.courseName.trim().lowercase()
            if (nameKey.isBlank()) return@forEach
            val fullKey = "$nameKey|${s.klpk.trim().lowercase()}"
            if (s.lecturer.isNotBlank()) {
                lecturerByKey.putIfAbsent(fullKey, s.lecturer)
                lecturerByKey.putIfAbsent(nameKey, s.lecturer)
            }
            if (s.kdmk.isNotBlank()) {
                kdmkByKey.putIfAbsent(fullKey, s.kdmk)
                kdmkByKey.putIfAbsent(nameKey, s.kdmk)
            }
        }
        return fresh.map { slot ->
            val nameKey = slot.courseName.trim().lowercase()
            val fullKey = "$nameKey|${slot.klpk.trim().lowercase()}"
            slot.copy(
                kdmk = slot.kdmk.ifBlank { kdmkByKey[fullKey] ?: kdmkByKey[nameKey].orEmpty() },
                lecturer = slot.lecturer.ifBlank { lecturerByKey[fullKey] ?: lecturerByKey[nameKey].orEmpty() }
            )
        }
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}
