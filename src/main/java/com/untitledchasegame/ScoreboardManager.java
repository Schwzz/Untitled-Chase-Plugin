package com.untitledchasegame;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    public void give(Player player, int runnersRemaining, String role) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("ucg", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "Chase Game");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore(ChatColor.YELLOW + "Runners Left:").setScore(4);
        obj.getScore(ChatColor.WHITE + String.valueOf(runnersRemaining)).setScore(3);
        obj.getScore(ChatColor.GRAY + " ").setScore(2);
        obj.getScore(ChatColor.YELLOW + "Your Role:").setScore(1);
        obj.getScore(role).setScore(0);

        playerBoards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
    }

    public void update(Player player, int runnersRemaining, String role) {
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) {
            give(player, runnersRemaining, role);
            return;
        }
        Objective obj = board.getObjective("ucg");
        if (obj == null) {
            give(player, runnersRemaining, role);
            return;
        }
        // Clear old dynamic entries and re-set them
        for (String entry : board.getEntries()) {
            // Only reset entries that are dynamic values (not the static labels)
            if (!entry.contains("Runners Left:") && !entry.contains("Your Role:") && !entry.equals(ChatColor.GRAY + " ")) {
                board.resetScores(entry);
            }
        }
        obj.getScore(ChatColor.WHITE + String.valueOf(runnersRemaining)).setScore(3);
        obj.getScore(role).setScore(0);
    }

    public void remove(Player player) {
        playerBoards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void removeAll() {
        for (UUID id : playerBoards.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        playerBoards.clear();
    }
}