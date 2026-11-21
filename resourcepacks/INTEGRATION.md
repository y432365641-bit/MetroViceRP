# Metro Vice RP Resource Pack Integration

## Bundled packs
The MetroViceRP plugin JAR embeds both resource packs. On startup it extracts them into:
- `plugins/MetroViceRP/resourcepacks/MetroViceRP-Bedrock.mcpack`
- `plugins/MetroViceRP/resourcepacks/MetroViceRP-Java.zip`
- `plugins/MetroViceRP/resourcepacks/MetroViceRP-Java-SHA1.txt`

## Bedrock Edition
MetroViceRP owns the Bedrock pack file and registers it for each connecting Bedrock session through the supported Geyser API. Geyser remains the Bedrock connection/protocol bridge; no separate resource-pack plugin and no manual copy into `Geyser-Spigot/packs/` are required. The matching custom-item mapping is installed in `plugins/Geyser-Spigot/custom_mappings/MetroViceRP.json` before Geyser starts.

## Java Edition
The Java pack is extracted and its SHA-1 is generated automatically. Java clients still need a URL reachable from their devices because the Java resource-pack handshake downloads the ZIP from a URL. Set `resource-pack.url` to a direct HTTP(S) URL for the extracted ZIP and set `resource-pack.sha1` to the value in `MetroViceRP-Java-SHA1.txt`; then enable `resource-pack.enabled`. This hosting requirement is part of the Java client protocol and is separate from the Bedrock pack delivery.

## Testing
After installing the JAR and restarting:
- Check the console for the extracted pack paths and Java SHA-1.
- Check for the Bedrock pack bridge ready message.
- Join from Bedrock and confirm that the pack is added to the connecting session.
- Join from Java after configuring the direct resource-pack URL and confirm the download prompt.

The phone, police MDT, rank-management laptop, EMS medical bag, and radio use the shared Metro Vice RP item models and mappings.
