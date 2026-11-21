package com.metrovice.rp.character;

import com.metrovice.rp.MetroViceRP;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;

public final class CharacterMenu {
    private CharacterMenu() {}

    public static void open(MetroViceRP plugin, CharacterService service, Player player) throws SQLException {
        List<CharacterProfile> profiles = service.list(player.getUniqueId());
        CharacterProfile active = service.active(player.getUniqueId()).orElse(null);
        CharacterMenuHolder holder = new CharacterMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27, MetroViceRP.color("&8Metro Vice &b| &fشخصياتك"));
        holder.setInventory(inventory);

        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        NamespacedKey key = new NamespacedKey(plugin, "character_id");
        for (int i = 0; i < Math.min(profiles.size(), slots.length); i++) {
            CharacterProfile profile = profiles.get(i);
            ItemStack item = new ItemStack(profile.id() == (active == null ? -1 : active.id())
                    ? Material.EMERALD : Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MetroViceRP.color((profile.id() == (active == null ? -1 : active.id()) ? "&a" : "&b") + profile.name()));
            meta.setLore(List.of(
                    MetroViceRP.color("&7العمر: &f" + profile.age()),
                    MetroViceRP.color("&7الوظيفة: &f" + profile.job()),
                    MetroViceRP.color("&7الرتبة: &f" + profile.rank()),
                    MetroViceRP.color("&7النقد: &a$" + profile.cash()),
                    MetroViceRP.color("&7البنك: &a$" + profile.bank()),
                    MetroViceRP.color(profile.id() == (active == null ? -1 : active.id()) ? "&aمفعلة حاليًا" : "&eاضغط للتبديل"),
                    MetroViceRP.color("&8ID: " + profile.id())
            ));
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, profile.id());
            item.setItemMeta(meta);
            inventory.setItem(slots[i], item);
        }

        if (profiles.isEmpty()) {
            ItemStack first = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta firstMeta = first.getItemMeta();
            firstMeta.setDisplayName(MetroViceRP.color("&aإنشاء شخصيتك الأولى"));
            firstMeta.setLore(List.of(
                    MetroViceRP.color("&7ابدأ حياتك في مدينة Metro Vice RP"),
                    MetroViceRP.color("&eاكتب في الدردشة:"),
                    MetroViceRP.color("&f/character create <name> <age>"),
                    MetroViceRP.color("&7مثال: /character create Ahmed 25"),
                    MetroViceRP.color("&8بعد الإنشاء سيتم تفعيلها تلقائيًا")
            ));
            first.setItemMeta(firstMeta);
            inventory.setItem(13, first);
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(MetroViceRP.color("&bإدارة الشخصيات"));
        infoMeta.setLore(List.of(
                MetroViceRP.color("&7لإنشاء شخصية:"),
                MetroViceRP.color("&f/character create <name> <age>"),
                MetroViceRP.color("&7لتغيير الاسم:"),
                MetroViceRP.color("&f/character rename <id> <name>"),
                MetroViceRP.color("&7للحذف:"),
                MetroViceRP.color("&f/character delete <id> CONFIRM")
        ));
        info.setItemMeta(infoMeta);
        inventory.setItem(22, info);
        player.openInventory(inventory);
    }
}