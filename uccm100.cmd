
set UCCMDIR=%~dp0
if "%UCCM100REPO%"=="" set UCCM100REPO=%LOCALAPPDATA%\uCcm100Repo

if not exist %UCCMDIR%util\uccm.Src\target\scala-2.12\classes\com\sudachen\uccm\Prog.class goto :use_jar
set CLASSPATH=%UCCMDIR%util\uccm.Src\target\scala-2.12\classes;%CLASSPATH% 
call :s_prog %*
set err=%errorlevel%
if %err% EQU 3 goto :stlink_reflash
exit %err%
 
:use_jar
call :j_prog %*
set err=%errorlevel%
if %err% EQU 3 goto :stlink_reflash
exit %err%

:stlink_reflash
"%UCCM100REPO%\jstlink-612\STLinkReflash.exe"
exit %err%

:s_prog
cmd /c scala com.sudachen.uccm.Prog %*
exit /B %errorlevel%

:j_prog
cmd /c java -jar %UCCMDIR%\uccm100.jar %*
exit /B %errorlevel%
