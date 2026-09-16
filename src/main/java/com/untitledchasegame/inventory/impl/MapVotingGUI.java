package com.untitledchasegame.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.untitledchasegame.GameManager;
import com.untitledchasegame.LocationManager;
import com.untitledchasegame.inventory.InventoryButton;
import com.untitledchasegame.inventory.InventoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapVotingGUI extends InventoryGUI {

    private final GameManager gameManager;
    private final List<Integer> playAreas;

    public MapVotingGUI(GameManager gameManager, LocationManager locationManager) {
        this.gameManager = gameManager;
        this.playAreas = locationManager.getPlayAreaNumbers();
        Collections.sort(this.playAreas);
    }

    @Override
    protected Inventory createInventory() {
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(playAreas.size() / 9.0)));
        return Bukkit.createInventory(null, rows * 9, ChatColor.GOLD + "Choose a Play Area");
    }

    @Override
    public void decorate(Player player) {
        int limit = Math.min(playAreas.size(), getInventory().getSize());
        for (int slot = 0; slot < limit; slot++) {
            int playArea = playAreas.get(slot);
            addButton(slot, new InventoryButton()
                    .creator(p -> buildItem(playArea))
                    .consumer(event -> {
                        Player clicker = (Player) event.getWhoClicked();
                        gameManager.castMapVote(clicker, playArea);
                        clicker.closeInventory();
                    })
            );
        }
        super.decorate(player);
    }

    private ItemStack buildItem(int playArea) {
        ItemStack item = XMaterial.matchXMaterial("PAPER")
                .map(XMaterial::parseItem)
                .orElseThrow(() -> new IllegalStateException("PAPER is unavailable."));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Play Area #" + playArea);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to vote for this map.",
                ChatColor.YELLOW + "Votes: " + gameManager.getMapVoteCount(playArea)
        ));
        item.setItemMeta(meta);
        return item;
    }
}