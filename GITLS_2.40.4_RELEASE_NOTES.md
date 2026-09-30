# GITLS 2.40.3

UI/UX update based on the latest navigation and module review.

## Changes
- Home dashboard now exposes pinned/favorite tools as a quick horizontal rail.
- Editor: line numbers, Copy, Share, Find, and Find & Replace quick actions.
- Finance Dashboard: budget usage progress bars with clear warning states.
- Color Picker: first-use onboarding explaining overlay and screen-capture permissions.
- Settings/version labels updated to 2.40.3.
- GitHub Actions build remains configured for root-level Android project.
- Stale Image Studio callback references remain blocked by a CI preflight check.

## Build
Run `Build APK` from GitHub Actions. Debug builds run on push to `main`; release builds can be started manually with the `release` workflow option.

Note: this environment cannot download the Gradle 8.11.1 distribution, so the APK was not locally compiled here. The source and workflow were checked structurally, and CI will perform the actual Android build.


## 2.40.4 Stability Fix
- Startup maintenance is now isolated with defensive error handling.
- Added a safe fallback screen if the home UI ever fails during launch, preventing an Activity crash back to the launcher.
- Update check is isolated from startup UI.
- Release builds keep minification disabled to avoid reflection-related release-only crashes in optional tools.
- Version code: 52.

## 2.40.5 PDF Export Fix
- Ekspor PDF laporan bulanan dan struk transaksi kini dibuat di background thread, lalu dibagikan di UI thread. Mencegah ANR saat data transaksi besar.
- Version code: 53.

## 2.40.6 Loading & Motion
- Splash screen resmi (androidx.core-splashscreen) untuk cold start tanpa layar kosong.
- Progress bar unduhan update dengan persentase dan ukuran MB, plus tombol Batal yang benar-benar menghentikan unduhan.
- Animasi otomatis mati bila skala durasi animator sistem = 0 (Reduce Motion), selain toggle "Animasi UI".
- Haptic feedback halus pada tombol; animasi tekan mengikuti pengaturan animasi.
- Ekspor PDF dicegah dobel-klik saat masih diproses.
- Version code: 54.

## 2.40.7 GitHub Delete Hardening
- Retry otomatis untuk secondary rate limit (403/429) dan konflik commit 409 pada hapus file.
- Hapus massal: bila listing tree GitHub terpotong (repo besar), otomatis pakai jalur Contents API agar tidak ada file tertinggal.
- Jeda kecil antar penghapusan di jalur Contents API.
- Kegagalan hapus kini tampil sebagai dialog dengan penyebab dan solusi (scope token, branch protection, SHA tidak cocok), bukan toast singkat.
- Version code: 55.
