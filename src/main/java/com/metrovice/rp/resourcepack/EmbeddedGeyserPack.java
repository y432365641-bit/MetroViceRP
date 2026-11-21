package com.metrovice.rp.resourcepack;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Installs the bundled resource packs into MetroViceRP's own data folder.
 * Geyser mappings are copied before Geyser initializes; the Bedrock pack
 * itself is registered through the supported Geyser API by the pack manager.
 */
public final class EmbeddedGeyserPack {
    private EmbeddedGeyserPack() {}

    public static Path install(JavaPlugin plugin) {
        Path plugins = plugin.getDataFolder().toPath().getParent();
        Path geyser = plugins.resolve("Geyser-Spigot");
        Path mappings = geyser.resolve("custom_mappings");
        Path ownPacks = plugin.getDataFolder().toPath().resolve("resourcepacks");
        Path bedrockPack = ownPacks.resolve("MetroViceRP-Bedrock.mcpack");
        Path javaPack = ownPacks.resolve("MetroViceRP-Java.zip");

        try {
            Files.createDirectories(ownPacks);
            Files.createDirectories(mappings);
            copy(plugin, "embedded/MetroViceRP-Bedrock.mcpack", bedrockPack);
            copy(plugin, "embedded/MetroViceRP-Java.zip", javaPack);
            copy(plugin, "embedded/MetroViceRP.json", mappings.resolve("MetroViceRP.json"));

            String javaSha1 = sha1(javaPack);
            Files.writeString(ownPacks.resolve("MetroViceRP-Java-SHA1.txt"), javaSha1);
            plugin.getLogger().info("MetroViceRP Bedrock pack installed: " + bedrockPack.toAbsolutePath());
            plugin.getLogger().info("MetroViceRP Java pack installed: " + javaPack.toAbsolutePath());
            plugin.getLogger().info("MetroViceRP Java pack SHA-1: " + javaSha1);
            plugin.getLogger().info("MetroViceRP Geyser custom item mappings installed.");
            return bedrockPack;
        } catch (IOException | NoSuchAlgorithmException exception) {
            plugin.getLogger().severe("Could not install bundled resource packs: " + exception.getMessage());
            plugin.getLogger().severe("Check that the plugins directory is writable and the embedded files exist.");
            return bedrockPack;
        }
    }

    private static String sha1(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void copy(JavaPlugin plugin, String resource, Path destination) throws IOException {
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) throw new IOException("Missing embedded resource: " + resource);
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}