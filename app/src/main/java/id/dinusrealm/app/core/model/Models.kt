package id.dinusrealm.app.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    ENGLISH,
    INDONESIAN
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val nickname: String = "",
    val classNotifEnabled: Boolean = true,
    val attendanceWatcherEnabled: Boolean = true,
    val autoAbsenEnabled: Boolean = true,
    val autoAbsenSecondsBeforeEnd: Int = 90,
    val billingNotifEnabled: Boolean = true
)

data class StudentMiniProfile(
    val nim: String = "",
    val name: String = "",
    val programStudy: String = "",
    val faculty: String = "",
    val semester: String = "",
    val academicStatus: String = "",
    val photoUrl: String = "",
    val emailMhs: String = "",
    val gender: String = "",
    val dosenWali: String = ""
)

data class StudentProfile(
    val nim: String = "",
    val name: String = "",
    val emailMhs: String = "",
    val emailLain: String = "",
    val hpMhs: String = "",
    val academicStatus: String = "",
    val kelas: String = "",
    val nisn: String = "",
    val nik: String = "",
    val alamatSmg: String = "",
    val telpSmg: String = "",
    val kdposSmg: String = "",
    val alamatAsal: String = "",
    val telpAsal: String = "",
    val kdposAsal: String = "",
    val alamatOrtu: String = "",
    val telpOrtu: String = "",
    val nmAyah: String = "",
    val nmIbuKandung: String = "",
    val nmWali: String = "",
    val tmpLahir: String = "",
    val tglLahir: String = "",
    val golDarah: String = "",
    val agama: String = "",
    val agamaNum: String = "",
    val wargaNegara: String = "",
    val urlBlog: String = "",
    val idWilayah: String = "",
    val nmKecamatan: String = "",
    val dosenWaliName: String = "",
    val dosenWaliNpp: String = "",
    val dosenWaliEmail: String = "",
    val dosenWaliPhone: String = "",
    val dosenWaliWa: String = ""
)

data class ScheduleSlot(
    val id: String,
    val kdmk: String,
    val klpk: String,
    val courseName: String,
    val sks: Int = 0,
    val dayIndex: Int, // 1=Senin .. 7=Minggu, 0=today
    val dayName: String,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val room: String,
    val lecturer: String = "",
    val idMaster: String? = null,
    val kdds: String = "",
    val isOpenClass: Boolean = false,
    val isAttended: Boolean = false,
    val meetingNumber: Int = 0
)

data class OnlineAttendanceItem(
    val idMaster: String,
    val kdds: String,
    val kdmk: String,
    val klpk: String,
    val courseName: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val lecturer: String,
    val isAttended: Boolean,
    val isOpenClass: Boolean = false,
    val meetingNumber: Int = 0,
    val rpsPlan: String = "",
    val rpsRealization: String = "",
    val isRpsConfirmed: Boolean = false
)

data class AttendanceCourse(
    val idJadwal: String,
    val kdmk: String,
    val klpk: String,
    val courseName: String,
    val hadir: Int,
    val sakit: Int,
    val izin: Int,
    val alpha: Int,
    val percentage: Double,
    val scheduleSummary: String = ""
)

data class AttendanceMeeting(
    val meetingNumber: Int,
    val label: String,
    val date: String,
    val status: String,
    val topic: String = ""
)

data class KrsSummary(
    val academicYear: String = "",
    val semester: String = "",
    val courseCount: Int = 0,
    val currentSks: Int = 0,
    val maxSks: Int = 24,
    val ipk: String = "0.00",
    val isVerified: Boolean = false
)

data class KrsCourse(
    val kdmk: String,
    val klpk: String,
    val courseName: String,
    val sks: Int,
    val statusTag: String = "",
    val scheduleText: String = "",
    val room: String = "",
    val lecturer: String = "",
    val semesterLabel: String = "",
    val attendancePercent: Double? = null
)

data class SemesterKrsGroup(
    val academicYear: String,
    val kodeTa: String,
    val courses: List<KrsCourse>
)

data class KhsSummary(
    val academicYear: String = "",
    val semester: String = "",
    val ips: String = "0.00",
    val ipk: String = "0.00",
    val previousSks: Int = 0,
    val currentSks: Int = 0,
    val totalSks: Int = 0,
    val isPaid: Boolean = true,
    val gradeDistribution: List<Pair<String, Int>> = emptyList()
)

