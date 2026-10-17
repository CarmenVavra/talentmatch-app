@echo off
REM ---------------------------------------------------------------------------
REM  TalentMatch beenden: stoppt Backend (Port 8080) und Frontend (Port 4200)
REM ---------------------------------------------------------------------------
setlocal

echo.
echo  TalentMatch wird beendet ...

call :killPort 8080 Backend
call :killPort 4200 Frontend

REM Konsolenfenster aus start.bat schliessen (falls noch offen)
taskkill /FI "WINDOWTITLE eq TalentMatch Backend*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq TalentMatch Frontend*" /T /F >nul 2>&1

echo  Fertig.
exit /b 0

:killPort
REM Sprachunabhaengig (netstat zeigt je nach Windows-Sprache LISTENING/ABHOEREN)
set found=
for /f %%p in ('powershell -NoProfile -Command "Get-NetTCPConnection -State Listen -LocalPort %1 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique"') do (
  if not "%%p"=="0" (
    taskkill /PID %%p /T /F >nul 2>&1
    set found=1
  )
)
if defined found (echo  %2 auf Port %1 gestoppt.) else (echo  %2 auf Port %1 lief nicht.)
exit /b 0
