package io.lightbeat.hue.bridge;

public class AccessPoint {

    private final String ip;
    private final String key;

    public AccessPoint(String ip) {
        this(ip, null);
    }

    AccessPoint(String ip, String key) {
        this.ip = ip;
        this.key = key;
    }

    public String getIp() {
        return ip;
    }

    String getKey() {
        return key;
    }

    boolean hasKey() {
        return key != null;
    }
}
