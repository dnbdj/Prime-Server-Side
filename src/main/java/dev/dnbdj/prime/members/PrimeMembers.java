package dev.dnbdj.prime.members;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.dnbdj.prime.PrimeMod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The verified Prime users, read from {@code config/prime-members.json}.
 *
 * <p>This file is the server's only source of truth for the tick. It is never
 * filled from anything a client sends, so a vanilla client cannot claim to be
 * verified: an operator adds people here by UUID and runs {@code /prime reload}.
 */
public final class PrimeMembers {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "prime-members.json";

	private static Map<UUID, Entry> byUuid = Map.of();
	private static Discord discord = new Discord("", "", "");

	private PrimeMembers() {
	}

	/** One verified player: a label for operators and their Discord id. */
	public record Entry(String name, String discordId) {
	}

	/** Bot credentials used to grant the role; blank values mean "not configured". */
	public record Discord(String token, String guildId, String roleId) {
		public boolean configured() {
			return !blank(token) && !blank(guildId) && !blank(roleId);
		}

		private static boolean blank(String value) {
			return value == null || value.isBlank();
		}
	}

	private record Document(Map<String, Entry> members, Discord discord) {
	}

	/** Reads the file, writing an empty one when it is missing. Never throws at the server. */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

		try {
			if (Files.notExists(path)) {
				Files.createDirectories(path.getParent());
				Files.writeString(path, GSON.toJson(new Document(new LinkedHashMap<>(), new Discord("", "", ""))));
				PrimeMod.LOGGER.info("Wrote an empty {} - add verified players there.", path);
			}

			Document document = GSON.fromJson(Files.readString(path), Document.class);
			Map<UUID, Entry> members = new LinkedHashMap<>();

			if (document != null && document.members() != null) {
				for (Map.Entry<String, Entry> raw : document.members().entrySet()) {
					UUID uuid = parseUuid(raw.getKey());

					if (uuid == null) {
						PrimeMod.LOGGER.warn("Skipping {}: not a UUID.", raw.getKey());
						continue;
					}

					if (raw.getValue() != null) {
						members.put(uuid, raw.getValue());
					}
				}
			}

			byUuid = members;
			discord = document == null || document.discord() == null
					? new Discord("", "", "")
					: document.discord();

			PrimeMod.LOGGER.info(
					"Prime: {} verified member(s), Discord role {}.",
					byUuid.size(),
					discord.configured() ? "enabled" : "disabled");
		} catch (IOException | RuntimeException error) {
			PrimeMod.LOGGER.error("Could not read {}: {}", path, error.getMessage());
		}
	}

	public static boolean isVerified(UUID uuid) {
		return uuid != null && byUuid.containsKey(uuid);
	}

	public static Entry entry(UUID uuid) {
		return uuid == null ? null : byUuid.get(uuid);
	}

	public static Discord discord() {
		return discord;
	}

	public static int size() {
		return byUuid.size();
	}

	private static UUID parseUuid(String value) {
		if (value == null) {
			return null;
		}

		try {
			return UUID.fromString(value.trim());
		} catch (IllegalArgumentException error) {
			return null;
		}
	}
}
