#!/bin/sh

# --- LightBeat Launcher Script ---
#
# This script checks for a valid Java JDK 21+ installation. If not found,
# it offers to download and set up a local copy for the application.
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

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd -- "$SCRIPT_DIR"
set -e # Exit immediately if a command exits with a non-zero status.

JAR_PATTERN="LightBeat-*-all.jar"
LOG_FILE="lightbeat.log"
REQUIRED_JAVA_VERSION=21

exit_and_keep_open() {
    printf "Press [Enter] to close this window. "; read dummy_var
    exit 1
}

check_java_version() {
    local java_cmd="$1"
    if [ ! -x "$java_cmd" ]; then
        return 1
    fi

    # Check if it's a JDK by looking for javac
    local javac_cmd="$(dirname "$java_cmd")/javac"
    if [ ! -f "$javac_cmd" ]; then
        echo "Info: Found Java at '$java_cmd' but it's not a JDK (javac not found)."
        return 1
    fi

    local java_version
    java_version=$("$java_cmd" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    local major_version
    major_version=$(echo "$java_version" | cut -d. -f1)

    if [ "$major_version" -lt "$REQUIRED_JAVA_VERSION" ]; then
        echo "Info: Found JDK at '$java_cmd' but version is $java_version. Required is $REQUIRED_JAVA_VERSION+."
        return 1
    fi

    echo "Found suitable JDK: $java_cmd (Version $java_version)"
    return 0
}

get_platform_details() {
    OS_TYPE=$(uname -s)
    case "$OS_TYPE" in
        Linux*)  OS_PARAM="linux";;
        Darwin*) OS_PARAM="mac";;
        *)       OS_PARAM="";;
    esac

    ARCH_TYPE=$(uname -m)
    case "$ARCH_TYPE" in
        x86_64)  ARCH_PARAM="x64";;
        aarch64) ARCH_PARAM="aarch64";;
        arm64)   ARCH_PARAM="aarch64";;
        *)       ARCH_PARAM="";;
    esac
}

show_manual_download_and_exit() {
    get_platform_details
    echo "$1"

    if [ -z "$OS_PARAM" ] || [ -z "$ARCH_PARAM" ]; then
        echo "Error: Unsupported platform (OS: '$OS_TYPE', Architecture: '$ARCH_TYPE')."
        echo "Please manually download and install Java ${REQUIRED_JAVA_VERSION} (JDK) or later from:"
        echo "https://adoptium.net/temurin/releases?version=${REQUIRED_JAVA_VERSION}"
        exit_and_keep_open
    fi

    DOWNLOAD_URL="https://adoptium.net/temurin/releases?version=${REQUIRED_JAVA_VERSION}&os=${OS_PARAM}&arch=${ARCH_PARAM}"

    echo "Please download and install Java ${REQUIRED_JAVA_VERSION} (JDK) or later from the link below:"
    echo "$DOWNLOAD_URL"
    exit_and_keep_open
}

download_and_unpack_jdk() {
    get_platform_details

    if [ -z "$OS_PARAM" ] || [ -z "$ARCH_PARAM" ]; then
        echo "Error: Unsupported platform (OS: '$OS_TYPE', Architecture: '$ARCH_TYPE')."
        echo "Automatic JDK download is not available for your system."
        exit_and_keep_open
    fi

    if ! touch .permission_test_file 2>/dev/null; then
        echo "Error: Insufficient permissions to write to the current directory."
        echo "Please run the script from a location where you have write access."
        exit_and_keep_open
    fi
    rm .permission_test_file

    local download_tool=""
    if command -v curl >/dev/null 2>&1; then
        download_tool="curl"
    elif command -v wget >/dev/null 2>&1; then
        download_tool="wget"
    else
        echo "Error: You need 'curl' or 'wget' to download the JDK."
        echo "Please install one of them and try again."
        exit_and_keep_open
    fi

    API_URL="https://api.adoptium.net/v3/binary/latest/${REQUIRED_JAVA_VERSION}/ga/${OS_PARAM}/${ARCH_PARAM}/jdk/hotspot/normal/eclipse?project=jdk"
    JDK_ARCHIVE="jdk-${REQUIRED_JAVA_VERSION}-${OS_PARAM}-${ARCH_PARAM}.tar.gz"

    echo "Downloading JDK from Adoptium..."
    if [ "$download_tool" = "curl" ]; then
        curl -L -o "$JDK_ARCHIVE" "$API_URL"
    else # wget
        wget -O "$JDK_ARCHIVE" "$API_URL"
    fi

    echo "Download complete. Unpacking..."
    mkdir -p "jdk"
    tar -xzf "$JDK_ARCHIVE" --strip-components=1 -C "jdk"
    rm "$JDK_ARCHIVE"
    echo "JDK has been successfully unpacked to 'jdk'."
}

