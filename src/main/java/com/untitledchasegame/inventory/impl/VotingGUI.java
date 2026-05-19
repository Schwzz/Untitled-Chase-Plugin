package com.untitledchasegame.inventory.impl;

import com.untitledchasegame.GameManager;
import com.untitledchasegame.inventory.InventoryButton;
import com.untitledchasegame.inventory.InventoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class VotingGUI extends InventoryGUI {

    private final GameManager gameManager;

    public VotingGUI(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, ChatColor.GOLD + "Play Again?");
    }

    @Override
    public void decorate(Player player) {
        addButton(11, new InventoryButton()
            .creator(p -> buildItem(Material.LIME_CONCRETE, ChatColor.GREEN + "" + ChatColor.BOLD + "YES", ChatColor.GRAY + "Vote to play again!"))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                gameManager.castVote(clicker, true);
                clicker.closeInventory();
            })
        );

        addButton(15, new InventoryButton()
            .creator(p -> buildItem(Material.RED_CONCRETE, ChatColor.RED + "" + ChatColor.BOLD + "NO", ChatColor.GRAY + "Vote to return to idle."))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                gameManager.castVote(clicker, false);
                clicker.closeInventory();
            })
        );

        super.decorate(player);
    }

    private ItemStack buildItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }
}