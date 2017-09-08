package io.lightbeat.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Does a simple HTTP GET request to the URL provided via constructor as string.
 */
public class URLConnectionReader {

    private final String urlString;


    public URLConnectionReader(String urlString) {
        this.urlString = urlString;
    }

    public String getFirstLine() throws Exception {

        String lineToReturn = "";
        URL latestVersion = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) latestVersion.openConnection();

        int responseCode = connection.getResponseCode();
        if (responseCode < 300 && responseCode >= 200) {

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            if ((inputLine = in.readLine()) != null) {
                lineToReturn = inputLine;
            }

            in.close();
        }

        return lineToReturn;
    }
}