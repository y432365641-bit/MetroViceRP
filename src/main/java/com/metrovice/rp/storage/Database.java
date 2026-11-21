package com.metrovice.rp.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;

public final class Database {
    private final JavaPlugin plugin;
    private Connection connection;

    public Database(JavaPlugin plugin) { this.plugin = plugin; }

    public void open() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs())
            throw new SQLException("Could not create plugin data folder");
        File file = new File(plugin.getDataFolder(), "metrovice.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS characters (
                  character_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  owner_uuid TEXT NOT NULL,
                  character_name TEXT NOT NULL COLLATE NOCASE,
                  age INTEGER NOT NULL,
                  job TEXT NOT NULL DEFAULT 'Citizen',
                  rank_name TEXT NOT NULL DEFAULT 'Citizen',
                  cash INTEGER NOT NULL DEFAULT 0 CHECK(cash >= 0),
                  bank INTEGER NOT NULL DEFAULT 0 CHECK(bank >= 0),
                  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE(owner_uuid, character_name)
                )
                """);
            ensureColumn("characters", "job", "TEXT NOT NULL DEFAULT 'Citizen'");
            ensureColumn("characters", "rank_name", "TEXT NOT NULL DEFAULT 'Citizen'");
            ensureColumn("characters", "cash", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn("characters", "bank", "INTEGER NOT NULL DEFAULT 0");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS active_characters (
                  owner_uuid TEXT PRIMARY KEY,
                  character_id INTEGER NOT NULL,
                  FOREIGN KEY(character_id) REFERENCES characters(character_id) ON DELETE CASCADE
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS character_state (
                  character_id INTEGER PRIMARY KEY,
                  state_data BLOB NOT NULL,
                  state_version INTEGER NOT NULL DEFAULT 2,
                  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(character_id) REFERENCES characters(character_id) ON DELETE CASCADE
                )
                """);
            ensureColumn("character_state", "state_version", "INTEGER NOT NULL DEFAULT 1");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS department_memberships (
                  membership_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  character_id INTEGER NOT NULL,
                  department TEXT NOT NULL,
                  rank_name TEXT NOT NULL,
                  on_duty INTEGER NOT NULL DEFAULT 0,
                  duty_started_at INTEGER,
                  total_duty_seconds INTEGER NOT NULL DEFAULT 0,
                  UNIQUE(character_id, department),
                  FOREIGN KEY(character_id) REFERENCES characters(character_id) ON DELETE CASCADE
                )
                """);
        }
    }

    private void ensureColumn(String table, String column, String definition) throws SQLException {
        boolean exists = false;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("name"))) exists = true;
        }
        if (!exists) try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    public Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) throw new SQLException("Database is not open");
        return connection;
    }

    public void close() {
        if (connection == null) return;
        try { connection.close(); }
        catch (SQLException exception) { plugin.getLogger().warning("Database close failed: " + exception.getMessage()); }
    }
}