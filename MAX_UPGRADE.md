# GITLS 2.40.7 — Max Upgrade Foundation

This package consolidates the next generation shared engine for the existing tools.

## Added/strengthened in this build
- Shared cancellable batch operation engine.
- Copy/move/delete batch engine with structured failure reports.
- Archive engine: create ZIP, safe extract, archive listing and SHA-256 verification.
- Folder analysis: total size + largest files.
- Duplicate finder: size grouping + SHA-256 matching.
- Large File Finder: sorted top files across accessible roots.
- Storage Analyzer now uses the shared folder-size engine.
- ZIP tool now includes Archive Inspector and SHA-256 verification.
- File Manager adds folder analysis from the current directory.

## Architecture for the remaining tool upgrades
The shared engine is deliberately separated into `GitlsMaxEngine.kt` so editor,
network, security, image, finance, backup and GitHub tools can adopt the same
progress/history/export/error-report patterns without duplicating code in MainActivity.

## Build note
The included wrapper targets Gradle 8.11.1. This environment could not download
that distribution because outbound access to services.gradle.org is unavailable.
The project was therefore not claimed as successfully compiled here.
