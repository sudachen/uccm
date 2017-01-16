@echo off

rem 
rem  It's an uCcm starting script
rem 
rem  normally it needs to be redistributed as part of the your project
rem

rem  Environment variable UCCM100REPO says 
rem     where uccm should place all packages and itself also
rem  Environment variable UCCM100DEV says
rem     you are using development version taken from git repository
rem     and where it is

cd %~dp0
set PROGNAME=%~nx0

if "%UCCM100REPO%"=="" set UCCM100REPO=%LOCALAPPDATA%\uCcm100Repo

if "%*"=="--self-update" (
	if exist "%UCCM100REPO%\uccm-uccm100" rmdir /Q /S "%UCCM100REPO%\uccm-uccm100"
	call :update_uccm
	if %errorlevel% NEQ 0 exit 1
	goto :eof
)

if not "%UCCM100DEV%"=="" (
	echo "*** Development uCcm version is using now ***"
	call %UCCM100DEV%\uccm100.cmd %*
	if %errorlevel% NEQ 0 exit 1
	goto :eof
)

if not exist "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" call :update_uccm
if %errorlevel% NEQ 0 exit 1
call "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" %*
if %errorlevel% NEQ 0 exit 1
exit /B 0

:update_uccm
java -jar %PROGNAME%
if %errorlevel% EQU 0 goto :eof
echo "failed to acquire uCcm build manager"
exit 1




