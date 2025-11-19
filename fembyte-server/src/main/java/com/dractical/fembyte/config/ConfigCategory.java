package com.dractical.fembyte.config;

public enum ConfigCategory {
    ASYNC("async"),
    PERFORMANCE("performance"),
    GAMEPLAY("gameplay"),
    MISC("misc");

    private final String baseKeyName;
    private static final ConfigCategory[] VALUES = values();

    ConfigCategory(String baseKeyName) {
        this.baseKeyName = baseKeyName;
    }

    public String getBaseKeyName() {
        return baseKeyName;
    }

    public static ConfigCategory[] valuesCached() {
        return VALUES;
    }
}
