# GitHub Build — GITLS 2.34.0

## What the workflow does

### `Build GITLS`
Runs automatically on pushes to `main`/`master` and on pull requests. It can also be started manually from **Actions → Build GITLS → Run workflow**.

The workflow:
1. Checks the project structure.
2. Installs the Android API 36 platform.
3. Uses Java 17 and Gradle 8.11.1.
4. Builds the debug APK.
5. Runs JVM unit tests.
6. Builds the release APK.
7. Builds a Play Store-ready **AAB artifact**.
8. Uploads APK/AAB files as GitHub Actions artifacts.

## GitHub Release

Create a tag such as `v2.34.0` (or run the release workflow manually). The release workflow builds and attaches the release APK and AAB to a GitHub Release.

## Important: signing

The current project does **not** contain a production signing key. The generated release APK/AAB is therefore not a production-signed Play Store artifact.

For Play Console publication, configure a secure Android signing key through GitHub Actions secrets and add the signing configuration to the release workflow. Never commit a keystore, passwords, or signing secrets to the repository.

## Recommended flow

```text
Edit project
   ↓
Push to GitHub
   ↓
Build GITLS workflow
   ↓
Check + test + build
   ↓
APK/AAB artifacts
   ↓
Test APK
   ↓
Configure signing
   ↓
Create signed AAB
   ↓
Google Play Console
```
