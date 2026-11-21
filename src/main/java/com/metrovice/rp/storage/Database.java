package com.metrovice.rp.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private final JavaPlugin plugin;
    private Connection connection;

    public Database(JavaPlugin plugin) { this.plugin = plugin; }

    public void open() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new SQLException("Could not create plugin data folder");
        }
        File file = new File(plugin.getDataFolder(), "metrovice.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS characters (
                  character_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  owner_uuid TEXT NOT NULL,
                  character_name TEXT NOT NULL COLLATE NOCASE,
                  age INTEGER NOT NULL,
                  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE(owner_uuid, character_name)
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS active_characters (
                  owner_uuid TEXT PRIMARY KEY,
                  character_id INTEGER NOT NULL,
                  FOREIGN KEY(character_id) REFERENCES characters(character_id) ON DELETE CASCADE
                )
                """);
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