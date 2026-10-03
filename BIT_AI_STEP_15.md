# Bit AI — Step 15

Modularisasi bertahap MainActivity: lapisan jaringan GitHub dipindah ke `feature/github/GithubApi.kt`.

## Perubahan
- 21 fungsi dipindah sebagai extension `MainActivity.*` (pemanggil tidak berubah): githubRequest, githubRequestRetry, githubRequestRaw, ghHeaders, encodePath, pushFilesToGitHub, uploadFolderToGitHub, unzipSafeForGithub, analyzeGithubZip, fungsi hapus tree (ghDelete*), ghCollectGithubContentFiles, helper error/jaringan (ghIsTokenAuthError, ghIsNetworkError, ghInternetAvailable, ghFriendlyError, ghDeleteError), ghFormatBytes, intentJobId.
- `ghCancelled` diubah dari `private` ke `internal` agar bisa dibaca modul baru.
- MainActivity.kt berkurang ~570 baris (16.627 -> 16.058).

## Belum
- Sisi UI GitHub (layar, dialog, progress) masih di MainActivity; state `gh*` tetap field class (extension tidak bisa punya field).
- Belum dikompilasi: build di CI/Android Studio dulu. Jika ada error "unresolved reference", biasanya import atau modifier visibilitas.
