package org.gi.gIEngine;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Set;

public class GIConfig {
    private final File file;
    private final FileConfiguration config;

    public GIConfig(File file){
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String getString(String key){
        return config.getString(key);
    }

    public List<String> getStringList(String key){
        return config.getStringList(key);
    }

    public int getInt(String key){
        return config.getInt(key);
    }

    public List<Integer> getIntList(String key){
        return config.getIntegerList(key);
    }

    public double getDouble(String key){
        return config.getDouble(key);
    }

    public List<Double> getDoubleList(String key){
        return config.getDoubleList(key);
    }

    public boolean getBoolean(String key){
        return config.getBoolean(key);
    }

    public List<Boolean> getBooleanList(String key){
        return config.getBooleanList(key);
    }

    public long getLong(String key){
        return config.getLong(key);
    }

    public List<Long> getLongList(String key){
        return config.getLongList(key);
    }

    public boolean contains(String key){
        return config.contains(key);
    }

    public ConfigurationSection getSection(String key){
        return config.getConfigurationSection(key);
    }

    public Set<String> getKeys(boolean isDeep){
        return config.getKeys(isDeep);
    }
}
