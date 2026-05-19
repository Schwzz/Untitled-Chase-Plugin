package com.untitledchasegame;

import com.untitledchasegame.inventory.gui.GUIListener;
import com.untitledchasegame.inventory.gui.GUIManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class UntitledChaseGame extends JavaPlugin {

    @Getter private GameManager gameManager;
    @Getter private LocationManager locationManager;
    @Getter private BossBarManager bossBarManager;
    @Getter private ScoreboardManager scoreboardManager;
    @Getter private GUIManager guiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.locationManager = new LocationManager(this);
        this.bossBarManager = new BossBarManager();
        this.scoreboardManager = new ScoreboardManager();
        this.guiManager = new GUIManager();
        this.gameManager = new GameManager(this);

        UCGCommand ucgCommand = new UCGCommand(this);
        getCommand("ucg").setExecutor(ucgCommand);
        getCommand("ucg").setTabCompleter(ucgCommand);

        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(guiManager), this);

        getLogger().info("UntitledChaseGame enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.getState() != GameState.IDLE) {
            gameManager.stopGame();
        }
        getLogger().info("UntitledChaseGame disabled.");
    }
}