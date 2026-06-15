package dev.suppenterrine.metis;

import dev.suppenterrine.metis.config.MetisConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common mod entrypoint. Loads the lightweight JSON config once at startup and
 * exposes it to the client-side HTTP server.
 *
 * <p>Metis is the perception sensor of the MCDS satellite ecosystem: it reads
 * what the player's own client sees and serves it over a local HTTP API.
 */
public class Metis implements ModInitializer {
	public static final String MOD_ID = "metis";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static MetisConfig config;

	@Override
	public void onInitialize() {
		config = MetisConfig.load();
		LOGGER.info("Metis initialized - API will be available at http://localhost:{}/api/coords when enabled", config.port);
	}

	/**
	 * Returns the loaded config instance (read at startup from config/metis.json).
	 */
	public static MetisConfig getConfig() {
		return config;
	}
}
