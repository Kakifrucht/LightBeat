#!/bin/sh

# --- LightBeat Launcher Script ---
#
# This script checks for a valid Java installation and launches LightBeat.
# By default, it saves all application output to a log file.
#
# ## Usage:
#
#   ./launch_lightbeat.sh
#   (Launches the application and creates 'lightbeat.log')
#
#   ./launch_lightbeat.sh --no-log
#   (Launches the application without creating a log file)
#
# -----------------------------------

JAR_PATTERN="LightBeat-*-all.jar"
LOG_FILE="lightbeat.log"

show_download_and_exit() {
    echo "$1"

    OS_TYPE=$(uname -s)
    case "$OS_TYPE" in
        Linux*)  OS_PARAM="linux";;
        Darwin*) OS_PARAM="macos";;
        *)       OS_PARAM="any";;
    esac

    ARCH_TYPE=$(uname -m)
    case "$ARCH_TYPE" in
        x86_64)  ARCH_PARAM="x64";;
        aarch64) ARCH_PARAM="aarch64";;
        arm64)   ARCH_PARAM="aarch64";;
        *)       ARCH_PARAM="any";;
    esac

    DOWNLOAD_URL="https://adoptium.net/en-GB/temurin/releases?version=17&os=${OS_PARAM}&arch=${ARCH_PARAM}"

    echo "Please download and install Java 17 (JRE) or later from the link below:"
    echo "$DOWNLOAD_URL"
    exit 1
}

LOGGING_ENABLED=true
if [ "$1" = "--no-log" ]; then
    LOGGING_ENABLED=false
    echo "Log file creation has been disabled."
fi

if ! command -v java >/dev/null 2>&1; then
    show_download_and_exit "Java is not installed on your system."
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
MAJOR_VERSION=$(echo "$JAVA_VERSION" | cut -d. -f1)

if [ "$MAJOR_VERSION" -lt 17 ]; then
    show_download_and_exit "You have Java version $JAVA_VERSION, but LightBeat requires Java 17 or higher."
fi

echo "Searching for application JAR file..."
JAR_FILE=$(find . -maxdepth 1 -name "$JAR_PATTERN" -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)

# Exit if no JAR file was found.
if [ -z "$JAR_FILE" ]; then
    echo "❌ Error: No application JAR found matching '$JAR_PATTERN'."
    echo "Please make sure the JAR file is in this directory."
    exit 1
fi

echo "Found application: $JAR_FILE"
echo "Java version $JAVA_VERSION found. Starting LightBeat..."

if [ "$LOGGING_ENABLED" = true ]; then
    nohup java -jar "$JAR_FILE" >"$LOG_FILE" 2>&1 &
else
    nohup java -jar "$JAR_FILE" >/dev/null 2>&1 &
fi
APP_PID=$!

sleep 5

if ! kill -0 "$APP_PID" >/dev/null 2>&1; then
    echo "❌ Error: LightBeat failed to start. The process is not running."
    if [ "$LOGGING_ENABLED" = true ]; then
        echo "Please check the log file for details: $LOG_FILE"
    fi
    exit 1
fi

echo "LightBeat has been launched successfully."
echo "   - Process ID (PID): $APP_PID"
if [ "$LOGGING_ENABLED" = true ]; then
    echo "   - Logs are being written to: $LOG_FILE (disable with --no-log flag)"
fi