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

for %%i in (%*) do if %%i == --self-update goto :do_self_update
for %%i in (%*) do if %%i == --no-dev set UCCM100DEV=
for %%i in (%*) do if %%i == --jlink-stlink goto :exec_jlink_stlink

if not "%UCCM100DEV%" == "" goto :exec_dev_version

:exec_no_dev
if not exist "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" call :update_uccm
if %errorlevel% NEQ 0 exit 1
call "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" %*
if %errorlevel% EQU 0 goto :eof
exit 1

:do_self_update
if exist "%UCCM100REPO%\uccm-uccm100" rmdir /Q /S "%UCCM100REPO%\uccm-uccm100"
call :update_uccm
if %errorlevel% EQU 0 goto :eof
exit 1

:update_uccm
java -jar %PROGNAME%
if %errorlevel% EQU 0 goto :eof
echo "failed to acquire uCcm build manager"
exit 1

:exec_dev_version
echo *** Development uCcm version is using now ***
call %UCCM100DEV%\uccm100.cmd %*
if %errorlevel% EQU 0 goto :eof
exit 1

:exec_jlink_stlink
set UCCM100DIR=%UCCM100REPO%\uccm-uccm100
if not "%UCCM100DEV%" == "" set UCCM100DIR=%UCCM100DEV%
call "%UCCM100DIR%\uccm100.cmd" %*
if %errorlevel% NEQ 3 exit 1
"%UCCM100REPO%\jstlink-612\STLinkReflash.exe"

goto :eof

rem
rem
rem
rem
rem
rem
rem
rem
rem
rem
rem
rem