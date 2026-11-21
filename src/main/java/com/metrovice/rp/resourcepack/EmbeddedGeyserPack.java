package com.metrovice.rp.resourcepack;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Copies the Bedrock pack and Geyser item mappings embedded in this JAR
 * before Geyser starts loading its local packs and mappings.
 */
public final class EmbeddedGeyserPack {
    private EmbeddedGeyserPack() {}

    public static void install(JavaPlugin plugin) {
        Path plugins = plugin.getDataFolder().toPath().getParent();
        Path geyser = plugins.resolve("Geyser-Spigot");
        Path packs = geyser.resolve("packs");
        Path mappings = geyser.resolve("custom_mappings");

        try {
            Files.createDirectories(packs);
            Files.createDirectories(mappings);
            copy(plugin, "embedded/MetroViceRP-Bedrock.mcpack",
                    packs.resolve("MetroViceRP-Bedrock.mcpack"));
            copy(plugin, "embedded/MetroViceRP.json",
                    mappings.resolve("MetroViceRP.json"));
            plugin.getLogger().info("Embedded Bedrock pack and Geyser mappings installed.");
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not install embedded Geyser content: " + exception.getMessage());
            plugin.getLogger().severe("Check that the server plugins directory is writable.");
        }
    }

    private static void copy(JavaPlugin plugin, String resource, Path destination) throws IOException {
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) {
                throw new IOException("Missing embedded resource: " + resource);
            }
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
