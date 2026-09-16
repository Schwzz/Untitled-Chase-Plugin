package com.untitledchasegame;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerStatsManager {

    private final UntitledChaseGame plugin;
    private final File file;
    private final FileConfiguration config;

    public PlayerStatsManager(UntitledChaseGame plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void incrementGamesPlayed(UUID uuid) {
        increment(uuid, "games-played");
    }

    public void incrementRunnerEscapes(UUID uuid) {
        increment(uuid, "runner-escapes");
    }

    public void incrementChaserWins(UUID uuid) {
        increment(uuid, "chaser-wins");
    }

    public void incrementTags(UUID uuid) {
        increment(uuid, "tags");
    }

    public int getGamesPlayed(UUID uuid) {
        return get(uuid, "games-played");
    }

    public int getRunnerEscapes(UUID uuid) {
        return get(uuid, "runner-escapes");
    }

    public int getChaserWins(UUID uuid) {
        return get(uuid, "chaser-wins");
    }

    public int getTags(UUID uuid) {
        return get(uuid, "tags");
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save stats.yml: " + exception.getMessage());
        }
    }

    private void increment(UUID uuid, String key) {
        String path = "players." + uuid + "." + key;
        config.set(path, config.getInt(path) + 1);
        save();
    }

    private int get(UUID uuid, String key) {
        return config.getInt("players." + uuid + "." + key, 0);
    }
}