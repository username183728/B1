# GITLS Uploader

APK terpisah untuk: **upload ZIP/file ke GitHub → tunggu build → pasang APK sebagai update** (tanpa hapus-instal).

## Tampilan
Hitam-putih seperti app utama GITLS; otomatis terang/gelap mengikuti sistem. Alur dibuat 3 langkah: **1 Pilih file → 2 Upload & build → 3 Pasang update**. Detail log disembunyikan (tombol "Lihat detail"), tombol "Cara pakai" di atas menjelaskan syarat update.

## Cara pakai
1. Pasang APK ini sekali (build dari repo ini, artifact `GITLS-Uploader-installable-debug-apk`).
2. Buka **Pengaturan GitHub**: isi token, `owner/repo` proyek yang mau dibuild, (opsional) branch & folder tujuan.
3. **Pilih ZIP / file** (atau Bagikan ZIP dari file manager ke GITLS Uploader).
4. **Upload & Build**. Hanya file yang berubah yang dikirim, semuanya dalam **satu commit** (satu build).
5. Setelah build selesai, APK diunduh dan pemasang Android terbuka sebagai **Update**.

"Ambil APK build terakhir" mengunduh APK dari build sukses terbaru tanpa upload.

## Token GitHub
Fine-grained token untuk repo tujuan: **Contents: Read & write**, **Actions: Read**, **Workflows: Read & write**
(Workflows wajib bila ZIP berisi `.github/workflows`). Classic token: scope `repo` + `workflow`.

## Keystore otomatis (disarankan)
Di app, tekan **Siapkan keystore & kirim ke GitHub** (sekali saja per repo). App membuat keystore di HP, lalu mengirim
4 secret (`ANDROID_KEYSTORE_*`, `ANDROID_KEY_*`) ke repo tujuan. Token butuh izin **Secrets: Read & write**
(classic: scope `repo`). Lalu tekan **Simpan cadangan keystore** dan simpan filenya di tempat pribadi.
Untuk repo lain (mis. repo Uploader sendiri), ganti repo di pengaturan lalu tekan tombol yang sama: keystore yang sama dipakai ulang.
Cara manual di bawah tetap berlaku bila tidak mau memakai tombol ini.

## Kenapa dulu harus hapus-instal? (dan solusinya)
Android hanya mau menimpa app jika **tanda tangan sama**. Tanpa keystore tetap, GitHub Actions membuat kunci debug
acak di tiap build, jadi APK baru selalu "berbeda" dari yang terpasang. Solusi: pakai satu keystore tetap.

```bash
keytool -genkeypair -v -keystore gitls-release.keystore -alias gitls \
  -keyalg RSA -keysize 2048 -validity 36500
base64 -w0 gitls-release.keystore    # salin hasilnya
```
Di **Settings → Secrets and variables → Actions** repo proyek (dan repo uploader ini) tambahkan:
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

- Setelah ini, hapus-instal **sekali terakhir** (app lama masih bertanda tangan kunci acak). Update berikutnya mulus.
- **Simpan cadangan keystore.** Kalau hilang, kamu harus hapus-instal lagi.
- GITLS Uploader memeriksa tanda tangan & versi APK sebelum memasang dan memberi tahu bila akan ditolak.

## Opsional: versionCode otomatis naik di proyek utama
Android menolak versi *lebih rendah*. Di workflow proyek utama tambahkan `-PversionCode=$((1000 + ${{ github.run_number }}))`
pada perintah `gradlew` (build.gradle proyek sudah membaca properti `versionCode`).

## Catatan
- Mode "mirror" menghapus file repo yang tidak ada di ZIP (folder `.github/` dilindungi bila ZIP tidak memuatnya).
- Folder pembungkus tunggal di dalam ZIP dibuang otomatis bila berisi proyek Gradle.
- Batas 50 MB per file. Repo harus sudah punya minimal 1 commit.
- Layar dijaga menyala saat proses berjalan; jangan tutup app sampai selesai (upload bersifat atomik: bila terhenti, tidak ada commit setengah jadi).
