<#
.SYNOPSIS
    Automatically updates everything on a Windows PC: Windows itself, drivers,
    and all installed programs (winget, Chocolatey, Microsoft Store, and
    Patch My PC Home).

.DESCRIPTION
    This is a one-shot "update the whole machine" script. Run it manually, or
    register it as a scheduled task with Install-UpdateTask.ps1 to run on a
    schedule with no interaction.

    What it does, in order:
      1. Self-elevates to Administrator (required for drivers / Windows Update).
      2. winget  -> upgrades every app winget knows about.
      3. Chocolatey (if installed) -> "choco upgrade all".
      4. Microsoft Store apps -> triggers the Store update scan.
      5. Patch My PC Home (if installed) -> runs it silently to patch
         third-party apps it manages.
      6. Windows Update + DRIVERS -> via the PSWindowsUpdate module, including
         optional driver updates delivered through Microsoft Update.

    Everything is logged to %ProgramData%\PCAutoUpdater\logs by default.

.PARAMETER IncludeDrivers
    Include driver updates from Windows / Microsoft Update. Default: $true.

.PARAMETER AutoReboot
    Allow Windows Update to reboot automatically if an update requires it.
    Default: $false (a reboot-needed flag is logged instead).

.PARAMETER LogDirectory
    Where to write logs. Default: %ProgramData%\PCAutoUpdater\logs.

.PARAMETER PatchMyPcPath
    Full path to PatchMyPC.exe. If omitted the script searches common
    locations. Patch My PC Home is run with /auto /s (silent, auto-install).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\Update-AllSoftware.ps1

.EXAMPLE
    .\Update-AllSoftware.ps1 -AutoReboot

.NOTES
    Requires Windows 10/11 and an internet connection. Run as Administrator
    (the script will relaunch elevated automatically).
#>

[CmdletBinding()]
param(
    [bool]   $IncludeDrivers = $true,
    [switch] $AutoReboot,
    [string] $LogDirectory   = (Join-Path $env:ProgramData 'PCAutoUpdater\logs'),
    [string] $PatchMyPcPath
)

# ---------------------------------------------------------------------------
# 0. Self-elevate to Administrator
# ---------------------------------------------------------------------------
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal(
    [Security.Principal.WindowsIdentity]::GetCurrent())
