package com.metrovice.rp.police;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class PoliceLaptopListener implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey key;

    public PoliceLaptopListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "police_mdt_laptop");
    }

    public ItemStack createLaptop() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(61002);
        meta.setDisplayName(MetroViceRP.color("&9&lPolice MDT Laptop"));
        meta.setLore(List.of(
                MetroViceRP.color("&7جهاز نظام الشرطة"),
                MetroViceRP.color("&eاضغط بزر الفأرة الأيمن لفتح MDT")
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.performCommand("mdt");
    }
}