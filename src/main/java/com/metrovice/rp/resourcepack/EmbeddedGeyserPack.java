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
 * Extracts the Bedrock and Java packs embedded in MetroViceRP.jar into the
 * plugin's own data folder. The Bedrock pack is NOT copied into Geyser's
 * packs directory; BedrockResourcePackManager registers it through Geyser API.
 *
 * The custom-item mapping is separate from the resource-pack archive.
 */
public final class EmbeddedGeyserPack {
    private EmbeddedGeyserPack() {}

    public static Path install(JavaPlugin plugin) {
        Path ownPacks = plugin.getDataFolder().toPath().resolve("resourcepacks");
        Path bedrockPack = ownPacks.resolve("MetroViceRP-Bedrock.mcpack");
        Path javaPack = ownPacks.resolve("MetroViceRP-Java.zip");
        Path geyserMappings = plugin.getDataFolder().toPath().getParent()
                .resolve("Geyser-Spigot").resolve("custom_mappings");

        try {
            Files.createDirectories(ownPacks);
            copy(plugin, "embedded/MetroViceRP-Bedrock.mcpack", bedrockPack);
            copy(plugin, "embedded/MetroViceRP-Java.zip", javaPack);

            // This is a mapping definition, not a resource-pack file.
            try {
                Files.createDirectories(geyserMappings);
                copy(plugin, "embedded/MetroViceRP.json",
                        geyserMappings.resolve("MetroViceRP.json"));
                plugin.getLogger().info("MetroViceRP Bedrock item mapping installed.");
            } catch (IOException mappingError) {
                plugin.getLogger().warning("Could not install optional Geyser item mapping: "
                        + mappingError.getMessage());
            }

            String javaSha1 = sha1(javaPack);
            Files.writeString(ownPacks.resolve("MetroViceRP-Java-SHA1.txt"), javaSha1);
            plugin.getLogger().info("Embedded Bedrock pack extracted to plugin-owned storage: "
                    + bedrockPack.toAbsolutePath());
            plugin.getLogger().info("Embedded Java pack extracted to plugin-owned storage: "
                    + javaPack.toAbsolutePath());
            plugin.getLogger().info("MetroViceRP Java pack SHA-1: " + javaSha1);
            plugin.getLogger().info("Bedrock pack will be registered through Geyser API; "
                    + "it will not be copied to Geyser-Spigot/packs.");
            return bedrockPack;
        } catch (IOException | NoSuchAlgorithmException exception) {
            plugin.getLogger().severe("Could not extract embedded resource packs: " + exception.getMessage());
            plugin.getLogger().severe("Verify that the JAR contains both embedded resource-pack archives.");
            return null;
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
