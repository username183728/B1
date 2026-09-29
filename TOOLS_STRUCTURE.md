# MyTools — Tool Structure

`MainActivity.kt` is now focused on app/navigation infrastructure. Tool entry points are grouped into `app/src/main/java/com/example/aidetest/tools/`.

## Categories

- `calculator/` — calculators and conversions
- `developer/` — editor, web project, GitHub, REST/WebSocket, formats
- `network/` — Wi-Fi, DNS, ping, ports, IP, SSL, scanners
- `system/` — device, storage, apps, battery, clipboard
- `utility/` — text, encoding, hash, random, time utilities
- `security/` — password, encryption, certificates, security tools
- `image/` — color, OCR, image, QR
- `iot/` — ESP/IoT tools
- `web/` — HTTP server and Wi-Fi hosting
- `finance/` — finance pages
- `workspace/` — studios/workspace/plugin customization
- `app/` — APK/help/unit-converter tools

## Important

The first refactor deliberately moves **tool entry points**, not every helper method. Shared UI/file/network helpers remain in `MainActivity.kt` as internal infrastructure so behavior is easier to preserve during the migration. `ToolRegistry.kt` is the human-readable catalog of tool IDs and categories.
