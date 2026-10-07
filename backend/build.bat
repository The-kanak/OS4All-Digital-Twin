@echo off
SET "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
SET "PATH=%JAVA_HOME%\bin;%PATH%"
ECHO JAVA version:
java -version
ECHO.
ECHO Running Maven compile...
cd /D "d:\OS4All\backend"
call mvn clean compile 2>&1
ECHO.
ECHO COMPILE_EXIT_CODE:%ERRORLEVEL%
