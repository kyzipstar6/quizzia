@echo off
setlocal

cd /d "%~dp0"
set PORT=3000

where py >nul 2>nul
if %errorlevel%==0 (
	start "Quiz Server (Python)" powershell -NoExit -Command "Set-Location -LiteralPath '%~dp0'; py -m http.server %PORT%"
) else (
	where python >nul 2>nul
	if %errorlevel%==0 (
		start "Quiz Server (Python)" powershell -NoExit -Command "Set-Location -LiteralPath '%~dp0'; python -m http.server %PORT%"
	) else (
		start "Quiz Server (Node)" powershell -NoExit -Command "Set-Location -LiteralPath '%~dp0'; npx --yes http-server . -p %PORT% -c-1"
	)
)

timeout /t 2 >nul
start "" "http://localhost:%PORT%/index.html"