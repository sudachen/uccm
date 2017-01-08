cmd /c sbt one-jar
del /q ..\..\uccm100.jar
copy target\scala-2.12\uccm_2.12-1.0.0-one-jar.jar ..\..\uccm100.jar
