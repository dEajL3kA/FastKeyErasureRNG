@echo off
cd /d "%~dp0"

if not exist "%JDK17_HOME%\bin\java.exe" (
	echo File "%JDK17_HOME%\bin\java.exe" not found. Please check JDK17_HOME and try again!
	pause
	exit /b 1
)

if not exist "%CD%\junit-platform-console-standalone.jar" (
	echo File "junit-platform-console-standalone.jar" not found in current directory!
	echo Please download from https://mvnrepository.com/artifact/org.junit.platform/junit-platform-console-standalone
	pause
	exit /b 1
)

for %%i in (%CD%\dist\fast-key-erasure.*.tests.jar) do (
	set "JAR_FILE_PATH=%%~i"
	goto:found_jarfile
)

echo File "dist\fast-key-erasure.tests.jar" not found. Please build first!
pause
exit /b 1

:found_jarfile

set "JAVA_HOME=%JDK17_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set CLASSPATH=

"%JAVA_HOME%\bin\java.exe" -Xmx10g -Xms10g -ea -jar "%CD%\junit-platform-console-standalone.jar" --classpath "%JAR_FILE_PATH%" --scan-classpath

pause
