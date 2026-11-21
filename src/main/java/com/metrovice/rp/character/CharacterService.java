package com.metrovice.rp.character;

import com.metrovice.rp.storage.Database;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CharacterService {
    private final JavaPlugin plugin;
    private final Database database;

    public CharacterService(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public CharacterProfile create(UUID owner, String name, int age) throws SQLException {
        int minAge = plugin.getConfig().getInt("settings.min-character-age", 18);
        int maxAge = plugin.getConfig().getInt("settings.max-character-age", 80);
        int maxCharacters = plugin.getConfig().getInt("settings.max-characters-per-player", 3);
        int minLength = plugin.getConfig().getInt("settings.character-name-min-length", 3);
        int maxLength = plugin.getConfig().getInt("settings.character-name-max-length", 16);
        if (age < minAge || age > maxAge) throw new IllegalArgumentException("العمر يجب أن يكون بين " + minAge + " و" + maxAge + ".");
        if (name.length() < minLength || name.length() > maxLength || !name.matches("[A-Za-z0-9_]+"))
            throw new IllegalArgumentException("الاسم يجب أن يكون من " + minLength + " إلى " + maxLength + " أحرف إنجليزية أو أرقام أو _.");
        if (list(owner).size() >= maxCharacters) throw new IllegalArgumentException("وصلت للحد الأقصى للشخصيات (" + maxCharacters + ").");

        String sql = "INSERT INTO characters(owner_uuid, character_name, age) VALUES(?,?,?)";
        try (PreparedStatement ps = database.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, owner.toString()); ps.setString(2, name); ps.setInt(3, age); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return find(owner, keys.getInt(1)).orElseThrow();
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unique"))
                throw new IllegalArgumentException("لديك شخصية بهذا الاسم بالفعل.");
            throw ex;
        }
        throw new SQLException("Could not read new character id");
    }

    public List<CharacterProfile> list(UUID owner) throws SQLException {
        List<CharacterProfile> result = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT character_id, owner_uuid, character_name, age, created_at FROM characters WHERE owner_uuid=? ORDER BY character_id")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public Optional<CharacterProfile> find(UUID owner, int id) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT character_id, owner_uuid, character_name, age, created_at FROM characters WHERE owner_uuid=? AND character_id=?")) {
            ps.setString(1, owner.toString()); ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    public void select(UUID owner, int id) throws SQLException {
        if (find(owner, id).isEmpty()) throw new IllegalArgumentException("هذه الشخصية غير موجودة في حسابك.");
        try (PreparedStatement ps = database.connection().prepareStatement("""
                INSERT INTO active_characters(owner_uuid, character_id) VALUES(?,?)
                ON CONFLICT(owner_uuid) DO UPDATE SET character_id=excluded.character_id
                """)) {
            ps.setString(1, owner.toString()); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public Optional<CharacterProfile> active(UUID owner) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement("""
                SELECT c.character_id, c.owner_uuid, c.character_name, c.age, c.created_at
                FROM active_characters a JOIN characters c ON c.character_id=a.character_id
                WHERE a.owner_uuid=?
                """)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    private CharacterProfile map(ResultSet rs) throws SQLException {
        return new CharacterProfile(rs.getInt("character_id"), rs.getString("owner_uuid"),
                rs.getString("character_name"), rs.getInt("age"), rs.getString("created_at"));
    }
}