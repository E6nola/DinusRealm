package id.dinusrealm.app

import id.dinusrealm.app.core.model.BillingItem
import id.dinusrealm.app.core.network.SiadinParsers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiadinParsersTest {

    @Test
    fun `parseTodaySchedule extracts room time and open_class`() {
        val json = JSONObject(
            """
            {
              "code": 200,
              "data": [
                {
                  "kdmk": "A12.66501",
                  "klpk": "A12.6501",
                  "nmmk": "Manajemen Proyek TI",
                  "sesi1": "08.40 - 10.20",
                  "ruang1": "H.4.12",
                  "nama_dosen": "Dr. Budi",
                  "open_class": { "id": 99812, "pertemuan_ke": 7 }
                }
              ]
            }
            """.trimIndent()
        )

        val slots = SiadinParsers.parseTodaySchedule(json)
        assertEquals(1, slots.size)
        val first = slots[0]
        assertEquals("Manajemen Proyek TI", first.courseName)
        assertEquals("08:40", first.startTime)
        assertEquals("10:20", first.endTime)
        assertEquals("H.4.12", first.room)
        assertEquals("Dr. Budi", first.lecturer)
        assertEquals("99812", first.idMaster)
        assertTrue(first.isOpenClass)
        assertEquals(7, first.meetingNumber)

        val closedIntJson = JSONObject(
            """
            {
              "code": 200,
              "data": [
                {
                  "kdmk": "A12.66502",
                  "klpk": "A12.6501",
                  "nmmk": "Keamanan Sistem Informasi",
                  "sesi1": "10.20 - 12.00",
                  "ruang1": "H.4.11",
                  "open_class": 1
                }
              ]
            }
            """.trimIndent()
        )
        val closedSlots = SiadinParsers.parseTodaySchedule(closedIntJson)
        assertEquals(1, closedSlots.size)
        assertFalse(closedSlots[0].isOpenClass)
        assertEquals(null, closedSlots[0].idMaster)
    }

    @Test
    fun `parseWeeklySchedule handles live nested hari and jadwal array`() {
        val json = JSONObject(
            """
            {
              "code": 200,
              "data": [
                {
                  "hari": "SENIN",
                  "jadwal": [
                    {
                      "nmmk": "AUDIT SISTEM INFORMASI",
                      "klpk": "A12.6504",
                      "sks": 4,
                      "hari": "SENIN",
                      "ruang": "H.4.11",
                      "sesi": "08.40-10.20",
                      "open_class": 1
                    }
                  ]
                },
                {
                  "hari": "RABU",
                  "jadwal": [
                    {
                      "nmmk": "MANAJEMEN PROYEK",
                      "klpk": "A12.6501",
                      "sks": 3,
                      "hari": "RABU",
                      "ruang": "H.3.2",
                      "sesi": "10.20-12.00",
                      "open_class": 0
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val slots = SiadinParsers.parseWeeklySchedule(json)
        assertEquals(2, slots.size)
        assertEquals("Senin", slots[0].dayName)
        assertEquals(1, slots[0].dayIndex)
        assertEquals("H.4.11", slots[0].room)
        assertEquals("08:40", slots[0].startTime)
        assertFalse(slots[0].isOpenClass)
        assertEquals("Rabu", slots[1].dayName)
        assertFalse(slots[1].isOpenClass)
    }

    @Test
    fun `parseWeeklySchedule expands flat hari1 to 3 fallback`() {
        val json = JSONObject(
            """
            {
              "code": 200,
              "data": [
                {
                  "kdmk": "MK01",
                  "klpk": "A12.01",
                  "nmmk": "Basis Data",
                  "sks": 4,
                  "hari1": "Rabu",
                  "sesi1": "10.20 - 12.00",
                  "ruang1": "H.3.2",
                  "hari2": "Senin",
                  "sesi2": "07.00 - 08.40",
                  "ruang2": "D.2.G"
                }
              ]
            }
            """.trimIndent()
        )

        val slots = SiadinParsers.parseWeeklySchedule(json)
        assertEquals(2, slots.size)
        assertEquals("Senin", slots[0].dayName)
        assertEquals("D.2.G", slots[0].room)
        assertEquals("Rabu", slots[1].dayName)
    }

    @Test
    fun `parseAttendanceCourses and parseAttendanceDetail match live berangkat field`() {
        val sumJson = JSONObject(
            """
            {
              "code": 200,
              "data": [
                {
                  "nama_matkul": "AUDIT SISTEM INFORMASI",
                  "kdmk": "A12.76501",
                  "prsn_hadir": 53.57,
                  "jml_hadir": 15,
                  "jml_bolos": 0,
                  "jadwal1": "SENIN, 08.40-10.20 (H.4.11)",
                  "jadwal2": "KAMIS, 08.40-10.20 (H.5.2)",
                  "jadwal3": null
                }
              ]
            }
            """.trimIndent()
        )
        val courses = SiadinParsers.parseAttendanceCourses(sumJson)
        assertEquals(1, courses.size)
        assertEquals("A12.76501", courses[0].kdmk)
        assertEquals(53.57, courses[0].percentage, 0.01)
        assertTrue(courses[0].scheduleSummary.contains("H.4.11"))

        val detailJson = JSONObject(
            """
            {
              "code": 200,
              "data": {
                "detail_absensi": [
                  {"pertemuan_ke": "Pertemuan ke-1", "tanggal": "2026-09-14", "berangkat": 1},
                  {"pertemuan_ke": "Pertemuan ke-2", "tanggal": "2026-09-21", "berangkat": 0}
                ]
              }
            }
            """.trimIndent()
        )
        val meetings = SiadinParsers.parseAttendanceDetail(detailJson)
        assertEquals(2, meetings.size)
        assertEquals("Pertemuan ke-1", meetings[0].label)
        assertEquals("Hadir", meetings[0].status)
        assertEquals("Alpha", meetings[1].status)
    }

    @Test
    fun `parseKhsCurrent parseTranscript and parseExamBundle match live fields`() {
        val khsJson = JSONObject(
            """
            {
              "code": 200,
              "data": {
                "is_bayar": 1,
                "ta": "2026/2027 Ganjil",
                "ips": "3.75",
                "max_sks": "24",
                "mata_kuliah": [
                  {
                    "nama_dosen": " BUDI WIDJAJANTO M.Kom",
                    "kdmk": "A12.76501",
                    "nmmk": "AUDIT SISTEM INFORMASI",
                    "sks": 4,
                    "klpk": "A12.6504",
                    "nakh": "A | 4.00"
                  }
                ]
              }
            }
            """.trimIndent()
        )
        val (sum, courses) = SiadinParsers.parseKhsCurrent(khsJson)
        assertEquals("3.75", sum.ips)
        assertEquals(1, courses.size)
        assertEquals("BUDI WIDJAJANTO M.Kom", courses[0].lecturer)
        assertEquals("A", courses[0].grade)

        val transJson = JSONObject(
            """
            {
              "code": 200,
              "data": [
                {"kdmk": "A12.101", "nl": "A", "nmmk": "ALGORITMA", "sks": 4}
              ]
            }
            """.trimIndent()
        )
        val trans = SiadinParsers.parseTranscript(transJson)
        assertEquals(4, trans.totalSks)
        assertEquals("A", trans.courses[0].grade)

        val examJson = JSONObject(
            """
            {
              "code": 200,
              "data": {
                "is_krs_verified": 1,
                "status_pembayaran": "Anda telah melakukan pembayaran",
                "is_bayar": 1,
                "jadwal_uts": [
                  {
                    "nmmk": "AUDIT SISTEM INFORMASI",
                    "sks": 4,
                    "klpk": "A12.6504",
                    "t_tglhari": "Rabu, 11 November 2026",
                    "t_jam": "07:30 - 09:30",
                    "ruang": "H.4.11",
                    "no_kursi": 413
                  }
                ]
              }
            }
            """.trimIndent()
        )
        val bundle = SiadinParsers.parseExamBundle(examJson, "UTS")
        assertTrue(bundle.isPaid)
        assertEquals(1, bundle.items.size)
        assertEquals("413", bundle.items[0].seatNumber)
        assertEquals("H.4.11", bundle.items[0].room)
    }

    @Test
    fun `parseBilling and parsePaymentInfo parse live tagihan and 5 bank VAs`() {
        val tagihanJson = JSONObject(
            """
            {
              "code": 200,
              "data": {
                "informasi": "SPP Lunas",
                "status_pembayaran": "Lunas",
                "tahun_ajaran": "2026/2027 Ganjil",
                "terbayar": 4500000,
                "sisa": 0,
                "is_adatagihan": 1,
                "SPP_sekarang": 3000000,
                "SKS_sekarang": 1500000,
                "tot_wajib": 4500000,
                "tot_kurang": 0,
                "total_tagih": 4500000
              }
            }
            """.trimIndent()
        )
        val bills = SiadinParsers.parseBilling(tagihanJson, "Reguler")
        assertEquals(1, bills.size)
        assertTrue(bills[0].isPaid)
        assertEquals(2, bills[0].breakdown.size)

        val payJson = JSONObject(
            """
            {
              "code": 200,
              "data": {
                "metode_bayar": [
                  {"nama_bank": "BCA Virtual Account", "va": "1234567890", "atas_nama": "JOE DOE", "auto_validasi": "Auto Validasi", "tutorial": "https://bca.co.id"}
                ],
                "catatan": "<p>Info</p>"
              }
            }
            """.trimIndent()
        )
        val info = SiadinParsers.parsePaymentInfo(payJson)
        assertEquals(1, info.methods.size)
        assertEquals("1234567890", info.methods[0].vaNumber)
    }

    @Test
    fun `parseKrsOnline extracts id_master kdds and attendance flag`() {
        val json = JSONObject(
            """
            {
              "code": 200,
              "data": {
                "jadwal": [
                  {
                    "id_master": 12345,
                    "kdds": "061234",
                    "kdmk": "A12.6601",
                    "klpk": "A12.6502",
                    "nmmk": "Data Mining",
                    "jam_mulai": "12.30",
                    "jam_selesai": "14.10",
                    "ruang": "H.5.4",
                    "is_attend": 0
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val list = SiadinParsers.parseKrsOnline(json)
        assertEquals(1, list.size)
        assertEquals("12345", list[0].idMaster)
        assertEquals("061234", list[0].kdds)
        assertEquals("12:30", list[0].startTime)
        assertFalse(list[0].isAttended)
        assertTrue(list[0].isOpenClass)
    }

    @Test
    fun `detectBillingChanges identifies new or modified unpaid bills only`() {
        val oldItem = BillingItem(
            id = "b1", title = "SPP", category = "Reguler",
            totalAmount = 3000000, paidAmount = 3000000, remainingAmount = 0,
            isPaid = true, statusText = "Lunas", rawSnapshotHash = "h1"
        )
        val newUnpaid = BillingItem(
            id = "b2", title = "SKS Semester Genap", category = "Reguler",
            totalAmount = 2400000, paidAmount = 0, remainingAmount = 2400000,
            isPaid = false, statusText = "Belum Lunas", vaBca = "12345", rawSnapshotHash = "h2"
        )

        val diffs = SiadinParsers.detectBillingChanges(listOf(oldItem), listOf(oldItem, newUnpaid))
        assertEquals(1, diffs.size)
        assertEquals("b2", diffs[0].id)
    }

    @Test
    fun `parseBilling prefers descriptive status over 3-letter status_pembayaran code`() {
        val paidJson = JSONObject(
            """
            {
              "code": 200,
              "data": {
                "status_pembayaran": "UTS",
                "status": "Lunas Pembayaran UTS",
                "tahun_ajaran": "2026/2027 Ganjil",
                "terbayar": 0,
                "sisa": 0,
                "tot_kurang": 0,
                "total_tagih": 0,
                "is_adatagihan": 1
              }
            }
            """.trimIndent()
        )
        val paid = SiadinParsers.parseBilling(paidJson, "Reguler")
        assertEquals(1, paid.size)
        assertEquals("Lunas Pembayaran UTS", paid[0].statusText)
        assertTrue(paid[0].isPaid)

        val unpaidJson = JSONObject(
            """
            {
              "code": 200,
              "data": {
                "status_pembayaran": "UTS",
                "status": "Belum Lunas UTS",
                "tahun_ajaran": "2026/2027 Ganjil",
                "terbayar": 0,
                "sisa": 1500000,
                "tot_kurang": 1500000,
                "total_tagih": 1500000,
                "is_adatagihan": 1
              }
            }
            """.trimIndent()
        )
        val unpaid = SiadinParsers.parseBilling(unpaidJson, "Reguler")
        assertEquals(1, unpaid.size)
        assertEquals("Belum Lunas UTS", unpaid[0].statusText)
        assertFalse(unpaid[0].isPaid)
    }
}
