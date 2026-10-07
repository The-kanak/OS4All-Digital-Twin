@echo off
SET "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
SET "PATH=%JAVA_HOME%\bin;%PATH%"
cd /D "d:\OS4All\backend"
call mvn test -Dspring.profiles.active=test 2>&1
ECHO.
ECHO TEST_EXIT_CODE:%ERRORLEVEL%
