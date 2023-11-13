@echo off
cd /d "%~dp0"

if not exist "%JDK17_HOME%\bin\java.exe" (
	echo File "%JDK17_HOME%\bin\java.exe" not found. Please check JDK17_HOME and try again!
	pause
	exit /b 1
)

for %%i in ("%CD%\libexec\junit-platform-console-standalone*.jar") do (
	set "JUNIT_RUNNER_PATH=%%~i"
	goto:found_runner
)

echo File "libexec\junit-platform-console-standalone.jar" not found. Please build first!
echo Please download from https://mvnrepository.com/artifact/org.junit.platform/junit-platform-console-standalone
pause
exit /b 1

:found_runner

for %%i in ("%CD%\dist\fast-key-erasure*.tests.jar") do (
	set "JAR_FILE_PATH=%%~i"
	goto:found_jarfile
)

echo File "dist\fast-key-erasure.tests.jar" not found. Please build first!
pause
exit /b 1

:found_jarfile

if "%PROCESSOR_ARCHITECTURE%"=="x86" (
	set TEE_EXEFILE=tee-x86.exe
) else if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
	set TEE_EXEFILE=tee-x64.exe
) else if "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
	set TEE_EXEFILE=tee-a64.exe
) else (
	echo Unknown processor architecture!
	pause
	exit /b 1
)

set "JAVA_HOME=%JDK17_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set CLASSPATH=

"%JAVA_HOME%\bin\java.exe" -Xmx10g -Xms10g -ea -jar "%JUNIT_RUNNER_PATH%" --classpath "%JAR_FILE_PATH%" --scan-classpath | "%CD%\libexec\windows\%TEE_EXEFILE%" NUL

pause
