# Bit AI — Step 17

## Perubahan
- Perbaikan bug: pengecekan ref branch saat upload GitHub dulu memakai `runCatching{}.getOrNull()`, sehingga error apa pun (401, jaringan putus, rate limit) dianggap "branch belum ada". Sekarang hanya HTTP 404/409 yang berarti ref tidak ada; error lain dilempar dan muncul sebagai pesan error yang jelas.
- Helper baru `logSwallowed()` / `Result.logFailure()` di `util/CommonHelpers.kt`: mencatat error yang sengaja diabaikan ke Logcat (tag GITLS) dengan pola token GitHub disamarkan. Dipasang pada 4 titik best-effort di GithubScreens.kt.

## Tidak dikerjakan (dengan alasan)
- RecyclerView untuk daftar tool: beranda hanya ~11 kartu kategori + 9 studio, dan "Semua Tools" sudah dikelompokkan per kategori, jadi tidak ada daftar panjang yang perlu di-recycle. Memindah ke RecyclerView di dalam ScrollView justru menghilangkan manfaatnya. Optimasi yang lebih tepat: cache view dashboard di showHome (perlu uji visual).
- Sisa `runCatching` di MainActivity.kt: banyak yang sudah ditangani lewat `getOrElse`/`onFailure`; menyentuh semuanya sekaligus berisiko tanpa build.
