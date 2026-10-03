# Bit AI — Step 19 (deteksi, analisis, dan perbaikan error di Text Editor)

Belum dikompilasi/diuji di perangkat; build di CI lalu uji manual poin bertanda (uji).

## Baru
- `feature/text/CodeDiagnostics.kt`: pemeriksa struktur lokal untuk JS, HTML (termasuk `<script>`/`<style>` di dalamnya), CSS, dan JSON. Murni Kotlin, bisa diuji unit.
  - JS/CSS: kurung tidak berpasangan/salah tutup, string/template/komentar tak ditutup, `debugger` tertinggal. Regex literal, string, dan komentar dilewati.
  - HTML: tag tak ditutup, tag penutup tanpa pembuka, salah urutan nesting, `>` hilang, id ganda, `<img>` tanpa `alt`.
  - JSON (lebih ketat dari `org.json`): koma berlebih/kurang, kutip tunggal, key tanpa kutip, `True/None/undefined`, komentar, kurung tak ditutup, teks setelah JSON.
  - Setiap isu membawa nomor baris/kolom, pesan Indonesia, dan (bila aman) `Fix` berisi daftar edit.
- `feature/text/EditorDiagnostics.kt`: garis bawah merah (error) / kuning (peringatan) di editor, debounce 500 ms, ringkasan di label status ("✖ 2 error").
- Tombol **Cek** di baris aksi cepat editor: daftar masalah (ketuk untuk lompat ke baris) dan **Perbaiki (N)**.
- Pratinjau sebelum/sesudah per perbaikan; baru diterapkan setelah **Terapkan**.
- `BitActionBridge`: aksi `EDITOR_DIAGNOSE` dan `EDITOR_FIX_PREVIEW` (hanya ID aksi, tanpa isi kode atau credential).
- Bit memahami perintah seperti "cek error", "periksa kode", "perbaiki kode", "validasi json". Jawabannya berisi chip aksi yang tampil di bawah balon chat.
- Pesan diagnosis tidak menyalin isi kode, hanya nomor baris dan karakter struktural.

## Perbaikan
- `answerBitLocally`: variabel `lower` dipakai sebelum dideklarasikan (error kompilasi); deklarasinya dipindah ke atas.

## Batasan (sengaja)
- Pemeriksaan struktur dasar, bukan parser bahasa penuh: tidak mendeteksi variabel tak terdefinisi, tipe, atau logika.
- Dari layar chat Bit, perbaikan hanya bisa diterapkan bila file sudah punya nama (tersimpan); hasilnya langsung ditulis ke file itu.
- Perbaikan untuk kurung hilang bersifat heuristik; selalu cek pratinjau.

## Uji manual
- (uji) JSON dengan koma berlebih, lalu Cek -> Perbaiki -> Terapkan -> Undo.
- (uji) HTML dengan `<div><span>` tak ditutup.
- (uji) Ketik di editor: garis merah muncul sekitar 0,5 detik setelah berhenti mengetik.
- (uji) Di chat Bit: "cek error di editor", ketuk chip "Pratinjau perbaikan".
- (uji) Tema gelap/terang, dan file besar (>200.000 karakter dilewati).

## Tambahan: animasi status GitHub Actions
- Layar "Cek Actions" kini memakai `StepStateView` yang sama dengan layar Proses Upload: cincin abu-abu (menunggu), cincin hitam berputar (berjalan), lingkaran hitam dengan centang digambar (selesai), lingkaran merah dengan silang (gagal).
- Berlaku untuk baris status "Actions sedang berjalan…", header job, dan setiap step.
- Step yang belum jalan sekarang tampil sebagai cincin abu-abu (sebelumnya semua step yang belum selesai ikut berputar), karena status `pending`/`in_progress` dari GitHub ikut dibaca.
- View dipakai ulang antar polling (3 detik): putaran tidak reset dan animasi centang hanya muncul saat status berubah.
- (uji) Jalankan Actions dan amati: step berurutan pending -> berputar -> centang; job gagal menampilkan silang merah.

## Tambahan: pratinjau animasi Lottie di tool JSON
- `feature/text/LottiePreview.kt`: `LottieJson.looksLikeLottie()` mengenali JSON Lottie (ada `layers`, `w`, `h`, `fr`/`op`) dan `showLottiePreview()` memutarnya memakai library Lottie yang sudah ada (tanpa dependensi baru, berjalan lokal).
- Layar pratinjau: info animasi (nama, ukuran, fps, durasi, jumlah layer), Putar/Jeda, Ulang, Loop, kecepatan (0.5x–2x), slider posisi, dan latar putih/gelap/kotak-kotak untuk melihat transparansi.
- Cara pakai: di tool JSON (editor mode JSON) tempel atau buka file Lottie, lalu tekan **Preview** di toolbar bawah. Di halaman "JSON Tools" lama ada tombol **Preview Lottie**. JSON biasa tetap tampil seperti sebelumnya.
- Bit mengenali kata "lottie" dan membuka tool JSON.
- Batas: file > 8 juta karakter ditolak. Gambar eksternal (URL/path) di dalam Lottie tidak dimuat; gambar base64 tertanam tetap jalan. Format `.lottie` (zip) belum didukung, hanya `.json`.
- (uji) Buka file Lottie dari LottieFiles lewat Buka, tekan Preview; coba Jeda, geser slider, ganti latar. Coba juga JSON non-Lottie dan Lottie rusak (harus muncul pesan error merah, bukan crash).
