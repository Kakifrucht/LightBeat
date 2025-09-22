package pw.wunderlich.lightbeat.hue.bridge;

/**
 * Simple wrapper around an ip address and a login key/username.
 */
public record AccessPoint(String ip, String key, String name, String certificateHash) {

    public AccessPoint(String ip, String key, String name) {
        this(ip, key, name, null);
    }

    public AccessPoint(String ip, String key) {
        this(ip, key, null, null);
    }

    public AccessPoint(String ip) {
        this(ip, null, null, null);
    }

    public boolean hasKey() {
        return key != null;
    }

    public boolean hasCertificateHash() {
        return certificateHash != null;
    }
}
