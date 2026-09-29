# MyTools 2.30.1 — Modular Tools Upgrade

This version keeps the existing tool implementations intact and adds the foundation for easier tool management.

## New foundation

- `core/ToolContracts.kt` — common tool contract, metadata, and capability flags.
- `core/ToolPreferences.kt` — persistent favorites and hidden-tool settings.
- `core/ToolHistory.kt` — persistent recent-tool history (20 entries).
- `tools/ToolRegistry.kt` — central catalog of tool IDs, names, categories and metadata.

## Upgrade direction by category

### Calculator
- History
- Copy/share result
- Scientific/programmer modes
- Saved formulas
- Graph/equation tools

### Developer
- JSON/XML/YAML validation
- REST history
- WebSocket client
- cURL generation
- Markdown/SQL tools
- Web Project Builder with Files / Editor / Preview / Console / Server / Export

### Network
- Ping/DNS/ports/traceroute
- SSL/WHOIS/subnet tools
- LAN discovery
- Connection history
- Network dashboard

### File
- Search/filter/sort
- Duplicate and large-file analysis
- Hash comparison
- Batch operations
- Backup/restore
- Storage visualization

### Security
- Password and hash utilities
- Certificate inspection
- URL inspection
- App permission audit
- Security dashboard and score
- Integrity monitoring

### System
- Device/CPU/RAM/storage/battery dashboard
- App manager
- Permission viewer
- Sensor and hardware diagnostics

### Utility
- Text tools
- Encoding tools
- Generators
- Diff tools
- History and export

### Image
- Metadata
- Resize/compress/convert
- OCR
- QR
- Color tools
- Batch processing

### IoT / ESP
- Discovery
- Serial monitor
- GPIO
- Sensors
- HTTP/MQTT
- OTA
- Multi-device dashboard

### Web
- HTTP server
- Wi-Fi hosting
- Request/access logs
- Project profiles
- Live reload

### Finance
- Transactions
- Budget
- Recurring entries
- Savings goals
- Reports
- Import/export

## Global UX standard

Every mature tool should eventually provide only the actions that make sense for it:

`Open → Input → Validate → Result → Copy / Share / Save / Export`

Use one shared Design System so tools do not look like unrelated mini-apps.

## v2.31.0 — Modular Tool State

- ToolRegistry remains the central catalog.
- Favorites are managed by `core/ToolPreferences.kt`.
- Recent tools are managed by `core/ToolHistory.kt`.
- MainActivity performs a one-time migration from the previous comma-separated preference format.
- Home "Favorit" and "Terbaru" now read from the modular stores.
- Existing tool routing and UI layouts are intentionally preserved.
