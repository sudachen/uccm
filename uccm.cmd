@echo off
cd %~dp0

if "%UCCM100REPO%"=="" set UCCM100REPO=%LOCALAPPDATA%\uCcm100Repo

if not "%UCCM100DEV%"=="" (
	call %UCCM100DEV%\uccm100.cmd %*
	goto :eof
)

if not exist "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" (
	java -jar getuccm100.jar
	if not %errorlevel% == 0 (
		"failed to acquire uCcm build manager"
		goto :eof
	)
)

"%UCCM100REPO%\uccm-uccm100\uccm100.cmd" %*


