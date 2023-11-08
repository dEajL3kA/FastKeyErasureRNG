@echo off
cd /d "%~dp0"

if not exist "%JDK17_HOME%\bin\java.exe" (
	echo File "%JDK17_HOME%\bin\java.exe" not found. Please check JDK17_HOME and try again!
	pause
	exit /b 1
)

if not exist "%ANT_HOME%\bin\ant.bat" (
	echo File "%ANT_HOME%\bin\ant.bat" not found. Please check ANT_HOME and try again!
	pause
	exit /b 1
)

set "JAVA_HOME=%JDK17_HOME%"
set "PATH=%JAVA_HOME%\bin;%ANT_HOME%\bin;%PATH%"

call "%ANT_HOME%\bin\ant.bat" %*

pause
