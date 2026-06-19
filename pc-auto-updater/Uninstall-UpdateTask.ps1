<#
.SYNOPSIS
    Removes the "PC Auto-Updater" scheduled task created by Install-UpdateTask.ps1.

.EXAMPLE
    # Run as Administrator
    .\Uninstall-UpdateTask.ps1
#>

$taskName = 'PC Auto-Updater'
$task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($task) {
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    Write-Host "Scheduled task '$taskName' removed." -ForegroundColor Green
} else {
    Write-Host "No scheduled task named '$taskName' was found." -ForegroundColor Yellow
}
