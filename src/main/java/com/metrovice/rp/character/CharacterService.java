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
        this.plugin = plugin; this.database = database;
    }

    public CharacterProfile create(UUID owner, String rawName, int age) throws SQLException {
        String name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        int minAge = plugin.getConfig().getInt("settings.min-character-age", 18);
        int maxAge = plugin.getConfig().getInt("settings.max-character-age", 80);
        int maxCharacters = plugin.getConfig().getInt("settings.max-characters-per-player", 3);
        int minLength = plugin.getConfig().getInt("settings.character-name-min-length", 3);
        int maxLength = plugin.getConfig().getInt("settings.character-name-max-length", 32);
        if (age < minAge || age > maxAge) throw new IllegalArgumentException("العمر يجب أن يكون بين " + minAge + " و" + maxAge + ".");
        if (name.length() < minLength || name.length() > maxLength || !validName(name))
            throw new IllegalArgumentException("الاسم يجب أن يكون بين " + minLength + " و" + maxLength + " أحرف، ويسمح بالحروف والمسافات والأرقام وشرطة الاسم.");
        if (list(owner).size() >= maxCharacters) throw new IllegalArgumentException("وصلت للحد الأقصى للشخصيات (" + maxCharacters + ").");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO characters(owner_uuid, character_name, age) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, owner.toString()); ps.setString(2, name); ps.setInt(3, age); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return find(owner, keys.getInt(1)).orElseThrow();
            }
        } catch (SQLException ex) {
            if (isConstraint(ex)) throw new IllegalArgumentException("لديك شخصية بهذا الاسم بالفعل.");
            throw ex;
        }
        throw new SQLException("Could not read new character id");
    }

    private boolean validName(String name) {
        return name.codePoints().allMatch(c -> Character.isLetterOrDigit(c) || Character.isWhitespace(c)
                || c == '-' || c == '_' || c == '\'');
    }

    public List<CharacterProfile> list(UUID owner) throws SQLException {
        List<CharacterProfile> result = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM characters WHERE owner_uuid=? ORDER BY character_id")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(map(rs)); }
        }
        return result;
    }

    public Optional<CharacterProfile> find(UUID owner, int id) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT * FROM characters WHERE owner_uuid=? AND character_id=?")) {
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
                SELECT c.* FROM active_characters a JOIN characters c ON c.character_id=a.character_id
                WHERE a.owner_uuid=?
                """)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    public void rename(UUID owner, int id, String newName) throws SQLException {
        String name = newName == null ? "" : newName.strip().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 32 || !validName(name))
            throw new IllegalArgumentException("الاسم غير صالح (3 إلى 32 حرفًا).");
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE characters SET character_name=? WHERE owner_uuid=? AND character_id=?")) {
            ps.setString(1, name); ps.setString(2, owner.toString()); ps.setInt(3, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("الشخصية غير موجودة بحسابك.");
        } catch (SQLException ex) {
            if (isConstraint(ex)) throw new IllegalArgumentException("لديك شخصية أخرى بهذا الاسم.");
            throw ex;
        }
    }

    public void delete(UUID owner, int id) throws SQLException {
        try (Connection connection = database.connection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM active_characters WHERE owner_uuid=? AND character_id=?")) {
                    ps.setString(1, owner.toString()); ps.setInt(2, id); ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM characters WHERE owner_uuid=? AND character_id=?")) {
                    ps.setString(1, owner.toString()); ps.setInt(2, id);
                    if (ps.executeUpdate() == 0) throw new IllegalArgumentException("الشخصية غير موجودة بحسابك.");
                }
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                if (ex instanceof SQLException sql) throw sql;
                if (ex instanceof IllegalArgumentException arg) throw arg;
                throw new SQLException(ex);
            } finally { connection.setAutoCommit(true); }
        }
    }

    private boolean isConstraint(SQLException ex) {
        return ex.getMessage() != null && (ex.getMessage().toLowerCase().contains("unique")
                || ex.getMessage().toLowerCase().contains("constraint"));
    }

    private CharacterProfile map(ResultSet rs) throws SQLException {
        return new CharacterProfile(rs.getInt("character_id"), rs.getString("owner_uuid"),
                rs.getString("character_name"), rs.getInt("age"), rs.getString("job"),
                rs.getString("rank_name"), rs.getLong("cash"), rs.getLong("bank"), rs.getString("created_at"));
    }
}