# Bit AI — Step 14

Step 14 fokus pada performa memori dan polling (tanpa mengubah tampilan).

## Perubahan
- Upload GitHub: blob dikirim secara streaming (file -> Base64 -> koneksi, buffer 48 KB). Tidak ada lagi `readBytes()` + String Base64 + body JSON sekaligus di RAM. Retry tetap bekerja karena file dibaca ulang dari awal.
- Foto Color Picker di-decode dengan `inSampleSize` (maks 2048 px) lewat `decodeSampledBitmap()`.
- Steganografi: tanpa `copy()` bitmap, tanpa `flatMap` list per bit, piksel diproses per baris dengan `getPixels`/`setPixels`. Resolusi penuh dipertahankan karena sampling akan merusak bit tersembunyi.
- Decode stego dibuat linear dan berhenti saat pesan lengkap (sebelumnya membuat ulang array + String setiap byte).
- Web server statis: file dikirim dengan stream 16 KB, bukan `readBytes()` per request.
- Polling berhenti saat `onPause` dan lanjut saat `onResume`: runnable modern UI (baterai/Wi-Fi), `ghTicker`, dan polling GitHub Actions.
- `registerReceiver` baterai dipusatkan di `batteryStatusIntent()` (cache 1 detik). Interval halaman baterai 1,5 dtk -> 3 dtk.

## Belum dikerjakan (butuh build + uji di perangkat)
- Pecah MainActivity per fitur, RecyclerView untuk daftar tool, R8/minify, pembatasan cleartext traffic.
