@echo off
REM ---------------------------------------------------------------------------
REM  TalentMatch starten: Backend (Spring Boot, Port 8080) + Frontend (Angular, Port 4200)
REM  Optional vorher setzen:  set OPENAI_API_KEY=sk-...   (ohne Key sind KI-Analysen deaktiviert)
REM ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

echo.
echo  TalentMatch wird gestartet ...
echo.

where java >nul 2>&1 || (echo [Fehler] Java wurde nicht gefunden. Bitte JDK 21+ installieren. & exit /b 1)
where npm  >nul 2>&1 || (echo [Fehler] npm wurde nicht gefunden. Bitte Node.js 20+ installieren. & exit /b 1)

REM Fenster, die vor dem Setzen der Umgebungsvariable geoeffnet wurden, kennen den Key noch nicht:
REM dann direkt aus den Windows-Umgebungsvariablen (Benutzer, sonst System) lesen.
if "%OPENAI_API_KEY%"=="" (
  for /f "usebackq delims=" %%k in (`powershell -NoProfile -Command "@([Environment]::GetEnvironmentVariable('OPENAI_API_KEY','User'), [Environment]::GetEnvironmentVariable('OPENAI_API_KEY','Machine')) | Where-Object { $_ } | Select-Object -First 1"`) do set "OPENAI_API_KEY=%%k"
)

if "%OPENAI_API_KEY%"=="" (
  echo  [Hinweis] OPENAI_API_KEY ist nicht gesetzt - die KI-Analyse neuer Lebenslaeufe ist deaktiviert.
  echo            Demo-Daten mit fertigen Analysen sind trotzdem verfuegbar.
  echo.
)

if not exist "frontend\node_modules" (
  echo  Installiere Frontend-Abhaengigkeiten ...
  pushd frontend
  call npm install || (popd & echo [Fehler] npm install fehlgeschlagen. & exit /b 1)
  popd
)

start "TalentMatch Backend" /D "%~dp0backend" cmd /k ".\mvnw.cmd -q spring-boot:run"
start "TalentMatch Frontend" /D "%~dp0frontend" cmd /k "npm start"

echo  Backend:  http://localhost:8080
echo  Frontend: http://localhost:4200
echo.
echo  Warte, bis das Frontend erreichbar ist ...
set /a tries=0
:wait
set /a tries+=1
curl -s -o nul http://localhost:4200 && goto open
if %tries% geq 90 goto timeout
REM 2 Sekunden warten (ping statt timeout: funktioniert auch ohne interaktive Konsole)
ping -n 3 127.0.0.1 >nul
goto wait

:open
start "" http://localhost:4200
echo  TalentMatch laeuft. Beenden mit stop.bat
exit /b 0

:timeout
echo  [Hinweis] Das Frontend antwortet noch nicht - bitte die Fenster "TalentMatch Backend/Frontend" pruefen.
exit /b 0
