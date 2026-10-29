#!/usr/bin/env bash
# Builds build/tvhome.apk with the plain Android SDK tools (no Gradle).
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}"
TOOLS="$SDK/build-tools/35.0.0"
ANDROID_JAR="$SDK/platforms/android-35/android.jar"
JDK="${TVHOME_JDK:-/c/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot}"
export PATH="$JDK/bin:$PATH"
# The SDK's .bat tools read JAVA_HOME themselves and need it as a Windows path.
export JAVA_HOME="$(cygpath -w "$JDK")"
MIN_SDK=25
TARGET_SDK=30
KEYSTORE=signing/tvhome.jks
KEY_ALIAS=tvhome
KEY_PASS=tvhome-local

rm -rf build
mkdir -p build/gen build/classes build/dex

"$TOOLS/aapt2" compile --dir res -o build/res.zip
"$TOOLS/aapt2" link -I "$ANDROID_JAR" --manifest AndroidManifest.xml \
    --min-sdk-version $MIN_SDK --target-sdk-version $TARGET_SDK \
    -A assets --java build/gen -o build/unsigned.apk build/res.zip

javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID_JAR" -Xlint:-options -Werror \
    -classpath "$TOOLS/core-lambda-stubs.jar" -d build/classes $(find src build/gen -name '*.java')
"$TOOLS/d8.bat" --min-api $MIN_SDK --lib "$ANDROID_JAR" --output build/dex \
    $(find build/classes -name '*.class')

python -I - build/unsigned.apk build/dex/classes.dex <<'PY'
import sys, zipfile
apk, dex = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(apk, "a", zipfile.ZIP_DEFLATED) as archive:
    archive.write(dex, "classes.dex")
PY

"$TOOLS/zipalign.exe" -p -f 4 build/unsigned.apk build/aligned.apk

if [ ! -f "$KEYSTORE" ]; then
    mkdir -p signing
    keytool -genkeypair -keystore "$KEYSTORE" -alias $KEY_ALIAS -keyalg RSA -keysize 2048 \
        -validity 36500 -storepass $KEY_PASS -keypass $KEY_PASS -dname "CN=tvhome" >/dev/null
fi
"$TOOLS/apksigner.bat" sign --ks "$KEYSTORE" --ks-key-alias $KEY_ALIAS \
    --ks-pass pass:$KEY_PASS --key-pass pass:$KEY_PASS --out build/tvhome.apk build/aligned.apk
"$TOOLS/apksigner.bat" verify build/tvhome.apk
echo "Built $(pwd)/build/tvhome.apk"
