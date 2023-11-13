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

if ! which ant >/dev/null; then
	echo "Error: Required command 'ant' not found!"
	exit 1
fi

unset CLASSPATH

ant "$@"