$isAdmin = $currentPrincipal.IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host 'Not running as Administrator - relaunching elevated...' -ForegroundColor Yellow
    $argList = @(
        '-NoProfile'
        '-ExecutionPolicy', 'Bypass'
        '-File', "`"$PSCommandPath`""
        '-IncludeDrivers', $IncludeDrivers
    )
    if ($AutoReboot)   { $argList += '-AutoReboot' }
    if ($LogDirectory) { $argList += @('-LogDirectory', "`"$LogDirectory`"") }
    if ($PatchMyPcPath){ $argList += @('-PatchMyPcPath', "`"$PatchMyPcPath`"") }
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $argList
    return
}

# ---------------------------------------------------------------------------
# Logging helpers
# ---------------------------------------------------------------------------
if (-not (Test-Path $LogDirectory)) {
    New-Item -ItemType Directory -Path $LogDirectory -Force | Out-Null
}
$timestamp = Get-Date -Format 'yyyy-MM-dd_HHmmss'
$logFile   = Join-Path $LogDirectory "update_$timestamp.log"

function Write-Log {
    param(
        [string] $Message,
        [ValidateSet('INFO', 'WARN', 'ERROR', 'STEP')]
        [string] $Level = 'INFO'
    )
    $line = '{0} [{1}] {2}' -f (Get-Date -Format 'HH:mm:ss'), $Level, $Message
    switch ($Level) {
        'STEP'  { Write-Host $line -ForegroundColor Cyan }
        'WARN'  { Write-Host $line -ForegroundColor Yellow }
        'ERROR' { Write-Host $line -ForegroundColor Red }
        default { Write-Host $line }
    }
    Add-Content -Path $logFile -Value $line
}

function Invoke-Step {
    param(
        [string]      $Name,
        [scriptblock] $Action
    )
    Write-Log "==== $Name ====" 'STEP'
    try {
        & $Action
        Write-Log "$Name finished." 'INFO'
    }
    catch {
        Write-Log "$Name failed: $($_.Exception.Message)" 'ERROR'
    }
}

Write-Log "PC Auto-Updater starting on $env:COMPUTERNAME (user: $env:USERNAME)." 'INFO'
Write-Log "Log file: $logFile" 'INFO'

# ---------------------------------------------------------------------------
# 1. winget - upgrade all apps
# ---------------------------------------------------------------------------
Invoke-Step 'winget upgrade --all' {
    $winget = Get-Command winget.exe -ErrorAction SilentlyContinue
    if (-not $winget) {
        Write-Log 'winget not found. Install "App Installer" from the Microsoft Store.' 'WARN'
        return
    }
    # Accept agreements so it never prompts; include unknown-version apps.
    $output = & winget upgrade --all --include-unknown --silent `
        --accept-source-agreements --accept-package-agreements 2>&1
    $output | ForEach-Object { Write-Log $_ }
}

# ---------------------------------------------------------------------------
# 2. Chocolatey - upgrade all (only if installed)
# ---------------------------------------------------------------------------
Invoke-Step 'Chocolatey upgrade all' {
    $choco = Get-Command choco.exe -ErrorAction SilentlyContinue
    if (-not $choco) {
        Write-Log 'Chocolatey not installed - skipping.' 'INFO'
        return
    }
    $output = & choco upgrade all -y --no-progress 2>&1
    $output | ForEach-Object { Write-Log $_ }
}

# ---------------------------------------------------------------------------
# 3. Microsoft Store apps - trigger update scan
# ---------------------------------------------------------------------------
Invoke-Step 'Microsoft Store app updates' {
    try {
        $namespace = 'root\cimv2\mdm\dmmap'
        $class     = 'MDM_EnterpriseModernAppManagement_AppManagement01'
        $obj = Get-CimInstance -Namespace $namespace -ClassName $class -ErrorAction Stop
        $result = $obj | Invoke-CimMethod -MethodName UpdateScanMethod
        Write-Log "Store update scan triggered (return code: $($result.ReturnValue))." 'INFO'
    }
    catch {
        Write-Log "Could not trigger Store scan via MDM: $($_.Exception.Message)" 'WARN'
        Write-Log 'The Store will still update apps in the background.' 'INFO'
    }
}

# ---------------------------------------------------------------------------
# 4. Patch My PC Home - silent run for third-party apps
# ---------------------------------------------------------------------------
Invoke-Step 'Patch My PC Home' {
    if (-not $PatchMyPcPath) {
        $candidates = @(
            (Join-Path $env:ProgramFiles        'Patch My PC\PatchMyPC.exe')
            (Join-Path ${env:ProgramFiles(x86)} 'Patch My PC\PatchMyPC.exe')
            (Join-Path $env:LOCALAPPDATA         'Patch My PC\PatchMyPC.exe')
            (Join-Path $env:USERPROFILE          'Downloads\PatchMyPC.exe')
            'C:\PatchMyPC\PatchMyPC.exe'
        )
        $PatchMyPcPath = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    }

    if (-not $PatchMyPcPath -or -not (Test-Path $PatchMyPcPath)) {
        Write-Log 'PatchMyPC.exe not found. Set -PatchMyPcPath to its full path.' 'WARN'
        return
    }

    Write-Log "Running Patch My PC Home silently: $PatchMyPcPath" 'INFO'
    # /auto = auto-install updates, /s = silent (uses settings in PatchMyPC.ini).
    $proc = Start-Process -FilePath $PatchMyPcPath -ArgumentList '/auto', '/s' `
        -PassThru -Wait
    Write-Log "Patch My PC exited with code $($proc.ExitCode)." 'INFO'
}

# ---------------------------------------------------------------------------
# 5. Windows Update + drivers (PSWindowsUpdate)
# ---------------------------------------------------------------------------
Invoke-Step 'Windows Update + drivers' {
    # Make sure NuGet provider + PSWindowsUpdate module are available.
    try {
        [Net.ServicePointManager]::SecurityProtocol = `
            [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    } catch { }

    if (-not (Get-Module -ListAvailable -Name PSWindowsUpdate)) {
        Write-Log 'Installing PSWindowsUpdate module...' 'INFO'
        if (-not (Get-PackageProvider -Name NuGet -ErrorAction SilentlyContinue)) {
            Install-PackageProvider -Name NuGet -MinimumVersion 2.8.5.201 `
                -Force -Scope AllUsers | Out-Null
        }
        Set-PSRepository -Name PSGallery -InstallationPolicy Trusted -ErrorAction SilentlyContinue
        Install-Module -Name PSWindowsUpdate -Force -Scope AllUsers -AcceptLicense
    }
    Import-Module PSWindowsUpdate -ErrorAction Stop

    # Register Microsoft Update so we also get driver + Office updates.
    try {
        Add-WUServiceManager -MicrosoftUpdate -Confirm:$false -ErrorAction SilentlyContinue | Out-Null
    } catch { }

    $params = @{
        MicrosoftUpdate = $true
        AcceptAll       = $true
        Install         = $true
        Verbose         = $false
    }
    if ($AutoReboot) { $params.AutoReboot = $true } else { $params.IgnoreReboot = $true }

    # By default WU does not push drivers unless requested; include them.
    if (-not $IncludeDrivers) {
        $params.NotCategory = 'Drivers'
        Write-Log 'Driver updates excluded by -IncludeDrivers:$false.' 'INFO'
    } else {
        Write-Log 'Driver updates included.' 'INFO'
    }

    $results = Get-WindowsUpdate @params
    if ($results) {
        $results | ForEach-Object {
            Write-Log ("WU: {0} - {1}" -f $_.KB, $_.Title)
        }
    } else {
        Write-Log 'No applicable Windows/driver updates found.' 'INFO'
    }

    if (Get-WURebootStatus -Silent) {
        if ($AutoReboot) {
            Write-Log 'A reboot is required and AutoReboot is enabled.' 'WARN'
        } else {
            Write-Log 'A reboot is REQUIRED to finish installing updates.' 'WARN'
        }
    }
}

Write-Log 'PC Auto-Updater finished. Review the log above for details.' 'STEP'
Write-Log "Full log saved to: $logFile" 'INFO'
