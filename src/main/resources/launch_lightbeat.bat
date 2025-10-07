@echo off
setlocal

:: ============================================================================
:: Configuration
:: ============================================================================
set "REQUIRED_JAVA_VERSION=21"
set "APP_DIR=app"
set "JAR_PATTERN=LightBeat-*-all.jar"

:: ============================================================================
:: Argument Parsing
:: ============================================================================
set "JAVA_OPTS="
:parse_args_loop
if "%~1"=="" goto :args_parsed

if /i "%~1"=="--dump-all-devices" (
    set "JAVA_OPTS=-Dlightbeat.audio.dumpAll=true"
    echo "Debug mode enabled: Dumping all audio devices."
)
rem

shift
goto :parse_args_loop
:args_parsed

echo Checking for Java...
where java >nul 2>nul
if %errorlevel% neq 0 (
    call :fail "Java command not found in your system's PATH."
    goto :eof
)

echo Verifying Java Development Kit (JDK)...
where javac >nul 2>nul
if %errorlevel% neq 0 (
    call :fail "A Java Runtime (JRE) was found, but a JDK is required (javac.exe is missing)."
    goto :eof
)

for /f "tokens=3" %%G in ('java -version 2^>^&1') do (
    set "JAVA_VERSION_STRING=%%G"
    goto :version_found
)
:version_found

set "JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%"

for /f "tokens=1,2 delims=." %%a in ("%JAVA_VERSION_STRING%") do (
    if "%%a" == "1" (
        set "MAJOR_VERSION=%%b"
    ) else (
        set "MAJOR_VERSION=%%a"
    )
)

echo Found Java version: %MAJOR_VERSION% (Full: %JAVA_VERSION_STRING%)

if %MAJOR_VERSION% lss %REQUIRED_JAVA_VERSION% (
    call :fail "Java version %MAJOR_VERSION% is installed, but version %REQUIRED_JAVA_VERSION% or newer is required."
    goto :eof
)

echo Java check passed.

echo Searching for application file in '%APP_DIR%' directory...
if not exist "%APP_DIR%\" (
    call :fail "Application directory '%APP_DIR%' not found."
    goto :eof
)

set "LATEST_JAR="
for /f "delims=" %%J in ('dir /b /o-d "%APP_DIR%\%JAR_PATTERN%" 2^>nul') do (
    set "LATEST_JAR=%%J"
    goto :jar_found
)

:: If the loop completes without finding a file, LATEST_JAR will be empty.
call :fail "Could not find a JAR file matching '%JAR_PATTERN%' in the '%APP_DIR%' directory."
goto :eof

:jar_found
echo Found application: %LATEST_JAR%

echo.
echo Launching %LATEST_JAR%...
echo.
java %JAVA_OPTS% --add-opens "java.base/java.lang=ALL-UNNAMED" -jar "%APP_DIR%\%LATEST_JAR%"

echo.
echo Application has finished.
goto :end

:: ============================================================================
:: Failure Subroutine
:: ============================================================================
:fail
echo.
echo =================================== ERROR ====================================
echo.
echo %~1
echo.
echo Please install or update your Java Development Kit (JDK).
echo.
echo Recommended Download (Adoptium Temurin JDK):
set "NATIVE_ARCH=%PROCESSOR_ARCHITECTURE%"
if defined PROCESSOR_ARCHITEW6432 set "NATIVE_ARCH=%PROCESSOR_ARCHITEW6432%"

set "ARCH_PARAM="
if /i "%NATIVE_ARCH%" == "AMD64" set "ARCH_PARAM=x64"
if /i "%NATIVE_ARCH%" == "ARM64" set "ARCH_PARAM=aarch64"

if not defined ARCH_PARAM (
    echo https://adoptium.net/temurin/releases?version=%REQUIRED_JAVA_VERSION%
) else (
    echo https://adoptium.net/temurin/releases?version=%REQUIRED_JAVA_VERSION%^&os=windows^&architecture=%ARCH_PARAM%
)
echo.
if /i "%ARCH_PARAM%" == "x64" (
    echo Alternatively use the LightBeat .msi installer on the website.
)
echo ============================================================================
echo.
pause
exit /b 1

:end
pause
endlocal
