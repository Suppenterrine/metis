package dev.suppenterrine.metis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.suppenterrine.metis.Metis;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lightweight, file-backed configuration. No ModMenu, no Cloth Config —
 * a plain JSON file at {@code .minecraft/config/metis.json}, freely editable.
 *
 * <pre>{ "enabled": true, "port": 25566 }</pre>
 */
public class MetisConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "metis.json";

	/** Default API port. 25566 avoids colliding with the usual MC server port (25565). */
	public static final int DEFAULT_PORT = 25566;
	private static final int MIN_PORT = 1;
	private static final int MAX_PORT = 65535;

	public boolean enabled = true;
	public int port = DEFAULT_PORT;

	/**
	 * Loads the config from disk, creating (and persisting) defaults when absent
	 * or unreadable. Always returns a usable, sanitized instance.
	 */
	public static MetisConfig load() {
		Path path = configPath();
		MetisConfig config = new MetisConfig();

		if (Files.exists(path)) {
			try {
				MetisConfig parsed = GSON.fromJson(Files.readString(path), MetisConfig.class);
				if (parsed != null) {
					config = parsed;
				}
			} catch (Exception e) {
				Metis.LOGGER.warn("Could not read {}, falling back to defaults: {}", FILE_NAME, e.getMessage());
			}
		}

		config.sanitize();
		config.save();
		return config;
	}

	/**
	 * Clamps invalid persisted values back to defaults.
	 */
	private void sanitize() {
		if (port < MIN_PORT || port > MAX_PORT) {
			port = DEFAULT_PORT;
		}
	}

	/**
	 * Writes the current config to disk (pretty-printed). Failures are logged, not fatal.
	 */
	public void save() {
		Path path = configPath();
		try {
			Files.writeString(path, GSON.toJson(this));
		} catch (IOException e) {
			Metis.LOGGER.warn("Could not write {}: {}", FILE_NAME, e.getMessage());
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}
}
