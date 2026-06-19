# PC Auto-Updater (Windows)

A set of PowerShell scripts that update **everything** on your Windows PC in one
go — Windows itself, **drivers**, and **all installed programs** — instead of
just what one widget or store shows you.

It pulls updates from every source it can find:

| Source | What it covers |
| --- | --- |
| **winget** | Most desktop apps (browsers, tools, runtimes, etc.) |
| **Chocolatey** | Anything installed via `choco` (skipped if not installed) |
| **Microsoft Store** | UWP / Store apps |
| **Patch My PC Home** | The third-party apps Patch My PC manages (run silently) |
| **Windows Update + Microsoft Update** | Windows patches **and drivers** |

Everything is logged to `C:\ProgramData\PCAutoUpdater\logs`.

---

## Files

- **`Update-AllSoftware.ps1`** — the main script. Updates everything once.
- **`Update-AllSoftware.cmd`** — double-click launcher for the script.
- **`Install-UpdateTask.ps1`** — registers a scheduled task so it runs
  automatically (weekly by default).
- **`Uninstall-UpdateTask.ps1`** — removes that scheduled task.

---

## Quick start

1. **Download/copy this `pc-auto-updater` folder to your PC.**
2. **Run it once manually** — double-click `Update-AllSoftware.cmd`
   (or right-click `Update-AllSoftware.ps1` → *Run with PowerShell*).
   It will ask for Administrator rights (UAC prompt) — accept it.

That's it for a one-time, update-everything run.

### Run from a PowerShell prompt instead

```powershell
# From inside the pc-auto-updater folder:
powershell -ExecutionPolicy Bypass -File .\Update-AllSoftware.ps1

# Let Windows reboot automatically if an update needs it:
.\Update-AllSoftware.ps1 -AutoReboot
```

---

## Make it automatic (recommended)

Open an **Administrator** PowerShell window in this folder and run:

```powershell
# Weekly, Sunday 3:00 AM (default), reboot allowed if needed:
.\Install-UpdateTask.ps1 -AutoReboot

# Or daily at 2:30 AM:
.\Install-UpdateTask.ps1 -Schedule Daily -Time 02:30 -AutoReboot
```

This creates a scheduled task called **"PC Auto-Updater"** that runs as SYSTEM,
so it works even when you're not logged in.

Test it immediately:

```powershell
Start-ScheduledTask -TaskName "PC Auto-Updater"
```

Remove it later:

```powershell
.\Uninstall-UpdateTask.ps1
```

---

## Patch My PC Home

You said Patch My PC Home is already installed. The script auto-detects
`PatchMyPC.exe` in the usual locations and runs it with `/auto /s`
(auto-install, silent). For the silent run to actually install the apps you
want, open Patch My PC Home once and:

1. Tick the apps you want kept up to date.
2. In **Settings → Advanced**, enable silent/unattended install options.
3. Close it — your choices are saved to `PatchMyPC.ini` and reused on every
   silent run.

If your `PatchMyPC.exe` lives somewhere unusual, point the script at it:

```powershell
.\Update-AllSoftware.ps1 -PatchMyPcPath "D:\Tools\Patch My PC\PatchMyPC.exe"
```

---

## Parameters (`Update-AllSoftware.ps1`)

| Parameter | Default | Description |
| --- | --- | --- |
| `-IncludeDrivers` | `$true` | Include driver updates from Windows/Microsoft Update |
| `-AutoReboot` | off | Allow automatic reboot when an update requires it |
| `-LogDirectory` | `%ProgramData%\PCAutoUpdater\logs` | Where logs are written |
| `-PatchMyPcPath` | auto-detect | Full path to `PatchMyPC.exe` |

---

## Notes & caveats

- **Requires Windows 10/11** and an internet connection.
- The script **self-elevates** — you'll see a UAC prompt the first time.
- The first run installs the **PSWindowsUpdate** module from the PowerShell
  Gallery (used to apply Windows + driver updates). This is a one-time setup.
- **Drivers:** Windows/Microsoft Update only offers drivers the manufacturer has
  published there. For full coverage on laptops/prebuilts, also keep your
  vendor's tool (Dell Command Update, Lenovo Vantage, HP Support Assistant,
  GeForce/AMD/Intel driver apps) — those publish drivers Windows Update doesn't.
- **Reboots:** without `-AutoReboot`, the script never reboots; it just logs
  that a reboot is needed. With `-AutoReboot`, it can restart the PC to finish
  installing updates — don't enable it on a machine you're actively using.
- Review what ran in the log file printed at the end of each run.
