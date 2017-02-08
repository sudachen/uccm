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

if not "%UCCM100DEV%" == "" goto :exec_dev_version

:exec_no_dev
if exist "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" goto :uccm100
cmd /c java -jar %PROGNAME%
if %errorlevel% EQU 0 goto :uccm100
echo "failed to acquire uCcm build manager"
exit 1
:uccm100
if %errorlevel% NEQ 0 exit 1
"%UCCM100REPO%\uccm-uccm100\uccm100.cmd" %*

:do_self_update
if exist "%UCCM100REPO%\uccm-uccm100" rmdir /Q /S "%UCCM100REPO%\uccm-uccm100"
cmd /c java -jar %PROGNAME%
if %errorlevel% EQU 0 goto :eof
echo "failed to acquire uCcm build manager"
exit 1

:exec_dev_version
echo *** Development uCcm version is using now ***
%UCCM100DEV%\uccm100.cmd %*

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