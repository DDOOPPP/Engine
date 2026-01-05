package org.gi.gIEngine;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.gi.Result;
import org.gi.Util;
import org.gi.builder.StatBuilder;
import org.gi.stat.IStat;
import org.gi.stat.IStatRegistry;
import org.gi.stat.enums.ScalingType;
import org.gi.stat.enums.StatCategory;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class StatLoader {
    private final Logger logger;
    private final IStatRegistry registry;
    private final Plugin plugin;
    private final File dataFolder;

    public StatLoader(Plugin plugin, IStatRegistry registry) {
        this.logger = plugin.getLogger();
        this.dataFolder = plugin.getDataFolder();
        this.registry = registry;
        this.plugin = plugin;
    }

    public Result load() {
        File file = new File(dataFolder, "stat.yml");
        if (!file.exists()) {
            plugin.saveResource("stat.yml", false);
        }

        GIConfig config = new GIConfig(file);

        ConfigurationSection section = config.getSection("stats");

        if (section == null) {
            logger.warning("No stats section found");
            return Result.Error("No stats section found");
        }

        int count = 0;

        for (String key : section.getKeys(false)) {
            ConfigurationSection statSection = section.getConfigurationSection(key);

            if (statSection == null) {
                logger.warning("Stats section '" + key + "' not found");
                continue;
            }

            try{
                IStat stat = parseStat(statSection,key);

                if (registry.register(stat)) {
                    count++;
                    logger.info("Registered stat '" + key + "'");
                }else{
                    logger.warning("Failed to register stat '" + key + "'");
                }
            }catch (Exception e) {
                logger.severe("Stat Parsing Failed '" + key + "'");
                logger.severe(e.getMessage());
            }
        }
        return Result.SUCCESS("Loaded " + count + " stats");
    }

    public Result reload(){
        if (registry.isLocked()){
            return Result.Error("StatsRegistry is locked");
        }

        registry.clear();

        return load();
    }

    public IStat parseStat(ConfigurationSection section,String statId) {
        String displayName = section.getString(" display-name",statId);
        List<String> description = section.getStringList(" descriptions");
        StatCategory category = Util.parseEnum(StatCategory.class, section.getString(" category","MISC").toUpperCase());
        ScalingType scalingType = Util.parseEnum(ScalingType.class, section.getString(" scaling-type","INTEGER").toUpperCase());

        double defaultValue = section.getDouble("default-value", 0);
        double minValue = section.getDouble("min-value", Double.MIN_VALUE);
        double maxValue = section.getDouble("max-value", Double.MAX_VALUE);
        boolean percentageBased = section.getBoolean("percentage-based", false);

        return StatBuilder.create()
                .id(statId)
                .displayName(displayName)
                .description(description)
                .category(category)
                .scalingType(scalingType)
                .defaultValue(defaultValue)
                .range(minValue,maxValue)
                .percentageBased(percentageBased)
                .build();
    }
}
