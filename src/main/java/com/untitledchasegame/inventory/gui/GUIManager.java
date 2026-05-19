package com.untitledchasegame.inventory.gui;

import com.untitledchasegame.inventory.InventoryGUI;
import com.untitledchasegame.inventory.InventoryHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class GUIManager {
    private final Map<Inventory, InventoryHandler> activeInventories = new HashMap<>();

    public void openGUI(InventoryGUI gui, Player player) {
        activeInventories.put(gui.getInventory(), gui);
        player.openInventory(gui.getInventory());
    }

    public void handleClick(InventoryClickEvent event) {
        InventoryHandler handler = activeInventories.get(event.getInventory());
        if (handler != null) handler.onClick(event);
    }

    public void handleOpen(InventoryOpenEvent event) {
        InventoryHandler handler = activeInventories.get(event.getInventory());
        if (handler != null) handler.onOpen(event);
    }

    public void handleClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        InventoryHandler handler = activeInventories.remove(inv);
        if (handler != null) handler.onClose(event);
    }
}