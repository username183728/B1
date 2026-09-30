# GITLS 2.40.7 - Background Delete Update

Penghapusan semua file GitHub sekarang dijalankan melalui `GithubDeleteService` sebagai Foreground Service.

- Tombol **Sembunyikan** menutup dialog saja.
- Proses tetap berjalan saat Activity diminimalkan/ditinggalkan.
- Notification menampilkan persen, tahap, detail file, dan tombol Batalkan.
- Notification tetap dapat dibuka untuk kembali ke GITLS.
- Jika Git Data API gagal/404, service memakai fallback Contents API seperti implementasi sebelumnya.
- Ditambahkan permission `FOREGROUND_SERVICE_DATA_SYNC`.
