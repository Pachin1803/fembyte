package com.dractical.fembyte.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;

import java.io.File;
import java.util.List;
import java.util.Map;

public final class FembyteGlobalConfig {

    private static final String CURRENT_VERSION = "1.0.0";

    private final ConfigFile configFile;

    public FembyteGlobalConfig(boolean initial) throws Exception {
        this.configFile = ConfigFile.loadConfig(
                new File(FembyteConfig.CONFIG_DIR.toFile(), FembyteConfig.GLOBAL_CONFIG_FILE)
        );

        structureConfig();
    }

    void structureConfig() {
        for (ConfigCategory category : ConfigCategory.valuesCached()) {
            createTitledSection(category.name(), category.getBaseKeyName());
        }
    }

    public void saveConfig() throws Exception {
        configFile.save();
    }

    public void createTitledSection(String title, String path) {
        configFile.addSection(title);
        configFile.addDefault(path, null);
    }

    public boolean getBoolean(String path, boolean def, String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getBoolean(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        configFile.addDefault(path, def);
        return configFile.getBoolean(path, def);
    }

    public String getString(String path, String def, String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getString(path, def);
    }

    public String getString(String path, String def) {
        configFile.addDefault(path, def);
        return configFile.getString(path, def);
    }

    public double getDouble(String path, double def, String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getDouble(path, def);
    }

    public double getDouble(String path, double def) {
        configFile.addDefault(path, def);
        return configFile.getDouble(path, def);
    }

    public int getInt(String path, int def, String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getInteger(path, def);
    }

    public int getInt(String path, int def) {
        configFile.addDefault(path, def);
        return configFile.getInteger(path, def);
    }

    public long getLong(String path, long def, String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getLong(path, def);
    }

    public long getLong(String path, long def) {
        configFile.addDefault(path, def);
        return configFile.getLong(path, def);
    }

    public List<String> getList(String path, List<String> def, String comment) {
        configFile.addDefault(path, def, comment);
        return configFile.getStringList(path);
    }

    public List<String> getList(String path, List<String> def) {
        configFile.addDefault(path, def);
        return configFile.getStringList(path);
    }

    public ConfigSection getConfigSection(String path, Map<String, Object> defaultKeyValue, String comment) {
        configFile.addDefault(path, null, comment);
        configFile.makeSectionLenient(path);
        defaultKeyValue.forEach((key, value) ->
                configFile.addExample(path + "." + key, value)
        );
        return configFile.getConfigSection(path);
    }

    public ConfigSection getConfigSection(String path, Map<String, Object> defaultKeyValue) {
        configFile.addDefault(path, null);
        configFile.makeSectionLenient(path);
        defaultKeyValue.forEach((key, value) ->
                configFile.addExample(path + "." + key, value)
        );
        return configFile.getConfigSection(path);
    }

    public Boolean getBoolean(String path) {
        String raw = configFile.getString(path, null);
        return raw == null ? null : Boolean.parseBoolean(raw);
    }

    public String getString(String path) {
        return configFile.getString(path, null);
    }

    public Double getDouble(String path) {
        String raw = configFile.getString(path, null);
        if (raw == null) return null;

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            FembyteConfig.LOGGER.warn(
                    "{} is not a valid number, skipping. Please check your configuration.",
                    path, e
            );
            return null;
        }
    }

    public Integer getInt(String path) {
        String raw = configFile.getString(path, null);
        if (raw == null) return null;

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            FembyteConfig.LOGGER.warn(
                    "{} is not a valid integer, skipping. Please check your configuration.",
                    path, e
            );
            return null;
        }
    }

    public Long getLong(String path) {
        String raw = configFile.getString(path, null);
        if (raw == null) return null;

        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            FembyteConfig.LOGGER.warn(
                    "{} is not a valid long, skipping. Please check your configuration.",
                    path, e
            );
            return null;
        }
    }

    public List<String> getList(String path) {
        return configFile.getList(path, null);
    }

    public ConfigSection getConfigSection(String path) {
        configFile.addDefault(path, null);
        configFile.makeSectionLenient(path);
        return configFile.getConfigSection(path);
    }

    public void addComment(String path, String comment) {
        configFile.addComment(path, comment);
    }
}
