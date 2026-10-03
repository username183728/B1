BIT AI STEP 29 - Lottie asset path fix

Fix utama:
- Semua animasi Bit berada di app/src/main/assets/bit/
- BitAnimationController.kt memakai bit/bit_idle.json untuk idle.
- playAsset(asset) memakai bit/$asset agar Lottie tidak mencari JSON di root assets.
- Animasi ANGRY tidak digunakan.
- Animasi: idle, tap, bump, dizzy, error, love, sad, wink.

Catatan: file JSON harus tetap berada di app/src/main/assets/bit/ saat di-upload ke GitHub.
