package io.lightbeat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Does an HTTP GET to the given {@link #URL_STRING} that returns the latest version via {@link #getVersionString()}.
 * Can check if an update is available via {@link #isUpdateAvailable()}.
 */
public class UpdateChecker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);
    private static final String URL_STRING = "https://lightbeat.io/latest";

    private final String currentVersionString;
    private String latestVersionString;


    public UpdateChecker(String currentVersionString) {
        this.currentVersionString = currentVersionString;
    }

    public boolean isUpdateAvailable() {
        if (latestVersionString == null) {
            try {
                latestVersionString = getVersionString();
            } catch (Exception e) {
                return false;
            }
        }

        return !currentVersionString.equals(latestVersionString);
    }

    public String getVersionString() throws Exception {

        if (latestVersionString != null) {
            return latestVersionString;
        }

        String version = "";
        URL latestVersion = new URL(URL_STRING);
        HttpURLConnection connection = (HttpURLConnection) latestVersion.openConnection();

        logger.info("Attempting connection to " + URL_STRING);
        int responseCode = connection.getResponseCode();
        if (responseCode < 300 && responseCode >= 200) {

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            if ((inputLine = in.readLine()) != null) {
                version = inputLine;
            }

            in.close();
        }

        latestVersionString = version;
        return latestVersionString;
    }
}
