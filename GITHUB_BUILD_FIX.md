# GITLS 2.40.2 — GitHub Build Fix

The failed build was caused by obsolete references to `imageInfoResult` and
`imageToolsResult` in `MainActivity.kt`. Those callbacks no longer exist after
Image Studio was merged, so Kotlin cannot infer the lambda types and reports:

`Cannot infer type for this parameter`
`Not enough information to infer type argument for 'R'`
`Unresolved reference 'imageInfoResult'`
`Unresolved reference 'imageToolsResult'`

This package removes those stale references and uses a root-level GitHub
Actions workflow, matching a repository where `gradlew`, `app/`, and
`.github/` are at the repository root.

The workflow also checks for those stale references before Gradle starts.
