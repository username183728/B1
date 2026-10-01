#!/usr/bin/env bash
set -euo pipefail

echo "== GITLS CI preflight =="

test -f settings.gradle
test -f build.gradle
test -f gradle/wrapper/gradle-wrapper.properties
test -x ./gradlew || chmod +x ./gradlew
test -f app/build.gradle
test -f app/src/main/AndroidManifest.xml

# Never allow credentials to be committed accidentally.
# Do not scan source-code strings for PEM marker text because the app
# legitimately contains a key-generator tool.
if grep -RInE --exclude-dir=.git --exclude='*.png' --exclude='*.jpg' \
  '(ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})' .; then
  echo "::error::Possible GitHub token found in repository."
  exit 1
fi

# Actual private-key files should never be committed.
if find . -type f \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pem' \) \
  -not -path './.git/*' -print -quit | grep -q .; then
  echo "::error::Signing/private-key file found in repository."
  exit 1
fi

# Basic XML validation for Android resources.
python3 - <<'PY'
import pathlib, xml.etree.ElementTree as ET
root = pathlib.Path(".")
bad = []
for p in root.glob("app/src/main/**/*.xml"):
    try:
        ET.parse(p)
    except Exception as e:
        bad.append((str(p), str(e)))
if bad:
    for p, e in bad:
        print(f"::error file={p}::{e}")
    raise SystemExit(1)
print("XML validation: OK")
PY

# Verify application id and version are present.
grep -q 'applicationId "com.gitls.app"' app/build.gradle
grep -q 'versionName' app/build.gradle

echo "Preflight: OK"
