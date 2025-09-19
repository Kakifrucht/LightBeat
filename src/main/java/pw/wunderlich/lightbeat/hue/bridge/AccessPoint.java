package pw.wunderlich.lightbeat.hue.bridge;

/**
 * Simple wrapper around an ip address and a login key/username.
 */
public record AccessPoint(String ip, String key) {

    public AccessPoint(String ip) {
        this(ip, null);
    }

    public boolean hasKey() {
        return key != null;
    }
}
