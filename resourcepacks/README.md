# Metro Vice RP Resource Packs

This directory contains the Java resource pack, the Bedrock/Geyser resource pack, and the Geyser custom-item mapping.

- `java/`: Java item models and textures.
- `bedrock/`: Bedrock `.mcpack` with matching item textures.
- `geyser/MetroViceRP.json`: Geyser v2 mapping definitions for the medical bag and police laptops.

The combined GitHub Actions bundle places the Bedrock pack under `plugins/Geyser-Spigot/packs/` and the mapping under `plugins/Geyser-Spigot/custom_mappings/`. Java pack delivery still requires a direct HTTPS URL and the generated SHA-1 in the plugin configuration.

A successful CI build verifies compilation and packaging; it does not replace live testing on Java and Bedrock clients.
