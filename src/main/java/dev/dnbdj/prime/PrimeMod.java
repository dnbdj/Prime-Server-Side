package dev.dnbdj.prime;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint of the server-side mod.
 *
 * <p>{@code fabric.mod.json} declares {@code "environment": "server"}, so this
 * never loads on a client; the dedicated-server entrypoint in
 * {@code dev.dnbdj.prime.server} does the actual wiring once a server exists.
 */
public class PrimeMod implements ModInitializer {
	public static final String MOD_ID = "prime";

	// Using your mod id as the logger name makes it clear which mod wrote each line.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Runs as soon as the server is in a mod-load-ready state, before any
		// world or player is present.
		LOGGER.info("Prime server side loading.");
	}
}
