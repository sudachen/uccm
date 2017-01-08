@echo off
cd %~dp0

if "%UCCM100REPO%"=="" set UCCM100REPO=%LOCALAPPDATA%\uCcm100Repo

if not "%UCCM100DEV%"=="" (
	call %UCCM100DEV%\uccm100.cmd %*
	exit /B %errorlevel%
)

if "%*"=="--uccm-update" (
	call :update_uccm
	exit /B %errorlevel%
)

if not exist "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" call :update_uccm
"%UCCM100REPO%\uccm-uccm100\uccm100.cmd" %*
goto :eof

:update_uccm
java -jar getuccm100.jar
if not %errorlevel% == 0 (
	echo "failed to acquire uCcm build manager"
	exit /B 1
)



