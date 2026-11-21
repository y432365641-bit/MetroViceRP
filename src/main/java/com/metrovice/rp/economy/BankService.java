package com.metrovice.rp.economy;

import com.metrovice.rp.character.CharacterProfile;
import com.metrovice.rp.character.CharacterService;
import com.metrovice.rp.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class BankService {
    public record Balance(long cash, long bank) {}

    private final Database database;
    private final CharacterService characters;

    public BankService(Database database, CharacterService characters) {
        this.database = database; this.characters = characters;
    }

    public Balance balance(UUID owner) throws SQLException {
        CharacterProfile profile = requireActive(owner);
        return new Balance(profile.cash(), profile.bank());
    }

    public void deposit(UUID owner, long amount) throws SQLException {
        validateAmount(amount);
        int id = requireActive(owner).id();
        transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE characters SET cash=cash-?, bank=bank+? WHERE character_id=? AND cash>=?")) {
                ps.setLong(1, amount); ps.setLong(2, amount); ps.setInt(3, id); ps.setLong(4, amount);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("رصيدك النقدي غير كافٍ.");
            }
        });
    }

    public void withdraw(UUID owner, long amount) throws SQLException {
        validateAmount(amount);
        int id = requireActive(owner).id();
        transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE characters SET bank=bank-?, cash=cash+? WHERE character_id=? AND bank>=?")) {
                ps.setLong(1, amount); ps.setLong(2, amount); ps.setInt(3, id); ps.setLong(4, amount);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("رصيد البنك غير كافٍ.");
            }
        });
    }

    public void transfer(UUID sender, UUID receiver, long amount) throws SQLException {
        validateAmount(amount);
        if (sender.equals(receiver)) throw new IllegalArgumentException("لا يمكنك التحويل لنفس الشخصية.");
        int fromId = requireActive(sender).id();
        int toId = requireActive(receiver).id();
        transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE characters SET bank=bank-? WHERE character_id=? AND bank>=?")) {
                ps.setLong(1, amount); ps.setInt(2, fromId); ps.setLong(3, amount);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("رصيد البنك غير كافٍ.");
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE characters SET bank=bank+? WHERE character_id=?")) {
                ps.setLong(1, amount); ps.setInt(2, toId);
                if (ps.executeUpdate() != 1) throw new IllegalArgumentException("تعذر الوصول إلى حساب المستلم.");
            }
        });
    }

    private CharacterProfile requireActive(UUID owner) throws SQLException {
        return characters.active(owner).orElseThrow(() -> new IllegalArgumentException("فعّل شخصية أولًا عبر /character menu."));
    }

    private void validateAmount(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("المبلغ لازم يكون أكبر من صفر.");
        if (amount > 1_000_000_000_000L) throw new IllegalArgumentException("المبلغ أكبر من الحد المسموح.");
    }

    @FunctionalInterface private interface SqlWork { void run(Connection connection) throws Exception; }

    private void transaction(SqlWork work) throws SQLException {
        Connection connection = database.connection();
        connection.setAutoCommit(false);
        try {
            work.run(connection);
            connection.commit();
        } catch (Exception ex) {
            connection.rollback();
            if (ex instanceof SQLException sql) throw sql;
            if (ex instanceof IllegalArgumentException arg) throw arg;
            throw new SQLException("Bank transaction failed", ex);
        } finally {
            connection.setAutoCommit(true);
        }
    }
}