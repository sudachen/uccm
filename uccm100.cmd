
set UCCMDIR=%~dp0

if not exist %UCCMDIR%util\uccm.Src\target\scala-2.12\classes\com\sudachen\uccm\Prog.class goto :use_jar
set CLASSPATH=%UCCMDIR%util\uccm.Src\target\scala-2.12\classes;%CLASSPATH% 
scala com.sudachen.uccm.Prog %*
exit /B %errorlevel%
 
:use_jar
java -jar %UCCMDIR%\uccm100.jar %*
if %errorlevel% NEQ 0 exit 1

