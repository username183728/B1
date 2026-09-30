# Ghost Floating / Borderless Header Update

## Perubahan UI
- Hamburger, Search, dan Profile memakai tombol transparan tanpa background/lingkaran container.
- Ditambahkan shadow/elevation tipis pada outline ikon agar tetap terbaca di atas konten.
- Ketiga kontrol header tetap berada di luar ScrollView sehingga fixed/sticky saat konten digulir.
- Search icon dinaikkan sedikit (`translationY=-2dp`) agar sejajar dengan Profile dan Hamburger.
- Judul header `GITLS` dan subtitle `Semua alat dalam satu aplikasi` disembunyikan dari header.
- Search icon membuka/fokus ke Search field dan otomatis menampilkan Search kembali jika sedang di-Snap-hide.
- Search icon hanya ditampilkan pada halaman yang memang memiliki pencarian global: Home dan Favorit.

## Kompatibilitas
- ID lama `tvTitle`, `tvSubtitle`, `homeMenu`, dan `homeProfile` tetap dipertahankan agar state/navigation lama tidak rusak.
- `app_name` tidak diubah karena itu adalah nama aplikasi Android/label sistem, bukan teks header.
- Snap/Enter Always dari update sebelumnya tetap digunakan.
