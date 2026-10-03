#!/usr/bin/env bash
set -euo pipefail

echo "== GITLS CI preflight =="

req_file() {
  if [ ! -f "$1" ]; then
    echo "::error::File wajib tidak ditemukan di root repository: $1 (kemungkinan terlewat/terfilter saat upload atau project ada di subfolder)."
    exit 1
  fi
}
req_grep() {
  if ! grep -q -e "$1" "$2"; then
    echo "::error file=$2::Pola wajib tidak ditemukan di $2: $1"
    exit 1
  fi
}

req_file settings.gradle
req_file build.gradle
req_file gradle/wrapper/gradle-wrapper.properties
[ -x ./gradlew ] || chmod +x ./gradlew
req_file app/build.gradle
req_file app/src/main/AndroidManifest.xml

# Fail early if the repository was packaged one directory too deep.
# GitHub Actions expects the Gradle project files at repository root.
if [ -f gitls_final_work/gradlew ] || [ -d gitls_final_work/app ]; then
  echo "::error::Gradle project is nested under gitls_final_work/. Move project files to repository root."
  exit 1
fi

# Build-toolchain invariants used by this project.
req_grep "distributionUrl=.*gradle-8\.11\.1-all\.zip" gradle/wrapper/gradle-wrapper.properties
req_grep "com.android.tools.build:gradle:8\.10\.0" build.gradle
req_grep "ext.kotlin_version = '2\.2\.21'" build.gradle
req_grep "compileSdkVersion 36" app/build.gradle
req_grep "targetSdkVersion 36" app/build.gradle
req_grep "sourceCompatibility JavaVersion.VERSION_17" app/build.gradle
req_grep "targetCompatibility JavaVersion.VERSION_17" app/build.gradle
req_grep "JvmTarget.fromTarget(\"17\")" app/build.gradle
req_grep "JavaLanguageVersion.of(17)" app/build.gradle

# Never allow credentials to be committed accidentally.
# Do not scan source-code strings for PEM marker text because the app
# legitimately contains a key-generator tool.
if grep -RInE --exclude-dir=.git --exclude='*.png' --exclude='*.jpg' \
  '(ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})' .; then
  echo "::error::Possible GitHub token found in repository."
  exit 1
fi

# TEST build exception: this repository intentionally bundles a dedicated
# non-production signing key under signing/. Production repositories should
# keep release signing keys outside source control.
private_key_found=$(find . -type f \
  \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pem' \) \
  -not -path './.git/*' \
  -not -path './signing/gitls-test.keystore' \
  -print -quit || true)
if [ -n "$private_key_found" ]; then
  echo "::error::Unexpected signing/private-key file found: $private_key_found"
  exit 1
fi

for f in signing/gitls-test.keystore signing/keystore.properties; do
  if [ ! -s "$f" ]; then
    echo "::error::$f tidak ada atau kosong. Uploader (GITLS Updater / GitHub Publisher) menyaring file *.keystore secara default; gunakan signing/gitls-test.keystore.b64 (dipulihkan otomatis oleh langkah CI) atau jangan kecualikan file ini."
    exit 1
  fi
done
echo "Bundled TEST signing key: OK"

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

# Resource reference guard: gagal cepat (1 detik) dengan pesan jelas jika ada XML
# yang merujuk resource lokal yang tidak ada, mis. file sisa dari upload lama
# (uploader hanya menambah/menimpa file, tidak pernah menghapus).
python3 - <<'PY'
import pathlib, re, sys, xml.etree.ElementTree as ET
res = pathlib.Path("app/src/main/res")
defined = {}
def add(t, n): defined.setdefault(t, set()).add(n.replace(".", "_"))
for d in res.iterdir():
    if not d.is_dir(): continue
    kind = d.name.split("-")[0]
    for f in d.iterdir():
        if kind == "values":
            if f.suffix != ".xml": continue
            for e in ET.parse(f).getroot():
                n = e.get("name")
                if not n: continue
                tag = e.get("type") if e.tag == "item" else e.tag
                tag = {"string-array": "array", "integer-array": "array"}.get(tag, tag)
                add(tag, n)
        else:
            add(kind, f.stem)
for f in res.rglob("*.xml"):
    for m in re.finditer(r"@\+id/(\w+)", f.read_text(encoding="utf-8")): add("id", m.group(1))
LIB = ("abc_", "mtrl_", "material_", "m3_", "design_", "notification_", "tooltip_", "common_", "androidx_")
CHECK = {"drawable", "mipmap", "color", "layout", "xml", "string", "dimen"}
errors = []
files = list(res.rglob("*.xml")) + [pathlib.Path("app/src/main/AndroidManifest.xml")]
for f in files:
    text = f.read_text(encoding="utf-8")
    for m in re.finditer(r"@(?!\+|android:)(\w+)/([\w.]+)", text):
        t, n = m.group(1), m.group(2).replace(".", "_")
        if t in CHECK and not n.startswith(LIB) and n not in defined.get(t, set()):
            line = text[:m.start()].count("\n") + 1
            errors.append((str(f), line, f"@{t}/{m.group(2)}"))
for f, line, ref in errors:
    print(f"::error file={f},line={line}::Resource {ref} dirujuk tetapi tidak ada. Tambahkan resource-nya atau hapus/perbaiki file ini (mungkin file sisa dari upload lama).")
if errors: sys.exit(1)
print("Resource reference guard: OK")
PY

# Guard: `::lateinitProp.isInitialized` hanya valid di class pemilik properti.
# Di file extension (MainActivity.xxx) ini bikin error kompilasi "Backing field ... is not accessible".
# Gunakan accessor aman seperti `bitAnim` (lihat MainActivity.kt).
python3 - <<'PY'
import pathlib, re, sys
bad = []
for p in pathlib.Path("app/src/main/java").rglob("*.kt"):
    if p.name == "MainActivity.kt":
        continue
    for i, line in enumerate(p.read_text(encoding="utf-8").splitlines(), 1):
        if re.search(r"::\w+\.isInitialized", line):
            bad.append((str(p), i))
for f, i in bad:
    print(f"::error file={f},line={i}::::prop.isInitialized dipakai di luar class pemilik (pakai accessor aman di MainActivity)")
if bad:
    sys.exit(1)
print("lateinit guard: OK")
PY

# Verify application id and version are present.
req_grep 'applicationId "com.gitls.app"' app/build.gradle
req_grep 'versionName' app/build.gradle

echo "Preflight: OK"
