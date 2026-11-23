# DinusRealm

Aplikasi Android native yang ringan dan cepat untuk membantu mahasiswa UDINUS memantau jadwal kuliah, presensi online, akademik, serta tagihan langsung dari HP.

> [!NOTE]
> Proyek ini adalah klien alternatif tidak resmi (*unofficial client*) dan tidak berafiliasi dengan pihak resmi Universitas Dian Nuswantoro (UDINUS).

---

## Unduh Aplikasi (Android 8.0+)

Unduh APK rilis resmi (**v1.0.0**) lewat halaman Releases:

- [DinusRealm v1.0.0 Release](https://github.com/E6nola/DinusRealm/releases/latest)

---

## Fitur Utama

- **Jadwal Offline & Widget Beranda**: Menyimpan jadwal harian dan mingguan (**Senin–Jumat**) secara lokal di SQLite untuk akses cepat tanpa koneksi internet, dilengkapi widget layar utama.
- **Absen Langsung dari Notifikasi**: Saat jam kuliah aktif, pemantau latar belakang memeriksa status kelas tiap **45 detik**. Begitu dosen membuka presensi, notifikasi muncul dengan tombol **`Absen Sekarang` / `Attend Now`** untuk mengirim presensi langsung tanpa perlu membuka aplikasi (mengikuti suara notifikasi bawaan HP).
- **Pengingat Kelas T-30 Menit & Auto-Absen Opsional**: Peringatan ruang dan dosen 30 menit sebelum kelas dimulai, serta opsi auto-absen detik terakhir menjelang kelas selesai (bisa diatur 30–180 detik).
- **Layanan Akademik & Keuangan**: Presensi, konfirmasi RPS, KRS, KHS & IPK, jadwal ujian UTS/UAS, tagihan & salin Virtual Account bank, heregistrasi, kuisioner, pengumuman akademik, dan profil mahasiswa.
- **Kustomisasi Tampilan**: Pilihan bahasa **English** (default) dan **Indonesia**, nama panggilan kustom (*Display Name*), serta **Light Mode / Dark Mode** (*System*, *Light*, *Dark*).
- **Tes Simulasi Notifikasi**: Uji tombol **`Absen Sekarang`** secara lokal dari menu Pengaturan tanpa mengirim data ke server SiAdin.

---

## Keamanan & Privasi

- NIM, kata sandi terenkripsi, dan token sesi disimpan **hanya secara lokal di perangkat** menggunakan Android Keystore (`AES/GCM`) dan SQLite lokal.
- Komunikasi jaringan dilakukan langsung ke endpoint resmi `https://api.dinus.ac.id/api/v1/` tanpa server perantara pihak ketiga.

---

## Build dari Source

### Prasyarat
- **JDK 17**
- **Android SDK** (`compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`)

### Langkah Build
1. Clone repository ini lalu buat `local.properties` di root proyek:
   ```properties
   sdk.dir=C:\\Path\\Ke\\Android\\Sdk
   ```
2. *(Opsional untuk release build)* Buat `keystore/signing.properties` beserta file `.jks` milik sendiri:
   ```properties
   storeFile=dinusrealm-release.jks
   storePassword=PASSWORD_KEYSTORE
   keyAlias=ALIAS_KEY
   keyPassword=PASSWORD_KEY
   ```
3. Jalankan unit test dan build APK:
   ```powershell
   .\gradlew.bat testDebugUnitTest assembleDebug
   ```
   atau untuk APK release:
   ```powershell
   .\gradlew.bat testDebugUnitTest assembleRelease
   ```
