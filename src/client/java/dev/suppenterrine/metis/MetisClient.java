package dev.suppenterrine.metis;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Client-side entrypoint. Runs a tiny localhost HTTP server that serves the
 * player's live coordinates and world context at {@code GET /api/coords}.
 * The listen port is read from {@link dev.suppenterrine.metis.config.MetisConfig}.
 */
public class MetisClient implements ClientModInitializer {
	private HttpServer server;
	private boolean serverStarted = false;

	@Override
	public void onInitializeClient() {
		if (Metis.getConfig().enabled) {
			startServer();
		}

		// Re-evaluate the enabled flag each tick so toggling it in the config file
		// (and reloading) starts/stops the server without a full restart.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean configEnabled = Metis.getConfig().enabled;

			if (configEnabled && !serverStarted) {
				startServer();
			} else if (!configEnabled && serverStarted) {
				stopServer();
			}
		});

		Metis.LOGGER.info("Registered config monitor");
	}

	private void startServer() {
		if (serverStarted) return;

		int port = Metis.getConfig().port;

		try {
			Metis.LOGGER.info("Starting Metis HTTP server on port {}", port);
			// Bind to loopback only — the API must never be reachable off-machine.
			server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
			server.createContext("/api/coords", this::handleCoordsRequest);
			server.setExecutor(Executors.newSingleThreadExecutor());
			server.start();
			serverStarted = true;
			Metis.LOGGER.info("Metis HTTP server started successfully");
		} catch (IOException e) {
			Metis.LOGGER.error("Failed to start Metis HTTP server on port {}", port, e);
		}
	}

	private void stopServer() {
		if (server != null) {
			Metis.LOGGER.info("Stopping Metis HTTP server");

			// Stop on a separate daemon thread so we never block the client tick.
			final HttpServer serverToStop = server;
			Thread stopThread = new Thread(() -> {
				serverToStop.stop(0);
				Metis.LOGGER.info("Metis HTTP server stopped successfully");
			});
			stopThread.setDaemon(true);
			stopThread.start();

			server = null;
			serverStarted = false;
		}
	}

	private void handleCoordsRequest(HttpExchange exchange) throws IOException {
		// CORS preflight
		if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
			sendResponse(exchange, 204, null);
			return;
		}

		// Loopback-only access guard
		InetAddress remote = exchange.getRemoteAddress().getAddress();
		if (remote == null || !remote.isLoopbackAddress()) {
			sendResponse(exchange, 403, "{\"error\": \"Access denied\"}");
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;

		if (player == null) {
			sendResponse(exchange, 404, "{\"error\": \"Player not in world\"}");
			return;
		}

		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		float yaw = player.getYaw();
		float pitch = player.getPitch();
		String world = player.getWorld().getRegistryKey().getValue().toString();

		RegistryEntry<Biome> biomeEntry = player.getWorld().getBiome(player.getBlockPos());
		String biome = biomeEntry.getKey().map(key -> key.getValue().toString()).orElse("unknown");

		String uuid = player.getUuid().toString();
		String username = player.getName().getString();

		// US locale → dots, not commas, in the JSON numbers.
		String responseText = String.format(Locale.US,
				"{\"x\": %.2f, \"y\": %.2f, \"z\": %.2f, \"yaw\": %.2f, \"pitch\": %.2f, \"world\": \"%s\", \"biome\": \"%s\", \"uuid\": \"%s\", \"username\": \"%s\"}",
				x, y, z, yaw, pitch, world, biome, uuid, username
		);
		sendResponse(exchange, 200, responseText);
	}

	private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
		exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
		exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

		if (response != null) {
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(statusCode, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		} else {
			exchange.sendResponseHeaders(statusCode, -1);
		}
	}
}
