@echo off
REM ===================================================================
REM  PC Auto-Updater - double-click launcher
REM  Runs Update-AllSoftware.ps1 (which self-elevates to Administrator).
REM ===================================================================
echo Starting PC Auto-Updater...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Update-AllSoftware.ps1"
echo.
echo Done. Press any key to close.
pause >nul
