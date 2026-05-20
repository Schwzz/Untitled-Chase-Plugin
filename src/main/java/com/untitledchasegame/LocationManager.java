package com.untitledchasegame;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LocationManager {

    private final UntitledChaseGame plugin;
    private Location lobby;
    private Location waitingArea;

    public LocationManager(UntitledChaseGame plugin) {
        this.plugin = plugin;
        load();
    }

    public void setLobby(Location loc) {
        this.lobby = loc;
        saveLocation("lobby", loc);
    }

    public void setWaitingArea(Location loc) {
        this.waitingArea = loc;
        saveLocation("waiting-area", loc);
    }

    public void setPlayArea(int number, Location loc) {
        saveLocation("play-area." + number, loc);
    }

    public void setServerNumber(int number) {
        plugin.getConfig().set("server-number", number);
        plugin.saveConfig();
    }

    public int getServerNumber() {
        return plugin.getConfig().getInt("server-number", -1);
    }

    public void resetLobby() {
        this.lobby = null;
        plugin.getConfig().set("locations.lobby", null);
        plugin.saveConfig();
    }

    public void resetWaitingArea() {
        this.waitingArea = null;
        plugin.getConfig().set("locations.waiting-area", null);
        plugin.saveConfig();
    }

    public void resetPlayAreas() {
        plugin.getConfig().set("locations.play-area", null);
        plugin.saveConfig();
    }

    public void resetServerNumber() {
        plugin.getConfig().set("server-number", null);
        plugin.saveConfig();
    }

    public void resetAll() {
        this.lobby = null;
        this.waitingArea = null;
        plugin.getConfig().set("locations", null);
        plugin.getConfig().set("server-number", null);
        plugin.saveConfig();
    }

    public Location getLobby() { return lobby; }
    public Location getWaitingArea() { return waitingArea; }

    public Location getRandomPlayArea() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains("locations.play-area")) return null;
        List<Integer> keys = new ArrayList<>();
        for (String key : cfg.getConfigurationSection("locations.play-area").getKeys(false)) {
            try {
                keys.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {}
        }
        if (keys.isEmpty()) return null;
        int chosen = keys.get(new Random().nextInt(keys.size()));
        return loadLocation("play-area." + chosen);
    }

    public List<Integer> getPlayAreaNumbers() {
        FileConfiguration cfg = plugin.getConfig();
        List<Integer> keys = new ArrayList<>();
        if (!cfg.contains("locations.play-area")) return keys;
        for (String key : cfg.getConfigurationSection("locations.play-area").getKeys(false)) {
            try {
                keys.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {}
        }
        return keys;
    }

    private void saveLocation(String key, Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "locations." + key;
        cfg.set(path + ".world", loc.getWorld().getName());
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        cfg.set(path + ".yaw", (double) loc.getYaw());
        cfg.set(path + ".pitch", (double) loc.getPitch());
        plugin.saveConfig();
    }

    private void load() {
        this.lobby = loadLocation("lobby");
        this.waitingArea = loadLocation("waiting-area");
    }

    private Location loadLocation(String key) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "locations." + key;
        if (!cfg.contains(path + ".world")) return null;
        String worldName = cfg.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw = (float) cfg.getDouble(path + ".yaw");
        float pitch = (float) cfg.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}