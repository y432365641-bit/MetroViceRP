package com.metrovice.rp.inventory;

import com.metrovice.rp.character.CharacterProfile;
import com.metrovice.rp.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public final class InventoryService {
    private static final int STORAGE_SIZE = 36;
    private final JavaPlugin plugin;
    private final Database database;

    public InventoryService(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void open(Player player, CharacterProfile character) throws SQLException {
        ItemStack[] contents = load(character.id());
        if (contents == null) {
            // One-time migration: preserve the character's existing vanilla items.
            contents = Arrays.copyOf(player.getInventory().getStorageContents(), STORAGE_SIZE);
            ItemStack[] armor = player.getInventory().getArmorContents();
            ItemStack[] extra = player.getInventory().getExtraContents();
            save(character.id(), contents);
            player.getInventory().clear();
            player.getInventory().setArmorContents(armor);
            player.getInventory().setExtraContents(extra);
            player.updateInventory();
        }
        Inventory gui = Bukkit.createInventory(new InventoryHolderImpl(character.id()), 54,
                ChatColor.DARK_GRAY + "Metro Vice | الحقيبة");
        for (int slot = 0; slot < STORAGE_SIZE; slot++) {
            gui.setItem(slot, contents[slot] == null ? null : contents[slot].clone());
        }
        ItemStack border = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 36; slot < 54; slot++) gui.setItem(slot, border);
        gui.setItem(49, weightItem(contents));
        player.openInventory(gui);
    }

    public boolean isBag(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof InventoryHolderImpl;
    }

    public int characterId(Inventory inventory) {
        return ((InventoryHolderImpl) inventory.getHolder()).characterId();
    }

    public void saveFrom(Inventory inventory) throws SQLException {
        if (!isBag(inventory)) return;
        ItemStack[] contents = new ItemStack[STORAGE_SIZE];
        for (int slot = 0; slot < STORAGE_SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            contents[slot] = item == null || item.getType().isAir() ? null : item.clone();
        }
        save(characterId(inventory), contents);
        inventory.setItem(49, weightItem(contents));
    }

    public void refresh(Inventory inventory) throws SQLException {
        if (!isBag(inventory)) return;
        ItemStack[] contents = load(characterId(inventory));
        if (contents == null) contents = new ItemStack[STORAGE_SIZE];
        for (int slot = 0; slot < STORAGE_SIZE; slot++) {
            inventory.setItem(slot, contents[slot] == null ? null : contents[slot].clone());
        }
        inventory.setItem(49, weightItem(contents));
    }

    private ItemStack[] load(int characterId) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT items_blob FROM character_inventory WHERE character_id=?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                byte[] bytes = rs.getBytes(1);
                try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                    return Arrays.copyOf((ItemStack[]) in.readObject(), STORAGE_SIZE);
                } catch (IOException | ClassNotFoundException | ClassCastException ex) {
                    throw new SQLException("Could not read saved character inventory", ex);
                }
            }
        }
    }

    private void save(int characterId, ItemStack[] contents) throws SQLException {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
                out.writeObject(Arrays.copyOf(contents, STORAGE_SIZE));
            }
            try (PreparedStatement ps = database.connection().prepareStatement("""
                    INSERT INTO character_inventory(character_id, items_blob, updated_at)
                    VALUES(?,?,CURRENT_TIMESTAMP)
                    ON CONFLICT(character_id) DO UPDATE SET
                    items_blob=excluded.items_blob, updated_at=CURRENT_TIMESTAMP
                    """)) {
                ps.setInt(1, characterId);
                ps.setBytes(2, bytes.toByteArray());
                ps.executeUpdate();
            }
        } catch (IOException ex) {
            throw new SQLException("Could not save character inventory", ex);
        }
    }

    public boolean canPlace(Inventory inventory, int slot, ItemStack cursor) {
        if (cursor == null || cursor.getType().isAir()) return true;
        double weight = 0;
        for (int i = 0; i < STORAGE_SIZE; i++) {
            ItemStack current = inventory.getItem(i);
            if (current != null && !current.getType().isAir()) weight += unitWeight(current) * current.getAmount();
        }
        ItemStack target = inventory.getItem(slot);
        if (target != null && !target.getType().isAir() && !target.isSimilar(cursor)) {
            weight -= unitWeight(target) * target.getAmount();
        }
        double projected = weight + unitWeight(cursor) * cursor.getAmount();
        double max = Math.max(1, plugin.getConfig().getDouble("inventory.max-weight-kg", 30.0));
        return projected <= max + 0.0001;
    }

    public boolean canPickup(Player player, int characterId, ItemStack incoming) throws SQLException {
        ItemStack[] contents = load(characterId);
        if (contents == null) contents = Arrays.copyOf(player.getInventory().getStorageContents(), STORAGE_SIZE);
        double weight = 0;
        for (ItemStack current : contents) {
            if (current != null && !current.getType().isAir()) {
                weight += unitWeight(current) * current.getAmount();
            }
        }
        double max = Math.max(1, plugin.getConfig().getDouble("inventory.max-weight-kg", 30.0));
        return weight + unitWeight(incoming) * incoming.getAmount() <= max + 0.0001;
    }

    private double unitWeight(ItemStack item) {
        return Math.max(0, plugin.getConfig().getDouble(
                "inventory.item-weights." + item.getType().name(), 0.1));
    }

    private ItemStack weightItem(ItemStack[] contents) {
        double weight = 0;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                weight += unitWeight(item) * item.getAmount();
            }
        }
        double max = Math.max(1, plugin.getConfig().getDouble("inventory.max-weight-kg", 30.0));
        ItemStack item = named(Material.BOOK,
                ChatColor.AQUA + "الوزن: " + ChatColor.WHITE
                        + String.format(java.util.Locale.US, "%.1f / %.1f KG", weight, max));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(java.util.List.of(ChatColor.GRAY + "يتم حفظ الحقيبة لكل شخصية بشكل مستقل."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}