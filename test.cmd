@echo off
cd /d "%~dp0"

if not exist "%JDK17_HOME%\bin\java.exe" (
	echo File "%JDK17_HOME%\bin\java.exe" not found. Please check JDK17_HOME and try again!
	pause
	exit /b 1
)

if not exist "%CD%\junit-platform-console-standalone.jar" (
	echo File "junit-platform-console-standalone.jar" not found in current directory!
	pause
	exit /b 1
)

for %%i in (%CD%\dist\fast-key-erasure.*.jdk-17.jar) do (
	set "JAR_FILE_CORE=%%i"
	goto:found_jarfile_core
)
echo File "fast-key-erasure.jdk-17.jar" not found. Please build first!
pause
exit /b 1

:found_jarfile_core

for %%i in (%CD%\dist\fast-key-erasure.*.tests.jar) do (
	set "JAR_FILE_TEST=%%i"
	goto:found_jarfile_test
)
echo File "fast-key-erasure.tests.jar" not found. Please build first!
pause
exit /b 1

:found_jarfile_test

set "JAVA_HOME=%JDK17_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set CLASSPATH=

"%JAVA_HOME%\bin\java.exe" -Xmx10g -Xms10g -ea -jar "%CD%\junit-platform-console-standalone.jar" --classpath "%JAR_FILE_TEST%;%JAR_FILE_CORE%;%CD%\lib\test\ascii85-1.2.jar" --scan-classpath

pause
