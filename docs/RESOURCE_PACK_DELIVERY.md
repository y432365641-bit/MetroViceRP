# MetroViceRP resource-pack delivery

Both edition-specific packs are packaged into the plugin JAR during the Maven build.

## Bedrock Edition

On plugin load, MetroViceRP copies the embedded Bedrock `.mcpack` and its Geyser custom-item mapping into Geyser's folders. This is automatic; server owners do not manually upload or copy the pack. Geyser remains responsible for the Bedrock resource-pack handshake.

## Java Edition

Java clients require an HTTP(S) download URL. MetroViceRP now serves the ZIP embedded in its JAR using a small built-in HTTP server.

1. Allocate one additional public TCP port in the game-host panel.
2. Set `resource-pack.java-server.port` to that allocated port.
3. Set `resource-pack.java-server.public-url` to the public address, for example `http://SERVER_IP:PORT`.
4. Restart the server. MetroViceRP logs the final ZIP URL and SHA-1.
5. Open that URL in a browser to verify that the ZIP can be downloaded, then test with a Java player.

No resource-pack file needs to be uploaded to an external file host. If the host cannot allocate an additional public TCP port, Java pack delivery from the plugin alone is not possible; the client must have a publicly reachable HTTP(S) URL.

Bedrock players are excluded from Java pack delivery and receive only the Bedrock pack through Geyser.
