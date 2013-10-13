@echo off
SETLOCAL ENABLEEXTENSIONS
set SIKULIX_HOME=%~dp0

echo +++ trying to start Sikuli Setup in %SIKULIX_HOME%
PATH=%SIKULIX_HOME%libs;%PATH%

IF not EXIST "%SIKULIX_HOME%sikuli-update.jar" goto NOUPDATE
  java -jar "%SIKULIX_HOME%sikuli-update.jar" update
goto FINALLY

:NOUPDATE
java -jar "%SIKULIX_HOME%sikuli-setup.jar"

:FINALLY

ENDLOCAL
pause
exit