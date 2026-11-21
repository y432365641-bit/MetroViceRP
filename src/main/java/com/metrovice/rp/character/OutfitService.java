package com.metrovice.rp.character;

import com.metrovice.rp.storage.Database;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class OutfitService {
    private final Database database;
    private final CharacterService characters;

    public OutfitService(Database database, CharacterService characters) {
        this.database = database;
        this.characters = characters;
    }

    public void save(Player player, String name) throws SQLException, IOException {
        String clean = cleanName(name);
        CharacterProfile active = characters.active(player.getUniqueId())
                .orElseThrow(() -> new IllegalArgumentException("فعّل شخصية أولًا."));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(player.getInventory().getArmorContents());
        }
        try (PreparedStatement ps = database.connection().prepareStatement("""
                INSERT INTO character_outfits(character_id, outfit_name, armor_blob)
                VALUES(?,?,?)
                ON CONFLICT(character_id, outfit_name) DO UPDATE SET
                    armor_blob=excluded.armor_blob, created_at=CURRENT_TIMESTAMP
                """)) {
            ps.setInt(1, active.id());
            ps.setString(2, clean);
            ps.setBytes(3, bytes.toByteArray());
            ps.executeUpdate();
        }
    }

    public boolean load(Player player, String name) throws SQLException, IOException, ClassNotFoundException {
        CharacterProfile active = characters.active(player.getUniqueId())
                .orElseThrow(() -> new IllegalArgumentException("فعّل شخصية أولًا."));
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT armor_blob FROM character_outfits WHERE character_id=? AND outfit_name=? COLLATE NOCASE")) {
            ps.setInt(1, active.id());
            ps.setString(2, cleanName(name));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(rs.getBytes(1)))) {
                    ItemStack[] armor = (ItemStack[]) in.readObject();
                    player.getInventory().setArmorContents(armor);
                    player.updateInventory();
                    return true;
                }
            }
        }
    }

    public List<String> list(Player player) throws SQLException {
        CharacterProfile active = characters.active(player.getUniqueId())
                .orElseThrow(() -> new IllegalArgumentException("فعّل شخصية أولًا."));
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT outfit_name FROM character_outfits WHERE character_id=? ORDER BY outfit_name")) {
            ps.setInt(1, active.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString(1));
            }
        }
        return names;
    }

    private String cleanName(String raw) {
        String value = raw == null ? "" : raw.strip().replaceAll("\\s+", " ");
        if (value.isEmpty() || value.length() > 24 || value.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("اسم الطقم يجب أن يكون من 1 إلى 24 حرفًا.");
        return value;
    }
}
