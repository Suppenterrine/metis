package dev.suppenterrine.metis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.suppenterrine.metis.perception.PerceptionSnapshot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Client-side entrypoint. Runs a tiny localhost HTTP server that serves the
 * player's live, embodied perception over a small family of read-only endpoints.
 *
 * <p>The data is captured once per client tick into an immutable
 * {@link PerceptionSnapshot} held in a {@code volatile} field; the HTTP handler
 * threads only ever read that snapshot, so they never touch the game thread.
 *
 * <p>What is exposed — and the perception class each endpoint carries — follows
 * the Law of Embodied Perception ({@code docs/PERCEPTION_RULES.md}). Metis
 * exposes raw, tagged data and applies no gating; private internal state is
 * simply never captured. The listen port is read from
 * {@link dev.suppenterrine.metis.config.MetisConfig}.
 */
public class MetisClient implements ClientModInitializer {
	// serializeNulls so every documented key is always present (null where a slot
	// is empty or a value is not applicable), rather than silently disappearing.
	private static final Gson GSON = new GsonBuilder().serializeNulls().create();

	private HttpServer server;
	private boolean serverStarted = false;

	/** Latest perception snapshot, or {@code null} when the player is not in a world. */
	private volatile PerceptionSnapshot snapshot;

	@Override
	public void onInitializeClient() {
		if (Metis.getConfig().enabled) {
			startServer();
		}

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean configEnabled = Metis.getConfig().enabled;

			if (configEnabled && !serverStarted) {
				startServer();
			} else if (!configEnabled && serverStarted) {
				stopServer();
			}

			// Capture the embodied snapshot on the game thread while running.
			snapshot = serverStarted ? PerceptionSnapshot.capture(client) : null;
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

			// Legacy contract (kept byte-for-byte stable): position + world context.
			server.createContext("/api/coords", this::handleCoordsRequest);

			// Embodied perception, each tagged with its rule-system class.
			server.createContext("/api/movement", jsonEndpoint(PerceptionSnapshot::movementPayload));
			server.createContext("/api/look", jsonEndpoint(PerceptionSnapshot::lookPayload));
			server.createContext("/api/equipment", jsonEndpoint(PerceptionSnapshot::equipmentPayload));
			server.createContext("/api/environment", jsonEndpoint(PerceptionSnapshot::environmentPayload));
			server.createContext("/api/players", jsonEndpoint(PerceptionSnapshot::playersPayload));

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
		if (guard(exchange)) return;

		PerceptionSnapshot snap = this.snapshot;
		if (snap == null) {
			sendResponse(exchange, 404, "{\"error\": \"Player not in world\"}");
			return;
		}

		// US locale → dots, not commas, in the JSON numbers. Format kept identical
		// to the original /api/coords contract that MCDS consumes for radio distance.
		String responseText = String.format(Locale.US,
				"{\"x\": %.2f, \"y\": %.2f, \"z\": %.2f, \"yaw\": %.2f, \"pitch\": %.2f, \"world\": \"%s\", \"biome\": \"%s\", \"uuid\": \"%s\", \"username\": \"%s\"}",
				snap.x, snap.y, snap.z, snap.yaw, snap.pitch, snap.world, snap.biome, snap.uuid, snap.username
		);
		sendResponse(exchange, 200, responseText);
	}

	/**
	 * Builds a handler that serializes one snapshot payload to JSON. Each shares
	 * the same loopback guard, CORS handling and "player not in world" semantics.
	 */
	private com.sun.net.httpserver.HttpHandler jsonEndpoint(Function<PerceptionSnapshot, Map<String, Object>> payload) {
		return exchange -> {
			if (guard(exchange)) return;

			PerceptionSnapshot snap = this.snapshot;
			if (snap == null) {
				sendResponse(exchange, 404, "{\"error\": \"Player not in world\"}");
				return;
			}
			sendResponse(exchange, 200, GSON.toJson(payload.apply(snap)));
		};
	}

	/**
	 * Handles the CORS preflight and the loopback-only access guard.
	 *
	 * @return {@code true} if the request was fully handled here (caller must return).
	 */
	private boolean guard(HttpExchange exchange) throws IOException {
		if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
			sendResponse(exchange, 204, null);
			return true;
		}

		InetAddress remote = exchange.getRemoteAddress().getAddress();
		if (remote == null || !remote.isLoopbackAddress()) {
			sendResponse(exchange, 403, "{\"error\": \"Access denied\"}");
			return true;
		}
		return false;
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
