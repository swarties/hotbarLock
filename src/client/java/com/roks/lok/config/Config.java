package com.roks.lok.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("hotbarlock.json");
	private static final int HOTBAR_SIZE = 9;

	private boolean[] lockedSlots = new boolean[HOTBAR_SIZE];
	private int toggleHotkey = GLFW.GLFW_KEY_0;
	private int windowX = 20;
	private int windowY = 20;
	private boolean windowMinimized;

	public static Config load() {
		if (!Files.exists(CONFIG_PATH)) {
			Config config = new Config();
			config.save();
			return config;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			Config parsed = GSON.fromJson(reader, Config.class);
			if (parsed == null) {
				parsed = new Config();
			}
			parsed.normalize();
			return parsed;
		} catch (Exception ignored) {
			Config fallback = new Config();
			fallback.save();
			return fallback;
		}
	}

	public void save() {
		normalize();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public boolean[] getLockedSlots() {
		normalize();
		return lockedSlots;
	}

	public boolean isSlotLocked(int hotbarIndex) {
		if (hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE) {
			return false;
		}
		normalize();
		return lockedSlots[hotbarIndex];
	}

	public void setSlotLocked(int hotbarIndex, boolean locked) {
		if (hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE) {
			return;
		}
		normalize();
		lockedSlots[hotbarIndex] = locked;
		save();
	}

	public int getToggleHotkeyOrDefault(int defaultKey) {
		return toggleHotkey == GLFW.GLFW_KEY_UNKNOWN ? defaultKey : toggleHotkey;
	}

	public void setToggleHotkey(int toggleHotkey) {
		this.toggleHotkey = toggleHotkey;
		save();
	}

	public int getWindowX() {
		return windowX;
	}

	public int getWindowY() {
		return windowY;
	}

	public void setWindowPosition(int windowX, int windowY) {
		this.windowX = windowX;
		this.windowY = windowY;
		save();
	}

	public boolean isWindowMinimized() {
		return windowMinimized;
	}

	public void setWindowMinimized(boolean windowMinimized) {
		this.windowMinimized = windowMinimized;
		save();
	}

	private void normalize() {
		if (lockedSlots == null) {
			lockedSlots = new boolean[HOTBAR_SIZE];
		} else if (lockedSlots.length != HOTBAR_SIZE) {
			lockedSlots = Arrays.copyOf(lockedSlots, HOTBAR_SIZE);
		}
		if (toggleHotkey == 0) {
			toggleHotkey = GLFW.GLFW_KEY_0;
		}
	}
}

