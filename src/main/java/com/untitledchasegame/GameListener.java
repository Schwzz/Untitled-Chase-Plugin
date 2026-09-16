package com.untitledchasegame;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

public class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && ((Player) event.getEntity()).getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof Player && ((Player) event.getDamager()).getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
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
            gameManager.tagPlayer(damager, victim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpectatorInteract(PlayerInteractEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isActiveParticipant(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isActiveParticipant(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (isActiveParticipant(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && isActiveParticipant((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isActiveParticipant((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryCommand(PlayerCommandPreprocessEvent event) {
        if (!isActiveParticipant(event.getPlayer())) return;

        String command = event.getMessage().toLowerCase(Locale.ROOT).split(" ", 2)[0];
        if (command.equals("/give") || command.equals("/item") || command.equals("/clear")
                || command.equals("/replaceitem")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerDisconnect(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        gameManager.handlePlayerJoin(event.getPlayer());
    }

    private boolean isActiveParticipant(Player player) {
        return gameManager.getState() == GameState.PLAYING && gameManager.isParticipant(player);
    }
}