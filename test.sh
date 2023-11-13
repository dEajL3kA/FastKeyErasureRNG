#!/bin/bash
set -e
cd -- "$(dirname "${BASH_SOURCE[0]}")"

if [ -z "${JAVA_HOME}" ]; then
	echo "Error: Environment variable JAVA_HOME is not set!"
	exit 1
fi

if [ ! -d "${JAVA_HOME}" ]; then
	echo "Error: Directory JAVA_HOME='${JAVA_HOME}' not found!"
	exit 1
fi

export PATH="${JAVA_HOME}/bin:${PATH}"

if ! which java >/dev/null; then
	echo "Error: Required command 'java' not found!"
	exit 1
fi

if ! which pv >/dev/null; then
	echo "Error: Required command 'pv' not found!"
	exit 1
fi

JUNIT_RUNNER_PATH="$(find -L "${PWD}/libexec" -type f -name 'junit-platform-console-standalone*.jar' | head -n 1)"
if [[ -z "${JUNIT_RUNNER_PATH}" || ! -e "${JUNIT_RUNNER_PATH}" ]]; then
	echo "Error: Required file \"libexec/junit-platform-console-standalone.jar\" not found!"
	echo "Please download from https://mvnrepository.com/artifact/org.junit.platform/junit-platform-console-standalone"
	exit 1
fi

JAR_FILE_PATH="$(find -L "${PWD}/dist" -type f -name 'fast-key-erasure.*.tests.jar' | head -n 1)"
if [[ -z "${JAR_FILE_PATH}" || ! -e "${JAR_FILE_PATH}" ]]; then
	echo "Error: File \"dist/fast-key-erasure.tests.jar\" not found. Please build first!"
	exit 1
fi

unset CLASSPATH

java -Xmx10g -Xms10g -ea -jar "${JUNIT_RUNNER_PATH}" --classpath "${JAR_FILE_PATH}" --scan-classpath | pv -q
