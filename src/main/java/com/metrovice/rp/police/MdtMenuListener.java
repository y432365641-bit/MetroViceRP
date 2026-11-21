package com.metrovice.rp.police;

import com.metrovice.rp.MetroViceRP;
import com.metrovice.rp.department.DepartmentService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MdtMenuListener implements Listener {
    private record Prompt(String action, String rank) {}
    private final JavaPlugin plugin;
    private final MdtMenuService menus;
    private final DepartmentService departments;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public MdtMenuListener(JavaPlugin plugin, MdtMenuService menus, DepartmentService departments) {
        this.plugin = plugin; this.menus = menus; this.departments = departments;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MdtMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != top) return;
        int slot = event.getRawSlot();
        try {
            switch (holder.view()) {
                case "main" -> {
                    if (slot == 20) menus.openRoster(player);
                    else if (slot == 22) {
                        prompts.put(player.getUniqueId(), new Prompt("search", ""));
                        player.closeInventory();
                        player.sendMessage(MetroViceRP.color("&bاكتب اسم المواطن أو رقم هويته في الدردشة:"));
                    } else if (slot == 24) menus.openRanks(player);
                    else if (slot == 40) player.closeInventory();
                }
                case "roster", "results" -> {
                    if (slot == 49) menus.openMain(player);
                }
                case "ranks" -> {
                    if (slot == 49) { menus.openMain(player); return; }
                    if (slot >= 0 && slot < 45 && player.hasPermission("metrovice.admin")) {
                        var item = top.getItem(slot);
                        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return;
                        String rank = item.getItemMeta().getDisplayName().replace("§b", "");
                        prompts.put(player.getUniqueId(), new Prompt("promote", rank));
                        player.closeInventory();
                        player.sendMessage(MetroViceRP.color("&eاكتب رقم هوية الشخصية التي تريد تعيين رتبتها إلى &b" + rank + "&e:"));
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("MDT menu error: " + ex.getMessage());
            player.sendMessage(MetroViceRP.color("&cتعذر تحميل بيانات MDT."));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Prompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String input = event.getMessage().strip();
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (!menus.authorized(player)) {
                    player.sendMessage(MetroViceRP.color("&cيجب أن تكون على رأس عمل الشرطة."));
                    return;
                }
                if (prompt.action().equals("search")) {
                    if (input.length() < 2 || input.length() > 32) {
                        player.sendMessage(MetroViceRP.color("&cالمدخل غير صالح."));
                        return;
                    }
                    menus.openSearchResults(player, input);
                } else if (prompt.action().equals("promote")) {
                    if (!player.hasPermission("metrovice.admin")) {
                        player.sendMessage(MetroViceRP.color("&cليس لديك صلاحية."));
                        return;
                    }
                    int id = Integer.parseInt(input);
                    departments.changeRankByCharacterId(id, "POLICE", prompt.rank());
                    player.sendMessage(MetroViceRP.color("&aتم تحديث الرتبة إلى " + prompt.rank() + " للشخصية رقم " + id));
                    menus.openMain(player);
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(MetroViceRP.color("&cيجب إدخال رقم هوية صحيح."));
            } catch (IllegalArgumentException ex) {
                player.sendMessage(MetroViceRP.color("&c" + ex.getMessage()));
            } catch (SQLException ex) {
                plugin.getLogger().severe("MDT chat action failed: " + ex.getMessage());
                player.sendMessage(MetroViceRP.color("&cتعذر تنفيذ العملية."));
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MdtMenuHolder) return;
    }
}