data class KhsCourse(
    val kdmk: String,
    val klpk: String = "",
    val courseName: String,
    val sks: Int,
    val grade: String,
    val lecturer: String = "",
    val tgsScore: String = "",
    val utsScore: String = "",
    val uasScore: String = "",
    val finalScore: String = "",
    val semesterLabel: String = ""
)

data class SemesterKhsGroup(
    val academicYear: String,
    val ips: String,
    val maxSks: Int,
    val isPaid: Boolean,
    val courses: List<KhsCourse>
)

data class IpChartPoint(
    val semesterLabel: String,
    val ips: Double,
    val ipk: Double
)

data class TranscriptSummary(
    val totalSks: Int = 0,
    val totalPoints: String = "",
    val ipk: String = "0.00",
    val courses: List<KhsCourse> = emptyList()
)

data class ExamScheduleItem(
    val type: String, // "UTS" or "UAS"
    val kdmk: String,
    val klpk: String,
    val courseName: String,
    val sks: Int = 0,
    val date: String,
    val time: String,
    val room: String,
    val seatNumber: String = "",
    val practicalDate: String = "",
    val practicalTime: String = "",
    val note: String = "",
    val paymentEligible: Boolean = true,
    val statusLabel: String = ""
)

data class ExamScheduleBundle(
    val isKrsVerified: Boolean = true,
    val isPaid: Boolean = true,
    val paymentStatusMessage: String = "",
    val quizFilled: Int = 0,
    val quizTotal: Int = 0,
    val items: List<ExamScheduleItem> = emptyList()
)

data class BillingItem(
    val id: String,
    val title: String,
    val category: String, // "Reguler" or "Non-Reguler"
    val totalAmount: Long,
    val paidAmount: Long,
    val remainingAmount: Long,
    val isPaid: Boolean,
    val statusText: String,
    val infoText: String = "",
    val academicYear: String = "",
    val viaText: String = "",
    val dueDate: String = "",
    val vaBca: String = "",
    val vaBjb: String = "",
    val vaMandiri: String = "",
    val breakdown: List<Pair<String, Long>> = emptyList(),
    val rawSnapshotHash: String = ""
)

data class PaymentBankMethod(
    val bankName: String,
    val vaNumber: String,
    val accountName: String,
    val validationNote: String,
    val tutorialUrl: String
)

data class PaymentInfoBundle(
    val methods: List<PaymentBankMethod> = emptyList(),
    val notesHtml: String = ""
)

data class HeregistrasiInfo(
    val isShow: Boolean = false,
    val academicStatus: String = "",
    val wave: String = "",
    val isSubmitted: Boolean = false,
    val fields: Map<String, String> = emptyMap(),
    val docs: List<SupportingDoc> = emptyList()
)

data class SupportingDoc(
    val key: String,
    val label: String,
    val uploaded: Boolean,
    val fileUrl: String = ""
)

data class QuestionnaireItem(
    val id: String,
    val title: String,
    val courseName: String = "",
    val lecturer: String = "",
    val isFilled: Boolean = false,
    val questions: List<QuestionnaireQuestion> = emptyList()
)

data class QuestionnaireQuestion(
    val id: String,
    val text: String,
    val options: List<String> = emptyList()
)

data class QuestionnaireOverview(
    val isShow: Boolean = false,
    val allFilled: Boolean = true,
    val total: Int = 0,
    val filled: Int = 0,
    val items: List<QuestionnaireItem> = emptyList()
)

data class QuizGateStatus(
    val isShow: Boolean = false,
    val isFilled: Boolean = true,
    val allFilled: Boolean = true,
    val quizLink: String = ""
)

data class AcademicAnnouncement(
    val id: String,
    val title: String,
    val date: String,
    val category: String = "",
    val summary: String = "",
    val htmlBody: String = "",
    val imageUrl: String = ""
)

data class AttendanceLogEntry(
    val id: Long = 0,
    val idMaster: String,
    val courseName: String,
    val mode: String, // "MANUAL_APP", "NOTIF_ACTION", "AUTO_LAST_MINUTE"
    val success: Boolean,
    val message: String,
    val timestampMillis: Long
)

data class LookupOption(
    val id: String,
    val label: String
)

data class MaintenanceStatus(
    val isMaintenance: Boolean = false,
    val message: String = "",
    val latestVersion: String = "",
    val requiredUpdate: Boolean = false
)
