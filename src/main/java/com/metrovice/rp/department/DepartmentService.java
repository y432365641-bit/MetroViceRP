package com.metrovice.rp.department;

import com.metrovice.rp.character.CharacterProfile;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.storage.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class DepartmentService {
    public record Membership(String department, String rank, boolean onDuty, long totalDutySeconds, Long dutyStartedAt) {}
    public record DutyMember(String characterName, String rank, String department, long dutySeconds) {}

    private final Database database;
    private final CharacterService characters;

    public DepartmentService(Database database, CharacterService characters) {
        this.database = database; this.characters = characters;
    }

    public String normalizeDepartment(String raw) {
        String value = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        if (!value.equals("POLICE") && !value.equals("EMS"))
            throw new IllegalArgumentException("القسم يجب أن يكون police أو ems.");
        return value;
    }

    public void hire(UUID owner, String department, String rank) throws SQLException {
        String dept = normalizeDepartment(department);
        String cleanRank = cleanRank(rank);
        int characterId = activeCharacter(owner).id();
        try (PreparedStatement ps = database.connection().prepareStatement("""
                INSERT INTO department_memberships(character_id, department, rank_name)
                VALUES(?,?,?)
                ON CONFLICT(character_id, department) DO UPDATE SET rank_name=excluded.rank_name
                """)) {
            ps.setInt(1, characterId); ps.setString(2, dept); ps.setString(3, cleanRank); ps.executeUpdate();
        }
        try (PreparedStatement ps = database.connection().prepareStatement(
                "UPDATE characters SET job=?, rank_name=? WHERE character_id=?")) {
            ps.setString(1, dept.equals("POLICE") ? "الشرطة" : "الإسعاف");
            ps.setString(2, cleanRank); ps.setInt(3, characterId); ps.executeUpdate();
        }
    }

    public void fire(UUID owner, String department) throws SQLException {
        String dept = normalizeDepartment(department);
        int characterId = activeCharacter(owner).id();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM department_memberships WHERE character_id=? AND department=?")) {
            ps.setInt(1, characterId); ps.setString(2, dept);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("الشخصية ليست موظفة في هذا القسم.");
        }
        String remainingDepartment = null;
        String remainingRank = null;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT department, rank_name FROM department_memberships WHERE character_id=? LIMIT 1")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    remainingDepartment = rs.getString("department");
                    remainingRank = rs.getString("rank_name");
                }
            }
        }
        if (remainingDepartment == null) {
            try (PreparedStatement update = database.connection().prepareStatement(
                    "UPDATE characters SET job='مواطن', rank_name='مواطن' WHERE character_id=?")) {
                update.setInt(1, characterId); update.executeUpdate();
            }
        } else {
            try (PreparedStatement update = database.connection().prepareStatement(
                    "UPDATE characters SET job=?, rank_name=? WHERE character_id=?")) {
                update.setString(1, remainingDepartment.equals("POLICE") ? "الشرطة" : "الإسعاف");
                update.setString(2, remainingRank);
                update.setInt(3, characterId);
                update.executeUpdate();
            }
        }
    }

    public List<Membership> memberships(UUID owner) throws SQLException {
        int characterId = activeCharacter(owner).id();
        List<Membership> result = new ArrayList<>();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT department, rank_name, on_duty, total_duty_seconds, duty_started_at FROM department_memberships WHERE character_id=? ORDER BY department")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long started = rs.getLong("duty_started_at");
                    Long start = rs.wasNull() ? null : started;
                    result.add(new Membership(rs.getString("department"), rs.getString("rank_name"),
                            rs.getInt("on_duty") == 1, rs.getLong("total_duty_seconds"), start));
                }
            }
        }
        return result;
    }

    public Membership toggleDuty(UUID owner, String department) throws SQLException {
        String dept = normalizeDepartment(department);
        int characterId = activeCharacter(owner).id();
        Membership current = membership(characterId, dept)
                .orElseThrow(() -> new IllegalArgumentException("شخصيتك ليست مسجلة في هذا القسم."));
        long now = System.currentTimeMillis() / 1000L;
        if (current.onDuty()) {
            long elapsed = current.dutyStartedAt() == null ? 0 : Math.max(0, now - current.dutyStartedAt());
            try (PreparedStatement ps = database.connection().prepareStatement("""
                    UPDATE department_memberships
                    SET on_duty=0, duty_started_at=NULL, total_duty_seconds=total_duty_seconds+?
                    WHERE character_id=? AND department=?
                    """)) {
                ps.setLong(1, elapsed); ps.setInt(2, characterId); ps.setString(3, dept); ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = database.connection().prepareStatement("""
                    UPDATE department_memberships SET on_duty=1, duty_started_at=?
                    WHERE character_id=? AND department=?
                    """)) {
                ps.setLong(1, now); ps.setInt(2, characterId); ps.setString(3, dept); ps.executeUpdate();
            }
        }
        return membership(characterId, dept).orElseThrow();
    }

    public void endAllDuty(UUID owner) throws SQLException {
        CharacterProfile active = characters.active(owner).orElse(null);
        if (active == null) return;
        int characterId = active.id();
        long now = System.currentTimeMillis() / 1000L;
        try (PreparedStatement ps = database.connection().prepareStatement("""
                UPDATE department_memberships
                SET total_duty_seconds=total_duty_seconds+
                    CASE WHEN duty_started_at IS NULL THEN 0 ELSE MAX(0, ? - duty_started_at) END,
                    on_duty=0, duty_started_at=NULL
                WHERE character_id=? AND on_duty=1
                """)) {
            ps.setLong(1, now); ps.setInt(2, characterId); ps.executeUpdate();
        }
    }

    public boolean isOnDuty(UUID owner, String department) throws SQLException {
        String dept = normalizeDepartment(department);
        int characterId = activeCharacter(owner).id();
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT on_duty FROM department_memberships WHERE character_id=? AND department=?")) {
            ps.setInt(1, characterId); ps.setString(2, dept);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }

    public List<DutyMember> onDuty(String department) throws SQLException {
        String dept = normalizeDepartment(department);
        List<DutyMember> result = new ArrayList<>();
        long now = System.currentTimeMillis() / 1000L;
        try (PreparedStatement ps = database.connection().prepareStatement("""
                SELECT c.character_name, d.rank_name, d.department, d.total_duty_seconds, d.duty_started_at
                FROM department_memberships d
                JOIN characters c ON c.character_id=d.character_id
                WHERE d.department=? AND d.on_duty=1
                ORDER BY d.rank_name, c.character_name
                """)) {
            ps.setString(1, dept);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long started = rs.getLong("duty_started_at");
                    long total = rs.getLong("total_duty_seconds") + Math.max(0, now - started);
                    result.add(new DutyMember(rs.getString("character_name"), rs.getString("rank_name"), dept, total));
                }
            }
        }
        return result;
    }

    private Optional<Membership> membership(int characterId, String dept) throws SQLException {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "SELECT department, rank_name, on_duty, total_duty_seconds, duty_started_at FROM department_memberships WHERE character_id=? AND department=?")) {
            ps.setInt(1, characterId); ps.setString(2, dept);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                long started = rs.getLong("duty_started_at");
                Long start = rs.wasNull() ? null : started;
                return Optional.of(new Membership(rs.getString("department"), rs.getString("rank_name"),
                        rs.getInt("on_duty") == 1, rs.getLong("total_duty_seconds"), start));
            }
        }
    }

    private CharacterProfile activeCharacter(UUID owner) throws SQLException {
        return characters.active(owner).orElseThrow(() -> new IllegalArgumentException("فعّل شخصية أولًا عبر /character menu."));
    }

    private String cleanRank(String rank) {
        String value = rank == null ? "" : rank.strip().replaceAll("\\s+", " ");
        if (value.isEmpty() || value.length() > 32 || value.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("اسم الرتبة غير صالح.");
        return value;
    }
}