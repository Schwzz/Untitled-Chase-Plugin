package com.untitledchasegame;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (gameManager.getState() != GameState.PLAYING) return;

        // Cancel all damage during PLAYING (peaceful mode)
        if (event.getEntity() instanceof Player || event.getDamager() instanceof Player) {
            event.setCancelled(true);
        }

        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (!gameManager.isParticipant(damager) || !gameManager.isParticipant(victim)) return;

        PlayerRole damagerRole = gameManager.getRole(damager);
        PlayerRole victimRole = gameManager.getRole(victim);

        if (damagerRole == PlayerRole.CHASER && victimRole == PlayerRole.RUNNER) {
            gameManager.tagPlayer(victim);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerLeave(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Auto-add to STARTING phase participants
        if (gameManager.getState() == GameState.STARTING) {
            Player player = event.getPlayer();
            gameManager.getParticipants().add(player.getUniqueId());
        }
    }
}