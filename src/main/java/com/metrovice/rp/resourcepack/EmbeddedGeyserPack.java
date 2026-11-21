package com.metrovice.rp.resourcepack;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Installs the Bedrock pack into MetroViceRP's own data folder.
 * The Geyser custom item mappings are copied before Geyser enables.
 */
public final class EmbeddedGeyserPack {
    private EmbeddedGeyserPack() {}

    public static Path install(JavaPlugin plugin) {
        Path plugins = plugin.getDataFolder().toPath().getParent();
        Path geyser = plugins.resolve("Geyser-Spigot");
        Path mappings = geyser.resolve("custom_mappings");
        Path ownPacks = plugin.getDataFolder().toPath().resolve("resourcepacks");
        Path bedrockPack = ownPacks.resolve("MetroViceRP-Bedrock.mcpack");

        try {
            Files.createDirectories(ownPacks);
            Files.createDirectories(mappings);
            copy(plugin, "embedded/MetroViceRP-Bedrock.mcpack", bedrockPack);
            copy(plugin, "embedded/MetroViceRP.json", mappings.resolve("MetroViceRP.json"));
            plugin.getLogger().info("MetroViceRP Bedrock pack installed in its own data folder: " + bedrockPack);
            plugin.getLogger().info("MetroViceRP Geyser custom item mappings installed.");
            return bedrockPack;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not install embedded Bedrock content: " + exception.getMessage());
            plugin.getLogger().severe("Check that the plugins directory is writable and the embedded files exist.");
            return bedrockPack;
        }
    }

    private static void copy(JavaPlugin plugin, String resource, Path destination) throws IOException {
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) throw new IOException("Missing embedded resource: " + resource);
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
