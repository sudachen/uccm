@echo off
cmd /c javac uccm/GetUccm.java
cmd /c jar -cvfe getuccm100.jar uccm.GetUccm uccm/GetUccm.class
copy /B uccm.cmd + getuccm100.jar ..\..\uccm.cmd
cd ../..
del /q uccm100-dist.zip 
cmd /c jar -cMf uccm100-dist.zip uccm.cmd



