# Bit AI — Step 12

Step 12 menambahkan runtime process context yang aman.

## Yang ditambahkan
- BitRuntimeContext sekarang mengetahui layar/proses aktif.
- Progress upload GitHub (persen dan file current/total) dicatat sebagai metadata aman.
- Detail tahap terakhir dicatat tanpa credential.
- Saat upload berhasil/gagal/dibatalkan, status proses diperbarui.
- Chat Bit dapat menjawab pertanyaan seperti status/progress/sampai mana berdasarkan konteks real-time.
- Token GitHub, password, cookie, dan credential tetap tidak disimpan atau dikirim ke Bit.

## Contoh
User: "Sekarang prosesnya sampai mana?"
Bit: membaca status aman dari proses upload yang sedang berjalan.

User: "Kenapa gagal?"
Bit: membaca judul/ringkasan error yang sudah disanitasi, bukan token.
