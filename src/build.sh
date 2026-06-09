#!/bin/bash
# UNSC Terminal (Halo MCC tracker) — APK build, no Gradle. Run from src/.
# Needs Android build-tools (aapt2,d8,zipalign,apksigner), platform android.jar, ecj.jar (JRE-only compile).
# Toolchain downloads fresh each VM (see Companion-App/APP-SPEC.md recipe). Override paths via env.
set -e
SDK=${SDK:-/tmp/sdk}
BT=${BT:-$(ls -d $SDK/android-1*/ 2>/dev/null | grep -E 'android-1[0-9]/' | head -1)}
[ -x "$BT/aapt2" ] || BT=$(dirname $(find $SDK -name aapt2 | head -1))
AJ=${AJ:-$(find $SDK -name android.jar | head -1)}
ECJ=${ECJ:-$SDK/ecj.jar}
KS=${KS:-../../../Apps/HaloTracker/halotracker-debug.ks}
OUTNAME=${1:-UNSCTerminal.apk}
PKG_PATH=four/parliament/halotracker

OUT=build; rm -rf $OUT; mkdir -p $OUT/gen $OUT/classes $OUT/apk $OUT/res/mipmap-xxhdpi $OUT/assets
cp ic_launcher.png $OUT/res/mipmap-xxhdpi/ic_launcher.png
cp data.json $OUT/assets/data.json

# 1. resources
"$BT/aapt2" compile --dir $OUT/res -o $OUT/res.zip
"$BT/aapt2" link -o $OUT/base.apk -I "$AJ" --manifest AndroidManifest.xml -A $OUT/assets --java $OUT/gen $OUT/res.zip

# 2. java -> class -> dex  (ecj: JRE-only compiler, source/target 8, no lambdas)
java -jar "$ECJ" -encoding UTF-8 -source 8 -target 8 -nowarn -bootclasspath "$AJ" -d $OUT/classes \
  $OUT/gen/$PKG_PATH/R.java MainActivity.java
"$BT/d8" --lib "$AJ" --release --output $OUT/apk $(find $OUT/classes -name '*.class')

# 3. assemble
cp $OUT/base.apk $OUT/app-unsigned.apk
(cd $OUT/apk && zip -q ../app-unsigned.apk classes.dex)

# 4. align + sign (debug key)
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias halo -keyalg RSA -keysize 2048 \
  -validity 10000 -storepass android -keypass android -dname "CN=UNSC Terminal" 2>/dev/null
"$BT/zipalign" -f 4 $OUT/app-unsigned.apk $OUT/app-aligned.apk
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$OUTNAME" $OUT/app-aligned.apk
"$BT/apksigner" verify "$OUTNAME" && echo "BUILD OK -> $OUTNAME"
