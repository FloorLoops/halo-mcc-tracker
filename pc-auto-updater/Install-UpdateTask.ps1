<#
.SYNOPSIS
    Registers PC Auto-Updater as a Windows Scheduled Task so the whole machine
    updates itself automatically on a schedule (no interaction needed).

.DESCRIPTION
    Creates a task named "PC Auto-Updater" that runs Update-AllSoftware.ps1 as
    SYSTEM with highest privileges. By default it runs weekly; you can change
    the schedule with the parameters below.

.PARAMETER Schedule
    Daily or Weekly. Default: Weekly.

.PARAMETER Time
    Time of day to run, HH:mm (24h). Default: 03:00.

.PARAMETER DayOfWeek
    For weekly schedules, which day. Default: Sunday.

.PARAMETER AutoReboot
    Pass -AutoReboot through to the updater so Windows can reboot if needed.

.EXAMPLE
    # Run as Administrator
    .\Install-UpdateTask.ps1 -Schedule Daily -Time 02:30 -AutoReboot
#>

[CmdletBinding()]
param(
    [ValidateSet('Daily', 'Weekly')]
    [string] $Schedule  = 'Weekly',
    [string] $Time      = '03:00',
    [ValidateSet('Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday')]
    [string] $DayOfWeek = 'Sunday',
    [switch] $AutoReboot
)

# Must run elevated to create a SYSTEM task.
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal(
    [Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $currentPrincipal.IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Error 'Please run this script from an elevated (Administrator) PowerShell prompt.'
    return
}

$taskName   = 'PC Auto-Updater'
$scriptPath = Join-Path $PSScriptRoot 'Update-AllSoftware.ps1'
if (-not (Test-Path $scriptPath)) {
    Write-Error "Cannot find Update-AllSoftware.ps1 next to this script ($scriptPath)."
    return
}

$psArgs = "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$scriptPath`""
if ($AutoReboot) { $psArgs += ' -AutoReboot' }

$action = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument $psArgs

if ($Schedule -eq 'Daily') {
    $trigger = New-ScheduledTaskTrigger -Daily -At $Time
} else {
    $trigger = New-ScheduledTaskTrigger -Weekly -DaysOfWeek $DayOfWeek -At $Time
}

$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' `
    -LogonType ServiceAccount -RunLevel Highest

$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -DontStopOnIdleEnd `
    -RunOnlyIfNetworkAvailable `
    -ExecutionTimeLimit (New-TimeSpan -Hours 4)

# Replace any existing task with the same name.
Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue

Register-ScheduledTask -TaskName $taskName `
    -Description 'Automatically updates Windows, drivers, and all installed programs.' `
    -Action $action -Trigger $trigger -Principal $principal -Settings $settings | Out-Null

Write-Host "Scheduled task '$taskName' created." -ForegroundColor Green
Write-Host "Schedule : $Schedule at $Time$(if($Schedule -eq 'Weekly'){" on $DayOfWeek"})"
Write-Host "Runs     : $scriptPath"
Write-Host ''
Write-Host 'Run it now to test with:' -ForegroundColor Cyan
Write-Host "    Start-ScheduledTask -TaskName `"$taskName`""
