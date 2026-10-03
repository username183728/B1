# GITLS Project Rules

1. Jangan ubah Keystore.
2. Jangan buat file TXT.
3. Jangan buat file MD lagi. Hanya boleh ada satu file MD, yaitu file ini.

## Signing / Update Rule

Keystore signing harus tetap sama untuk seluruh rilis aplikasi `com.gitls.app`.
Jangan generate keystore baru untuk update berikutnya.

Jika build dilakukan melalui GitHub Actions, gunakan GitHub Secrets:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Jangan commit file keystore ke repository.
