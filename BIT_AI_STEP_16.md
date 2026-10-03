# Bit AI — Step 16

## Perubahan
- UI GitHub dipindah ke `feature/github/GithubScreens.kt` (53 fungsi extension `MainActivity.*`): pengaturan, browser repo, dialog hapus, progress, hasil, layar Actions, upload ZIP/folder. MainActivity.kt: 16.627 -> ~14.390 baris (bersama Step 15).
- Member private yang dipakai modul baru dijadikan `internal`: applyBitFaceTheme, bitPendingChatMessage, bitProcessErrorView, bitProcessFace, createGhBitProcessAssistant, playBitThinking, showBitChat, showGhBitProcessError.
- Download log Actions kini streaming ke file `.part` lalu di-rename (tidak lagi `readBytes()` penuh); cek magic ZIP dari 4 byte awal.
- Polling sensor ESP dijeda saat `onPause` dan lanjut di `onResume` bila masih aktif.
- R8 aktif: `minifyEnabled true`, `shrinkResources true`. Seluruh `com.example.aidetest.**` di-keep dan `-dontobfuscate` agar aman terhadap refleksi dan stack trace tetap terbaca.
- `kotlin.incremental=true` (tetap in-process).

## Tidak diubah
- `usesCleartextTraffic`: network_security_config tidak bisa membatasi ke rentang IP LAN (hanya domain/IP persis), jadi membatasinya akan memutus tool ESP32.
- RecyclerView / cache dashboard: perubahan UI besar, perlu uji visual di perangkat.

## Jika build/rilis bermasalah
- Error "unresolved reference" di file feature/github: biasanya import/visibilitas.
- Crash hanya di APK release: set `minifyEnabled false` untuk membandingkan, lalu perketat aturan keep.
