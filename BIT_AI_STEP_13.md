# Bit AI — Step 13

Step 13 menghubungkan notifikasi Error Bit dengan runtime process context.

## Perubahan
- Saat upload GitHub gagal, notifikasi Bit di kanan atas membawa konteks proses yang aman ke chat Bit.
- Konteks berisi operasi, tahap/progress terakhir, jumlah file bila tersedia, judul error, dan ringkasan penyebab.
- Menekan Bit atau label `Error` membuka chat Bit dengan konteks tersebut.
- BitRuntimeContext melakukan sanitasi terhadap pola credential GitHub umum sebelum konteks dipakai Bit.
- Token, password, cookie, dan credential tetap tidak diberikan ke Bit.

## Alur
User → proses gagal → Bit bergerak ke kiri/atas + `Error` → tekan → Chat Bit → Bit sudah membawa konteks error.
