package com.metrovice.rp.inventory;

import com.metrovice.rp.character.CharacterService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class InventoryListener implements Listener {
    private final JavaPlugin plugin;
    private final InventoryService inventories;
    private final CharacterService characters;

    public InventoryListener(JavaPlugin plugin, InventoryService inventories, CharacterService characters) {
        this.plugin = plugin;
        this.inventories = inventories;
        this.characters = characters;
    }

    @EventHandler
    public void onBagClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!inventories.isBag(top)) return;
        // The lower player inventory and decorative weight panel are locked for this first safe version.
        if (event.getClickedInventory() != top || event.getRawSlot() >= 36) {
            event.setCancelled(true);
            return;
        }
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try { inventories.saveFrom(top); }
                catch (SQLException ex) {
                    plugin.getLogger().severe("Could not save bag for " + player.getName() + ": " + ex.getMessage());
                    player.sendMessage("§cتعذر حفظ الحقيبة. لا تغلقها قبل إبلاغ الإدارة.");
                }
            });
        }
    }

    @EventHandler
    public void onBagClose(InventoryCloseEvent event) {
        Inventory top = event.getInventory();
        if (!inventories.isBag(top)) return;
        try { inventories.saveFrom(top); }
        catch (SQLException ex) {
            plugin.getLogger().severe("Could not save bag on close: " + ex.getMessage());
            event.getPlayer().sendMessage("§cتعذر حفظ الحقيبة. راجع الإدارة.");
        }
    }
}