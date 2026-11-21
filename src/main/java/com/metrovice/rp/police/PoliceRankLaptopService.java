package com.metrovice.rp.police;

import com.metrovice.rp.MetroViceRP;
import com.metrovice.rp.department.DepartmentService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;

public final class PoliceRankLaptopService implements Listener, CommandExecutor {
    private final JavaPlugin plugin;
    private final DepartmentService departments;
    private final NamespacedKey laptopKey;
    private final NamespacedKey characterIdKey;
    private final NamespacedKey rankKey;

    public PoliceRankLaptopService(JavaPlugin plugin, DepartmentService departments) {
        this.plugin = plugin;
        this.departments = departments;
        laptopKey = new NamespacedKey(plugin, "police_rank_laptop");
        characterIdKey = new NamespacedKey(plugin, "rank_laptop_character_id");
        rankKey = new NamespacedKey(plugin, "rank_laptop_rank");
    }

    public ItemStack createLaptop() {
        ItemStack item = new ItemStack(Material.LECTERN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MetroViceRP.color("&d&lPolice Rank Manager"));
        meta.setLore(List.of(
                MetroViceRP.color("&7حاسوب إدارة رتب الشرطة"),
                MetroViceRP.color("&eمتاح لرتبة ملازم فما فوق")));
        meta.getPersistentDataContainer().set(laptopKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()
                || !item.getItemMeta().getPersistentDataContainer().has(laptopKey, PersistentDataType.BYTE)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!hasAuthority(player)) {
            player.sendMessage(MetroViceRP.color("&cحاسوب إدارة الرتب متاح للشرطة من رتبة ملازم فما فوق أثناء المناوبة."));
            return;
        }
        openStaff(player);
    }

    private boolean hasAuthority(Player player) {
        try {
            List<String> order = plugin.getConfig().getStringList("departments.police-ranks");
            int lieutenant = order.indexOf("ملازم");
            if (lieutenant < 0) return false;
            return departments.memberships(player.getUniqueId()).stream()
                    .anyMatch(m -> m.department().equals("POLICE") && m.onDuty()
                            && order.indexOf(m.rank()) >= 0 && order.indexOf(m.rank()) <= lieutenant);
        } catch (SQLException ex) {
            plugin.getLogger().warning("Rank laptop permission check failed: " + ex.getMessage());
            return false;
        }
    }

    private void openStaff(Player player) {
        try {
            MdtMenuHolder holder = new MdtMenuHolder("rankstaff");
            Inventory inv = Bukkit.createInventory(holder, 54, "§1POLICE §8| §dإدارة الأفراد");
            int slot = 0;
            for (DepartmentService.RosterMember member : departments.roster("POLICE")) {
                if (slot >= 45) break;
                ItemStack entry = button(Material.PLAYER_HEAD, "&f" + member.characterName(),
                        "&7رقم الشخصية: &b" + member.characterId(),
                        "&7الرتبة الحالية: &f" + member.rank(),
                        member.onDuty() ? "&aعلى رأس العمل" : "&7خارج الخدمة");
                entry.editMeta(meta -> meta.getPersistentDataContainer().set(characterIdKey,
                        PersistentDataType.INTEGER, member.characterId()));
                inv.setItem(slot++, entry);
            }
            if (slot == 0) inv.setItem(22, button(Material.BARRIER, "&7لا يوجد أفراد مسجلون"));
            inv.setItem(49, button(Material.BARRIER, "&cإغلاق"));
            player.openInventory(inv);
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not load police rank roster: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر تحميل سجل الشرطة."));
        }
    }

    private void openRanks(Player player, int characterId) {
        MdtMenuHolder holder = new MdtMenuHolder("rankchoices");
        Inventory inv = Bukkit.createInventory(holder, 54, "§1POLICE §8| §eاختيار الرتبة");
        List<String> ranks = plugin.getConfig().getStringList("departments.police-ranks");
        int slot = 0;
        for (String rank : ranks) {
            if (slot >= 45) break;
            ItemStack entry = button(Material.NAME_TAG, "&b" + rank, "&7اضغط لتعيين الرتبة");
            entry.editMeta(meta -> {
                meta.getPersistentDataContainer().set(characterIdKey, PersistentDataType.INTEGER, characterId);
                meta.getPersistentDataContainer().set(rankKey, PersistentDataType.STRING, rank);
            });
            inv.setItem(slot++, entry);
        }
        inv.setItem(49, button(Material.ARROW, "&eالعودة للأفراد"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MdtMenuHolder holder)) return;
        if (!holder.view().equals("rankstaff") && !holder.view().equals("rankchoices")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (!hasAuthority(player)) {
            player.closeInventory();
            player.sendMessage(MetroViceRP.color("&cلم تعد تملك صلاحية إدارة الرتب."));
            return;
        }
        int slot = event.getRawSlot();
        if (holder.view().equals("rankstaff")) {
            if (slot == 49) { player.closeInventory(); return; }
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;
            Integer id = item.getItemMeta().getPersistentDataContainer().get(characterIdKey, PersistentDataType.INTEGER);
            if (id != null) openRanks(player, id);
        } else {
            if (slot == 49) { openStaff(player); return; }
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;
            var data = item.getItemMeta().getPersistentDataContainer();
            Integer id = data.get(characterIdKey, PersistentDataType.INTEGER);
            String rank = data.get(rankKey, PersistentDataType.STRING);
            if (id == null || rank == null) return;
            try {
                departments.changeRankByCharacterId(id, "POLICE", rank);
                player.sendMessage(MetroViceRP.color("&aتم تحديث رتبة الشخصية رقم " + id + " إلى " + rank));
                openStaff(player);
            } catch (SQLException | IllegalArgumentException ex) {
                player.sendMessage(MetroViceRP.color("&cتعذر تغيير الرتبة: " + ex.getMessage()));
            }
        }
    }

    private ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MetroViceRP.color(name));
        meta.setLore(java.util.Arrays.stream(lore).map(MetroViceRP::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("metrovice.admin")) {
            sender.sendMessage(MetroViceRP.color("&cليس لديك صلاحية."));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(MetroViceRP.color("&e/ranklaptopgive <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(MetroViceRP.color("&cاللاعب غير متصل."));
            return true;
        }
        if (!target.getInventory().addItem(createLaptop()).isEmpty()) {
            sender.sendMessage(MetroViceRP.color("&cمخزون اللاعب ممتلئ."));
            return true;
        }
        sender.sendMessage(MetroViceRP.color("&aتم تسليم حاسوب إدارة الرتب إلى " + target.getName()));
        return true;
    }
}
