package com.metrovice.rp.character;

import com.metrovice.rp.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.UUID;

public final class CharacterStateService {
    private final Database database;

    public CharacterStateService(Database database) { this.database = database; }

    public void save(Player player, int characterId) throws SQLException {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
                out.writeObject(player.getInventory().getContents());
                out.writeObject(player.getInventory().getArmorContents());
                out.writeObject(player.getEnderChest().getContents());
                out.writeFloat(player.getExp());
                out.writeInt(player.getLevel());
                out.writeDouble(player.getHealth());
                out.writeInt(player.getFoodLevel());
                out.writeFloat(player.getSaturation());
                Location loc = player.getLocation();
                out.writeObject(loc.getWorld() == null ? "" : loc.getWorld().getName());
                out.writeDouble(loc.getX()); out.writeDouble(loc.getY()); out.writeDouble(loc.getZ());
                out.writeFloat(loc.getYaw()); out.writeFloat(loc.getPitch());
            }
            try (PreparedStatement ps = database.connection().prepareStatement("""
                INSERT INTO character_state(character_id, state_data, updated_at) VALUES(?,?,CURRENT_TIMESTAMP)
                ON CONFLICT(character_id) DO UPDATE SET state_data=excluded.state_data, updated_at=CURRENT_TIMESTAMP
                """)) {
                ps.setInt(1, characterId); ps.setBytes(2, bytes.toByteArray()); ps.executeUpdate();
            }
        } catch (IOException ex) {
            throw new SQLException("Could not serialize character state", ex);
        }
    }

    public void load(Player player, int characterId) throws SQLException {
        byte[] data = null;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT state_data FROM character_state WHERE character_id=?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) data = rs.getBytes(1); }
        }
        if (data == null) {
            clearPlayerState(player);
            return;
        }
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            ItemStack[] inventory = (ItemStack[]) in.readObject();
            ItemStack[] armor = (ItemStack[]) in.readObject();
            ItemStack[] ender = (ItemStack[]) in.readObject();
            float exp = in.readFloat();
            int level = in.readInt();
            double health = in.readDouble();
            int food = in.readInt();
            float saturation = in.readFloat();
            String worldName = (String) in.readObject();
            double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
            float yaw = in.readFloat(), pitch = in.readFloat();

            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.getEnderChest().setContents(ender);
            player.setExp(Math.max(0, Math.min(1, exp)));
            player.setLevel(Math.max(0, level));
            player.setFoodLevel(Math.max(0, Math.min(20, food)));
            player.setSaturation(Math.max(0, saturation));
            player.setHealth(Math.max(1, Math.min(player.getMaxHealth(), health)));

            World world = Bukkit.getWorld(worldName);
            if (world != null && world.equals(player.getWorld())) {
                player.teleport(new Location(world, x, y, z, yaw, pitch));
            }
            player.updateInventory();
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            throw new SQLException("Could not deserialize character state", ex);
        }
    }

    public void clearPlayerState(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getEnderChest().clear();
        player.setExp(0);
        player.setLevel(0);
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setHealth(player.getMaxHealth());
        player.updateInventory();
    }

    public void delete(int characterId) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement("DELETE FROM character_state WHERE character_id=?")) {
            ps.setInt(1, characterId); ps.executeUpdate();
        }
    }
}