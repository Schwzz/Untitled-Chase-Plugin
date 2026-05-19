package com.untitledchasegame;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class BossBarManager {

    private BossBar bossBar;

    public BossBarManager() {
        this.bossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
    }

    public void show(Player player) {
        bossBar.addPlayer(player);
    }

    public void hide(Player player) {
        bossBar.removePlayer(player);
    }

    public void hideAll() {
        bossBar.removeAll();
    }

    public void update(String title, double progress) {
        bossBar.setTitle(title);
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        bossBar.setProgress(clamped);
        if (clamped > 0.5) {
            bossBar.setColor(BarColor.GREEN);
        } else if (clamped > 0.25) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.RED);
        }
    }

    public void setVisible(boolean visible) {
        bossBar.setVisible(visible);
    }
}