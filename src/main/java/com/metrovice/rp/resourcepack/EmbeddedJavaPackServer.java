package com.metrovice.rp.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serves the Java resource pack that is embedded inside MetroViceRP.jar.
 * The only host requirement is an additional publicly reachable TCP port;
 * the pack itself is never uploaded manually to a file host.
 */
public final class EmbeddedJavaPackServer {
    private static final String PACK_NAME = "MetroViceRP-Java.zip";
    private static final String PACK_PATH = "/" + PACK_NAME;

    private final JavaPlugin plugin;
    private HttpServer server;
    private ExecutorService executor;
    private String downloadUrl = "";
    private String sha1 = "";

    public EmbeddedJavaPackServer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)
                || !plugin.getConfig().getBoolean("resource-pack.java-server.enabled", true)) {
            plugin.getLogger().info("Java resource-pack HTTP server is disabled in config.yml.");
            return;
        }

        Path pack = plugin.getDataFolder().toPath().resolve("resourcepacks").resolve(PACK_NAME);
        if (!Files.isRegularFile(pack)) {
            plugin.getLogger().severe("Embedded Java resource pack is missing: " + pack.toAbsolutePath());
            plugin.getLogger().severe("The JAR must contain embedded/MetroViceRP-Java.zip.");
            return;
        }

        String bindAddress = plugin.getConfig().getString("resource-pack.java-server.bind-address", "0.0.0.0");
        int port = plugin.getConfig().getInt("resource-pack.java-server.port", 8085);
        String publicBase = plugin.getConfig().getString("resource-pack.java-server.public-url", "").trim();

        if (port < 1 || port > 65535) {
            plugin.getLogger().severe("Invalid resource-pack.java-server.port: " + port);
            return;
        }

        try {
            sha1 = sha1(pack);
            server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            server.createContext(PACK_PATH, exchange -> servePack(exchange, pack));
            executor = Executors.newFixedThreadPool(2, task -> {
                Thread thread = new Thread(task, "MetroViceRP-ResourcePack-HTTP");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.start();

            if (publicBase.isBlank()) {
                plugin.getLogger().warning("Java pack server is listening on " + bindAddress + ":" + port
                        + ", but public-url is empty. Set resource-pack.java-server.public-url to the public "
                        + "address of this allocated port before Java players can download the pack.");
            } else if (!publicBase.startsWith("http://") && !publicBase.startsWith("https://")) {
                plugin.getLogger().warning("resource-pack.java-server.public-url must start with http:// or https://. "
                        + "Java pack delivery is disabled until it is corrected.");
            } else {
                while (publicBase.endsWith("/")) {
                    publicBase = publicBase.substring(0, publicBase.length() - 1);
                }
                downloadUrl = publicBase + PACK_PATH;
                plugin.getLogger().info("Embedded Java resource pack server is ready: " + downloadUrl);
                plugin.getLogger().info("Java resource pack SHA-1: " + sha1);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not start the embedded Java resource-pack server: "
                    + exception.getMessage());
            plugin.getLogger().severe("Check that this TCP port is allocated to the server and is not already in use.");
            stop();
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not prepare the embedded Java resource pack: "
                    + exception.getMessage());
            stop();
        }
    }

    private void servePack(HttpExchange exchange, Path pack) throws IOException {
        String method = exchange.getRequestMethod();
        if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("HEAD")) {
            exchange.getResponseHeaders().set("Allow", "GET, HEAD");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        if (!PACK_PATH.equals(exchange.getRequestURI().getPath()) || !Files.isRegularFile(pack)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        long size = Files.size(pack);
        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + PACK_NAME + "\"");
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");

        if (method.equalsIgnoreCase("HEAD")) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, size);
        try (OutputStream output = exchange.getResponseBody()) {
            Files.copy(pack, output);
        } finally {
            exchange.close();
        }
    }

    private static String sha1(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public String downloadUrl() {
        return downloadUrl;
    }

    public String sha1() {
        return sha1;
    }

    public boolean isReady() {
        return server != null && !downloadUrl.isBlank() && !sha1.isBlank();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
