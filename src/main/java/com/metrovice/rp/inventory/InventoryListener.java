package com.metrovice.rp.inventory;

import com.metrovice.rp.character.CharacterService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

        if (event.getClickedInventory() != top || event.getRawSlot() < 0 || event.getRawSlot() >= 36) {
            event.setCancelled(true);
            return;
        }

        InventoryAction action = event.getAction();
        boolean safeClick = action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.PICKUP_HALF
                || action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME
                || action == InventoryAction.SWAP_WITH_CURSOR;
        if (!safeClick || event.isShiftClick() || event.getHotbarButton() >= 0) {
            event.setCancelled(true);
            return;
        }

        if (event.getCursor() != null && !inventories.canPlace(top, event.getRawSlot(), event.getCursor())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage("§cالحقيبة ممتلئة! لا يمكن تجاوز الوزن الأقصى.");
            }
            return;
        }

        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try { inventories.saveFrom(top); }
                catch (SQLException ex) {
                    plugin.getLogger().severe("Could not save bag for " + player.getName() + ": " + ex.getMessage());
                    player.sendMessage("§cتعذر حفظ الحقيبة. أبلغ الإدارة.");
                }
            });
        }
    }

    @EventHandler
    public void onBagDrag(InventoryDragEvent event) {
        if (inventories.isBag(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        try {
            var active = characters.active(player.getUniqueId());
            if (active.isPresent() && !inventories.canPickup(
                    player, active.get().id(), event.getItem().getItemStack())) {
                event.setCancelled(true);
                player.sendMessage("§cالحقيبة ممتلئة! لا يمكنك التقاط هذا الغرض.");
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not validate bag capacity for " + player.getName() + ": " + ex.getMessage());
            event.setCancelled(true);
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