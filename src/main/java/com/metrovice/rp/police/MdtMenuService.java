package com.metrovice.rp.police;

import com.metrovice.rp.MetroViceRP;
import com.metrovice.rp.character.CharacterProfile;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.department.DepartmentService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;

public final class MdtMenuService {
    private final JavaPlugin plugin;
    private final CharacterService characters;
    private final DepartmentService departments;

    public MdtMenuService(JavaPlugin plugin, CharacterService characters, DepartmentService departments) {
        this.plugin = plugin; this.characters = characters; this.departments = departments;
    }

    public boolean authorized(Player player) throws SQLException {
        return departments.isOnDuty(player.getUniqueId(), "POLICE");
    }

    public void openMain(Player player) throws SQLException {
        if (!authorized(player)) { player.sendMessage(MetroViceRP.color("&cنظام MDT متاح للشرطة أثناء المناوبة فقط.")); return; }
        Inventory inv = Bukkit.createInventory(new MdtMenuHolder("main"), 45, "§1§lMETRO VICE §8| §bPOLICE MDT");
        inv.setItem(20, item(Material.PLAYER_HEAD, "§bسجل المناوبين", "§7عرض أفراد الشرطة على رأس عملهم"));
        inv.setItem(22, item(Material.COMPASS, "§dالبحث عن مواطن", "§7بحث بالاسم أو رقم الهوية"));
        inv.setItem(24, item(Material.WRITABLE_BOOK, "§eإدارة الرتب", "§7عرض التسلسل وترقية الأفراد"));
        inv.setItem(40, item(Material.BARRIER, "§cإغلاق", "§7إغلاق واجهة MDT"));
        player.openInventory(inv);
    }

    public void openRoster(Player player) throws SQLException {
        Inventory inv = Bukkit.createInventory(new MdtMenuHolder("roster"), 54, "§1POLICE MDT §8| §bالمناوبون");
        List<DepartmentService.DutyMember> roster = departments.onDuty("POLICE");
        int slot = 0;
        for (DepartmentService.DutyMember member : roster) {
            if (slot >= 45) break;
            inv.setItem(slot++, item(Material.PLAYER_HEAD, "§f" + member.characterName(),
                    "§7الرتبة: §b" + member.rank(), "§8المدة: " + duration(member.dutySeconds())));
        }
        inv.setItem(49, item(Material.ARROW, "§eالعودة", "§7القائمة الرئيسية"));
        player.openInventory(inv);
    }

    public void openRanks(Player player) throws SQLException {
        if (!player.hasPermission("metrovice.admin")) {
            player.sendMessage(MetroViceRP.color("&cإدارة الرتب تحتاج صلاحية الإدارة."));
            return;
        }
        Inventory inv = Bukkit.createInventory(new MdtMenuHolder("ranks"), 54, "§1POLICE MDT §8| §eسُلّم الرتب");
        List<String> ranks = plugin.getConfig().getStringList("departments.police-ranks");
        int slot = 0;
        for (String rank : ranks) {
            if (slot >= 45) break;
            inv.setItem(slot++, item(Material.NAME_TAG, "§b" + rank, "§7اضغط لاختيار الرتبة", "§8ثم اكتب رقم هوية الفرد"));
        }
        inv.setItem(49, item(Material.ARROW, "§eالعودة", "§7القائمة الرئيسية"));
        player.openInventory(inv);
    }

    public void openSearchResults(Player player, String query) throws SQLException {
        Inventory inv = Bukkit.createInventory(new MdtMenuHolder("results"), 54, "§1POLICE MDT §8| §bنتائج البحث");
        if (query.matches("\\d+")) {
            try {
                CharacterProfile profile = characters.findByIdentityNumber(Integer.parseInt(query)).orElse(null);
                if (profile != null) inv.setItem(22, citizenItem(profile));
            } catch (NumberFormatException ignored) { }
        } else {
            int slot = 0;
            for (CharacterProfile profile : characters.searchByName(query)) {
                if (slot >= 45) break;
                inv.setItem(slot++, citizenItem(profile));
            }
        }
        inv.setItem(49, item(Material.ARROW, "§eالعودة", "§7القائمة الرئيسية"));
        player.openInventory(inv);
    }

    private ItemStack citizenItem(CharacterProfile p) {
        return item(Material.PAPER, "§f" + p.name(), "§7رقم الهوية: §b" + p.id(),
                "§7العمر: §f" + p.age(), "§7الوظيفة: §f" + p.job(), "§7الرتبة: §f" + p.rank());
    }

    private String duration(long seconds) {
        return (seconds / 3600) + " ساعة و " + ((seconds % 3600) / 60) + " دقيقة";
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        stack.setItemMeta(meta);
        return stack;
    }
}