echo "Launching LightBeat"

LOGGING_ENABLED=true
if [ "$1" = "--no-log" ]; then
    LOGGING_ENABLED=false
    echo "Log file creation has been disabled."
fi

# 1. Find a suitable JDK
JAVA_EXECUTABLE=""
echo "Searching for a suitable JDK..."

# Priority 1: Check local 'jdk' directory
JAVA_LOCAL_PATH="jdk/bin/java"
if [ -f "$JAVA_LOCAL_PATH" ]; then
    if check_java_version "$JAVA_LOCAL_PATH"; then
        JAVA_EXECUTABLE="$JAVA_LOCAL_PATH"
    fi
fi

# Priority 2: Check system PATH if local JDK not found or invalid
if [ -z "$JAVA_EXECUTABLE" ] && command -v java >/dev/null 2>&1; then
    if check_java_version "$(command -v java)"; then
        JAVA_EXECUTABLE="$(command -v java)"
    fi
fi

# 3. If no JDK found, offer to download it
if [ -z "$JAVA_EXECUTABLE" ]; then
    echo "No suitable JDK (version $REQUIRED_JAVA_VERSION+) found on your system."
    printf "Would you like to download and set up a local JDK for this application? [Y/n] "
    read -r response
    case "$response" in
        [yY][eE][sS]|[yY]|"")
            download_and_unpack_jdk
            JAVA_EXECUTABLE="$JAVA_LOCAL_PATH"
            ;;
        *)
            show_manual_download_and_exit "Operation cancelled."
            ;;
    esac
fi

# 4. Find the application JAR
echo "Searching for application JAR file..."
JAR_FILE=$(find ./app -maxdepth 1 -name "$JAR_PATTERN" -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)

if [ -z "$JAR_FILE" ]; then
    echo "Error: No application JAR found matching '$JAR_PATTERN'."
    echo "Please make sure the JAR file is in the 'app' directory."
    exit_and_keep_open
fi

echo "Found application: $JAR_FILE"
echo "Using JDK at '$JAVA_EXECUTABLE'. Starting LightBeat..."

set +e # Disable exit on error

# 5. Launch the application
if [ "$LOGGING_ENABLED" = true ]; then
    nohup "$JAVA_EXECUTABLE" --add-opens 'java.base/java.lang=ALL-UNNAMED' -jar "$JAR_FILE" >"$LOG_FILE" 2>&1 &
else
    nohup "$JAVA_EXECUTABLE" --add-opens 'java.base/java.lang=ALL-UNNAMED' -jar "$JAR_FILE" >/dev/null 2>&1 &
fi
APP_PID=$!

sleep 2
if ! kill -0 "$APP_PID" >/dev/null 2>&1; then
    echo "Error: LightBeat failed to start. The process is not running."
    if [ "$LOGGING_ENABLED" = true ]; then
        echo "Please check the log file for details: $LOG_FILE"
    fi
    exit_and_keep_open
fi

echo "LightBeat has been launched successfully."
echo "   - Process ID (PID): $APP_PID"
if [ "$LOGGING_ENABLED" = true ]; then
    echo "   - Logs are being written to: $LOG_FILE (disable with --no-log flag)"
fi
sleep 5
