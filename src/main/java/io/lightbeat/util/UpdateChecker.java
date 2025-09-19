package io.lightbeat.util;

import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Checks the GitHub releases API for the given repository to find the latest version.
 * Can check if an update is available via {@link #isUpdateAvailable()}.
 */
public class UpdateChecker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);
    // URL to get the latest release information from the GitHub API
    private static final String URL_STRING = "https://api.github.com/repos/Kakifrucht/LightBeat/releases/latest";

    private final String currentVersionString;
    private String latestVersionString;


    public UpdateChecker(String currentVersionString) {
        this.currentVersionString = currentVersionString;
    }

    /**
     * Checks if a newer version of the software is available.
     *
     * @return true if an update is found, false otherwise.
     */
    public boolean isUpdateAvailable() {

        if (currentVersionString.equals("dev")) {
            return false;
        }

        if (latestVersionString == null) {
            try {
                latestVersionString = getVersionString();
                logger.info("Latest version: {}", latestVersionString);
            } catch (Exception e) {
                logger.error("Failed to check for updates", e);
                return false;
            }
        }

        if (latestVersionString == null || latestVersionString.isEmpty()) {
            return false;
        }

        if (isNewerVersion(currentVersionString, latestVersionString)) {
            logger.info("Update to LightBeat version {} found (current is {})", latestVersionString, currentVersionString);
            return true;
        }

        return false;
    }

    /**
     * Fetches the latest version string from the GitHub API.
     * This method parses the JSON response manually to avoid adding a JSON library dependency.
     *
     * @return The latest version string (e.g., "v1.2.3").
     * @throws Exception if there is an issue with the network connection or parsing the response.
     */
    public String getVersionString() throws Exception {

        if (latestVersionString != null) {
            return latestVersionString;
        }

        String version = "";
        URL latestVersionUrl = new URI(URL_STRING).toURL();
        HttpURLConnection connection = (HttpURLConnection) latestVersionUrl.openConnection();
        // Set a user-agent to comply with GitHub API requirements
        connection.setRequestProperty("User-Agent", "LightBeat-Update-Checker");

        logger.info("Attempting connection to " + URL_STRING);
        int responseCode = connection.getResponseCode();

        if (responseCode >= 200 && responseCode < 300) {
            // Use Guava's CharStreams to simplify reading the response
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                String jsonResponse = CharStreams.toString(reader);

                // Simple manual JSON parsing to find the "tag_name"
                String tagNameKey = "\"tag_name\":\"";
                int startIndex = jsonResponse.indexOf(tagNameKey);

                if (startIndex != -1) {
                    startIndex += tagNameKey.length();
                    int endIndex = jsonResponse.indexOf("\"", startIndex);
                    if (endIndex != -1) {
                        version = jsonResponse.substring(startIndex, endIndex);
                    }
                }
            }
        } else {
            logger.warn("Failed to fetch update info. Response code: {}", responseCode);
        }

        latestVersionString = version;
        return latestVersionString;
    }

    /**
     * Compares two version strings to determine if the latest version is newer.
     * Supports simple semantic versioning (e.g., "1.2.3").
     *
     * @param currentVersion The current version string.
     * @param latestVersion  The latest version string from the server.
     * @return true if latestVersion is newer than currentVersion.
     */
    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        // Remove any non-numeric characters except for dots (e.g., "v" prefix)
        String cleanCurrent = currentVersion.replaceAll("[^0-9.]", "");
        String cleanLatest = latestVersion.replaceAll("[^0-9.]", "");

        if (cleanCurrent.equals(cleanLatest)) {
            return false;
        }

        String[] currentParts = cleanCurrent.split("\\.");
        String[] latestParts = cleanLatest.split("\\.");

        int maxLength = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < maxLength; i++) {
            try {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                if (latestPart > currentPart) {
                    return true;
                }
                if (latestPart < currentPart) {
                    return false;
                }
            } catch (NumberFormatException e) {
                logger.error("Could not parse version strings: current='{}', latest='{}'", currentVersion, latestVersion, e);
                return false;
            }
        }

        return false;
    }
}