<# Windows on-host APK build for UNSC Terminal — mirrors build.sh.
   Uses the local Android SDK (build-tools 34) + Zulu JDK javac. Run from src\.
   Usage: powershell -File build-win.ps1 <OUTNAME.apk>  (PREMIUM edited in MainActivity.java beforehand) #>
param([string]$OutName="UNSCTerminal.apk")
$ErrorActionPreference="Stop"
$SDK="C:\Users\egiii\AppData\Local\Android\Sdk"
$BT="$SDK\build-tools\34.0.0"      # aapt2/zipalign/apksigner
$BT8="$SDK\build-tools\35.0.0"     # d8 (34's d8 NPEs on Zulu-26 bytecode)
$AJ="$SDK\platforms\android-34\android.jar"
$KS="C:\Users\egiii\Claude\Apps\HaloTracker\halotracker-debug.ks"
$PKG="four\parliament\halotracker"
$src=$PSScriptRoot
Set-Location $src
$OUT="build"
if(Test-Path $OUT){ Remove-Item $OUT -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$OUT\gen","$OUT\classes","$OUT\apk","$OUT\res\mipmap-xxhdpi" | Out-Null
Copy-Item ic_launcher.png "$OUT\res\mipmap-xxhdpi\ic_launcher.png"

# 1. resources — NO -A here; Windows aapt2 stores nested asset paths with backslashes,
#    which Android's AssetManager can't resolve. We inject assets ourselves (forward slashes).
& "$BT\aapt2.exe" compile --dir "$OUT\res" -o "$OUT\res.zip"
& "$BT\aapt2.exe" link -o "$OUT\base.apk" -I "$AJ" --manifest AndroidManifest.xml --java "$OUT\gen" "$OUT\res.zip"

# 2. compile (javac; source/target 8 to match ecj build — no lambdas in source anyway)
& javac -source 8 -target 8 -encoding UTF-8 -nowarn -bootclasspath "$AJ" -d "$OUT\classes" "$OUT\gen\$PKG\R.java" MainActivity.java
# jar the classes so d8 takes ONE input (Windows command line overflows with 60+ .class paths)
Push-Location "$OUT\classes"; & jar cf "..\classes.jar" .; Pop-Location
& "$BT8\d8.bat" --min-api 26 --lib "$AJ" --release --output "$OUT\apk" "$OUT\classes.jar"
if(-not (Test-Path "$OUT\apk\classes.dex")){ throw "d8 produced no classes.dex" }

# 3. assemble — inject classes.dex + all assets with EXPLICIT forward-slash entry names
Copy-Item "$OUT\base.apk" "$OUT\app-unsigned.apk"
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$apk=[System.IO.Compression.ZipFile]::Open((Resolve-Path "$OUT\app-unsigned.apk"), 'Update')
function Add-Entry($zip,$file,$name){ $e=$zip.CreateEntry($name, [System.IO.Compression.CompressionLevel]::Optimal); $s=$e.Open(); $b=[System.IO.File]::ReadAllBytes($file); $s.Write($b,0,$b.Length); $s.Close() }
Add-Entry $apk "$OUT\apk\classes.dex" "classes.dex"
Add-Entry $apk (Resolve-Path data.json) "assets/data.json"
foreach($f in Get-ChildItem icons\*.png){ Add-Entry $apk $f.FullName ("assets/icons/"+$f.Name) }
foreach($f in Get-ChildItem gameicons\*.png){ Add-Entry $apk $f.FullName ("assets/gameicons/"+$f.Name) }
foreach($f in Get-ChildItem ranks\*.png){ Add-Entry $apk $f.FullName ("assets/ranks/"+$f.Name) }
$apk.Dispose()
# verify: dex + forward-slash assets present
$zc=[System.IO.Compression.ZipFile]::OpenRead((Resolve-Path "$OUT\app-unsigned.apk"))
$hasDex=[bool]($zc.Entries | Where-Object FullName -eq 'classes.dex')
$nIcons=($zc.Entries | Where-Object { $_.FullName -like 'assets/icons/*' }).Count
$nGame=($zc.Entries | Where-Object { $_.FullName -like 'assets/gameicons/*' }).Count
$nRank=($zc.Entries | Where-Object { $_.FullName -like 'assets/ranks/*' }).Count
$zc.Dispose()
if(-not $hasDex){ throw "classes.dex missing" }
if($nIcons -ne 700){ throw "expected 700 icons, got $nIcons" }
if($nGame -ne 7){ throw "expected 7 game icons, got $nGame" }
if($nRank -ne 30){ throw "expected 30 rank emblems, got $nRank" }
Write-Host "assets OK: dex + $nIcons icons + $nGame game-arts + $nRank rank-emblems (forward-slash)"

# 4. align + sign
& "$BT\zipalign.exe" -f 4 "$OUT\app-unsigned.apk" "$OUT\app-aligned.apk"
& "$BT\apksigner.bat" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out $OutName "$OUT\app-aligned.apk"
& "$BT\apksigner.bat" verify $OutName
Write-Host "BUILD OK -> $OutName"
