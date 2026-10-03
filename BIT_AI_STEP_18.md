# Bit AI — Step 18 (perbaikan hasil audit)

Belum dikompilasi/diuji di perangkat; build di CI lalu uji manual poin bertanda (uji).

- Back: logika dipusatkan di `handleBack()`; didaftarkan juga lewat `OnBackInvokedCallback` (API 33+) dan `enableOnBackInvokedCallback="true"` (uji di Android 16).
- Keuangan: backup/ekspor memakai `allTx()` (tanpa batas 1000). Restore divalidasi sebelum data lama dihapus (wajib ada `transactions`, tipe/nominal valid). Transaksi berulang tidak lagi tergantung `day_of_month <= hari ini`. `onDowngrade` tidak menghapus data.
- Pengingat: tipe `date` dijadwalkan ulang setelah reboot/update (+`MY_PACKAGE_REPLACED`); pengingat nonaktif tidak aktif lagi setelah reboot.
- Upload GitHub: lewati `.env`, `local.properties`, keystore/p12/pfx, kunci SSH, `.gradle`, `node_modules`; validasi ukuran sebelum jaringan; mode 100755 untuk `gradlew`/`*.sh`; branch baru dicabangkan dari default branch; `Retry-After` dihormati; pesan non-fast-forward dan repo-tak-bisa-diakses lebih jelas.
- Updater: tanda tangan APK dibandingkan dengan app terpasang; `.sha256` yang rusak membatalkan update.
- Quick Tile membuka layar Keuangan. Debug/release memakai keystore sama bila secrets CI diisi (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`).
- Tes unit dasar ditambahkan (`HelpersTest`).

## Sengaja tidak diubah
- `configChanges`/`onSaveInstanceState`: butuh uji visual; menambah `uiMode` tanpa handler akan merusak ganti tema.
- Cleartext HTTP (perlu untuk ESP32), `FileProvider` path, `NEARBY_WIFI_DEVICES`, `REQUEST_INSTALL_PACKAGES`, pemecahan MainActivity, tipe uang `REAL`.
