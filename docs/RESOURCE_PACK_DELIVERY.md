# MetroViceRP resource-pack delivery

Both edition-specific resource packs are embedded into `MetroViceRP.jar` during the Maven build.

## Bedrock Edition

MetroViceRP extracts the embedded Bedrock `.mcpack` into its own plugin data directory:

`plugins/MetroViceRP/resourcepacks/MetroViceRP-Bedrock.mcpack`

The plugin-owned `BedrockResourcePackManager` loads that file with Geyser's `PackCodec.path(...)` and registers it for each connecting Bedrock session through `SessionLoadResourcePacksEvent`.

The Bedrock pack itself is **not copied to** `plugins/Geyser-Spigot/packs/`, and it does not need an external file host or a manually uploaded file. Geyser remains the Bedrock protocol transport because it owns the Bedrock connection. MetroViceRP owns the embedded pack, extraction, validation path, registration, and delivery logging.

The separate `MetroViceRP.json` custom-item mapping may be installed into Geyser's `custom_mappings` directory. This is a mapping definition, not the resource-pack archive.

## Java Edition

Java clients require a publicly reachable HTTP(S) download URL. MetroViceRP can serve the ZIP embedded in its JAR using its built-in HTTP server.

1. Allocate one additional public TCP port in the game-host panel.
2. Set `resource-pack.java-server.port` to that allocated port.
3. Set `resource-pack.java-server.public-url` to the public address, for example `http://SERVER_IP:PORT`.
4. Restart the server. MetroViceRP logs the final ZIP URL and SHA-1.
5. Open that URL in a browser to verify that the ZIP can be downloaded, then test with a Java player.

No resource-pack file needs to be uploaded to an external file host. If the host cannot allocate an additional public TCP port, Java pack delivery from the plugin alone is not possible; the client must have a publicly reachable HTTP(S) URL.

Bedrock players must be excluded from Java pack delivery and receive only the Bedrock pack through the per-session Geyser API registration described above.

## Validation status

The source integration is present, but it is not considered verified until the project builds successfully and a real Bedrock client downloads/accepts the pack. A log that the pack was registered only confirms registration, not client acceptance or successful application.
