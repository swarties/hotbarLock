package com.roks.lok;

import com.mojang.blaze3d.platform.InputConstants;
import com.roks.lok.config.Config;
import com.roks.lok.ui.HotbarLockOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

public class HotbarLockClient implements ClientModInitializer {
	private static Config config;
	private static boolean toggleKeyWasDown;

	@Override
	public void onInitializeClient() {
		config = Config.load();

		HotbarLockOverlay.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (config == null || client.getWindow() == null) {
				return;
			}

			int toggleHotkey = config.getToggleHotkeyOrDefault(GLFW.GLFW_KEY_0);
			boolean toggleDown = InputConstants.isKeyDown(client.getWindow(), toggleHotkey);
			if (toggleDown && !toggleKeyWasDown && !(client.screen instanceof AbstractContainerScreen<?>)) {
				HotbarLockOverlay.toggleVisible();
			}
			toggleKeyWasDown = toggleDown;
		});
	}

	public static Config getConfig() {
		return config;
	}

	public static boolean isSlotLocked(int hotbarIndex) {
		return config != null && config.isSlotLocked(hotbarIndex);
	}

	public static void updateToggleHotkey(int keyCode) {
		if (config == null) {
			return;
		}
		config.setToggleHotkey(keyCode);
		toggleKeyWasDown = false;
	}
}