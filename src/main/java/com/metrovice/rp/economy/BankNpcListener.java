package com.metrovice.rp.economy;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;

public final class BankNpcListener implements Listener {
    private final MetroViceRP plugin;
    private final BankService bank;
    private final NamespacedKey npcKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey amountKey;

    public BankNpcListener(MetroViceRP plugin, BankService bank) {
        this.plugin = plugin;
        this.bank = bank;
        this.npcKey = new NamespacedKey(plugin, "bank_employee");
        this.actionKey = new NamespacedKey(plugin, "bank_action");
        this.amountKey = new NamespacedKey(plugin, "bank_amount");
    }

    public Villager spawnEmployee(Location location) {
        Villager villager = location.getWorld().spawn(location, Villager.class, npc -> {
            npc.setCustomName(MetroViceRP.color("&b&lموظف البنك"));
            npc.setCustomNameVisible(true);
            npc.setProfession(Villager.Profession.LIBRARIAN);
            npc.setAI(false);
            npc.setInvulnerable(true);
            npc.setPersistent(true);
            npc.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);
        });
        plugin.getConfig().set("bank.employee.enabled", true);
        plugin.getConfig().set("bank.employee.world", location.getWorld().getName());
        plugin.getConfig().set("bank.employee.x", location.getX());
        plugin.getConfig().set("bank.employee.y", location.getY());
        plugin.getConfig().set("bank.employee.z", location.getZ());
        plugin.getConfig().set("bank.employee.yaw", location.getYaw());
        plugin.getConfig().set("bank.employee.pitch", location.getPitch());
        plugin.getConfig().set("bank.employee.uuid", villager.getUniqueId().toString());
        plugin.saveConfig();
        return villager;
    }

    public void restoreEmployee() {
        if (!plugin.getConfig().getBoolean("bank.employee.enabled", false)) return;
        String worldName = plugin.getConfig().getString("bank.employee.world", "");
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            plugin.getLogger().warning("Bank employee world is missing; use /mvrp banknpc to place the employee again.");
            return;
        }
        Location location = new Location(Bukkit.getWorld(worldName),
                plugin.getConfig().getDouble("bank.employee.x"),
                plugin.getConfig().getDouble("bank.employee.y"),
                plugin.getConfig().getDouble("bank.employee.z"),
                (float) plugin.getConfig().getDouble("bank.employee.yaw"),
                (float) plugin.getConfig().getDouble("bank.employee.pitch"));
        // Load the saved chunk before checking its entity UUID, so an NPC in an
        // otherwise-unloaded chunk is not duplicated after a restart.
        location.getChunk().load();
        String savedId = plugin.getConfig().getString("bank.employee.uuid", "");
        if (savedId != null && !savedId.isBlank()) {
            try {
                Entity existing = Bukkit.getEntity(java.util.UUID.fromString(savedId));
                if (existing != null && existing.isValid()) return;
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Saved bank employee UUID is invalid; creating a replacement.");
            }
        }
        spawnEmployee(location);
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Byte marker = entity.getPersistentDataContainer().get(npcKey, PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1) return;
        event.setCancelled(true);
        openMenu(event.getPlayer());
    }

    @EventHandler
    public void protectNpc(EntityDamageEvent event) {
        Byte marker = event.getEntity().getPersistentDataContainer().get(npcKey, PersistentDataType.BYTE);
        if (marker != null && marker == (byte) 1) event.setCancelled(true);
    }

    private void openMenu(Player player) {
        try {
            BankService.Balance balance = bank.balance(player.getUniqueId());
            Inventory inventory = Bukkit.createInventory(null, 27, MetroViceRP.color("&8البنك &b| &fخدمة الموظف"));
            inventory.setItem(4, item(Material.PLAYER_HEAD, "&b&lحسابك البنكي", List.of(
                    "&7النقد معك: &a$" + balance.cash(),
                    "&7رصيد البنك: &b$" + balance.bank(),
                    "&8كل شخصية لها حساب مستقل"
            ), null, 0));
            long[] amounts = {100, 500, 1000};
            int[] depositSlots = {11, 12, 13};
            int[] withdrawSlots = {15, 14, 10};
            for (int i = 0; i < amounts.length; i++) {
                inventory.setItem(depositSlots[i], item(Material.EMERALD, "&aإيداع $" + amounts[i],
                        List.of("&7إيداع المبلغ من النقد إلى البنك"), "deposit", amounts[i]));
                inventory.setItem(withdrawSlots[i], item(Material.GOLD_INGOT, "&eسحب $" + amounts[i],
                        List.of("&7سحب المبلغ من البنك إلى النقد"), "withdraw", amounts[i]));
            }
            inventory.setItem(22, item(Material.BARRIER, "&cإغلاق", List.of("&7إغلاق نافذة البنك"), "close", 0));
            player.openInventory(inventory);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
            player.sendMessage(MetroViceRP.color("&7أنشئ أو فعّل شخصية أولًا عبر &f/character"));
        } catch (SQLException ex) {
            plugin.getLogger().severe("Could not open bank employee menu: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر جلب حسابك البنكي."));
        }
    }

    private ItemStack item(Material material, String name, List<String> lore, String action, long amount) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(MetroViceRP.color(name));
        meta.setLore(lore.stream().map(MetroViceRP::color).toList());
        if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (amount > 0) meta.getPersistentDataContainer().set(amountKey, PersistentDataType.LONG, amount);
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(MetroViceRP.color("&8البنك &b| &fخدمة الموظف"))) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;
        if (action.equals("close")) { player.closeInventory(); return; }
        Long amount = item.getItemMeta().getPersistentDataContainer().get(amountKey, PersistentDataType.LONG);
        if (amount == null) return;
        try {
            if (action.equals("deposit")) {
                bank.deposit(player.getUniqueId(), amount);
                player.sendMessage(MetroViceRP.color("&aتم إيداع $" + amount + " بنجاح."));
            } else if (action.equals("withdraw")) {
                bank.withdraw(player.getUniqueId(), amount);
                player.sendMessage(MetroViceRP.color("&aتم سحب $" + amount + " بنجاح."));
            }
            openMenu(player);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
            openMenu(player);
        } catch (SQLException ex) {
            plugin.getLogger().severe("Bank employee transaction failed: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر تنفيذ العملية البنكية."));
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(MetroViceRP.color("&8البنك &b| &fخدمة الموظف"))) event.setCancelled(true);
    }
}