package com.roks.lok.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.roks.lok.HotbarLockClient;
import com.roks.lok.config.Config;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

public final class HotbarLockOverlay {
	private static final int WIDTH = 132;
	private static final int TITLE_HEIGHT = 14;
	private static final int ROW_HEIGHT = 12;
	private static final int CONTENT_PADDING = 6;
	private static final int HOTKEY_BUTTON_HEIGHT = 14;

	private static boolean visible;
	private static boolean listeningForHotkey;
	private static boolean dragging;
	private static int dragOffsetX;
	private static int dragOffsetY;
	private static int dragPreviewX;
	private static int dragPreviewY;

	private HotbarLockOverlay() {
	}

	public static void register() {
		ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?>)) {
				return;
			}

			ScreenEvents.afterRender(screen).register((current, guiGraphics, mouseX, mouseY, tickDelta) -> {
				if (!visible) {
					return;
				}
				render(current, guiGraphics, mouseX, mouseY);
			});

			ScreenMouseEvents.allowMouseClick(screen).register((current, context) ->
				allowMouseClick(current, context)
			);
			ScreenMouseEvents.afterMouseRelease(screen).register((current, context, consumed) ->
				afterMouseRelease(current, context, consumed)
			);
			ScreenKeyboardEvents.allowKeyPress(screen).register((current, context) ->
				allowKeyPress(current, context)
			);
		});
	}

	public static void toggleVisible() {
		visible = !visible;
		if (!visible) {
			listeningForHotkey = false;
			dragging = false;
		}
	}

	private static boolean allowMouseClick(Screen screen, MouseButtonEvent context) {
		if (!visible) {
			return true;
		}
		double mouseX = context.x();
		double mouseY = context.y();
		int button = context.button();

		Config config = HotbarLockClient.getConfig();
		if (config == null) {
			return true;
		}

		int x = clampX(config.getWindowX(), screen.width);
		int y = clampY(config.getWindowY(), screen.height, config.isWindowMinimized());
		int height = getWindowHeight(config.isWindowMinimized());
		if (!isInside((int) mouseX, (int) mouseY, x, y, WIDTH, height)) {
			return true;
		}

		if (button != 0) {
			return false;
		}

		int minimizeX = x + WIDTH - 14;
		if (isInside((int) mouseX, (int) mouseY, minimizeX, y + 1, 12, TITLE_HEIGHT - 2)) {
			config.setWindowMinimized(!config.isWindowMinimized());
			return false;
		}

		if (isInside((int) mouseX, (int) mouseY, x, y, WIDTH, TITLE_HEIGHT)) {
			dragging = true;
			dragOffsetX = (int) mouseX - x;
			dragOffsetY = (int) mouseY - y;
			dragPreviewX = x;
			dragPreviewY = y;
			return false;
		}

		if (config.isWindowMinimized()) {
			return false;
		}

		int contentTop = y + TITLE_HEIGHT + CONTENT_PADDING;
		for (int slot = 0; slot < 9; slot++) {
			int rowTop = contentTop + slot * ROW_HEIGHT;
			if (isInside((int) mouseX, (int) mouseY, x + CONTENT_PADDING, rowTop, WIDTH - CONTENT_PADDING * 2, ROW_HEIGHT)) {
				config.setSlotLocked(slot, !config.isSlotLocked(slot));
				return false;
			}
		}

		int hotkeyTop = contentTop + 9 * ROW_HEIGHT + CONTENT_PADDING;
		if (isInside((int) mouseX, (int) mouseY, x + CONTENT_PADDING, hotkeyTop, WIDTH - CONTENT_PADDING * 2, HOTKEY_BUTTON_HEIGHT)) {
			listeningForHotkey = true;
			return false;
		}

		return false;
	}

	private static boolean afterMouseRelease(Screen screen, MouseButtonEvent context, boolean consumed) {
		if (!visible || context.button() != 0 || !dragging) {
			return consumed;
		}

		dragging = false;
		Config config = HotbarLockClient.getConfig();
		if (config != null) {
			config.setWindowPosition(
				clampX(dragPreviewX, screen.width),
				clampY(dragPreviewY, screen.height, config.isWindowMinimized())
			);
		}
		return true;
	}

	private static boolean allowKeyPress(Screen screen, KeyEvent context) {
		Config config = HotbarLockClient.getConfig();
		if (config == null) {
			return true;
		}

		if (listeningForHotkey) {
			HotbarLockClient.updateToggleHotkey(context.key());
			listeningForHotkey = false;
			return false;
		}

		if (context.key() == config.getToggleHotkeyOrDefault(GLFW.GLFW_KEY_0)) {
			toggleVisible();
			return false;
		}

		return true;
	}

	private static void render(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		Config config = HotbarLockClient.getConfig();
		if (config == null) {
			return;
		}

		if (dragging) {
			dragPreviewX = clampX(mouseX - dragOffsetX, screen.width);
			dragPreviewY = clampY(mouseY - dragOffsetY, screen.height, config.isWindowMinimized());
		}

		int x = dragging ? dragPreviewX : clampX(config.getWindowX(), screen.width);
		int y = dragging ? dragPreviewY : clampY(config.getWindowY(), screen.height, config.isWindowMinimized());
		int height = getWindowHeight(config.isWindowMinimized());
		if (!dragging && (x != config.getWindowX() || y != config.getWindowY())) {
			config.setWindowPosition(x, y);
		}

		Font font = Minecraft.getInstance().font;
		guiGraphics.fill(x, y, x + WIDTH, y + height, 0xCC111111);
		guiGraphics.fill(x, y, x + WIDTH, y + TITLE_HEIGHT, 0xCC2B2B2B);
		guiGraphics.drawString(font, "HotbarLock", x + 4, y + 3, 0xFFFFFFFF, false);

		String minText = config.isWindowMinimized() ? "+" : "-";
		guiGraphics.drawString(font, minText, x + WIDTH - 10, y + 3, 0xFFFFFFFF, false);

		if (config.isWindowMinimized()) {
			return;
		}

		int contentTop = y + TITLE_HEIGHT + CONTENT_PADDING;
		for (int slot = 0; slot < 9; slot++) {
			int rowTop = contentTop + slot * ROW_HEIGHT;
			boolean locked = config.isSlotLocked(slot);

			int boxX = x + CONTENT_PADDING;
			int boxY = rowTop + 1;
			guiGraphics.fill(boxX, boxY, boxX + 9, boxY + 9, 0xFF555555);
			if (locked) {
				guiGraphics.fill(boxX + 2, boxY + 2, boxX + 7, boxY + 7, 0xFF00CC66);
			}

			guiGraphics.drawString(font, "Slot " + (slot + 1), boxX + 14, rowTop + 2, 0xFFFFFFFF, false);
		}

		int buttonTop = contentTop + 9 * ROW_HEIGHT + CONTENT_PADDING;
		int buttonX = x + CONTENT_PADDING;
		int buttonWidth = WIDTH - CONTENT_PADDING * 2;
		guiGraphics.fill(buttonX, buttonTop, buttonX + buttonWidth, buttonTop + HOTKEY_BUTTON_HEIGHT, 0xFF3A3A3A);

		String keyLabel = listeningForHotkey
			? "Hotkey: [press key]"
			: "Hotkey: [" + keyName(config.getToggleHotkeyOrDefault(InputConstants.UNKNOWN.getValue())) + "]";
		guiGraphics.drawString(font, keyLabel, buttonX + 4, buttonTop + 3, 0xFFFFFFFF, false);
	}

	private static int getWindowHeight(boolean minimized) {
		if (minimized) {
			return TITLE_HEIGHT;
		}
		return TITLE_HEIGHT + CONTENT_PADDING + (9 * ROW_HEIGHT) + CONTENT_PADDING + HOTKEY_BUTTON_HEIGHT + CONTENT_PADDING;
	}

	private static int clampX(int x, int screenWidth) {
		return Math.max(0, Math.min(x, Math.max(0, screenWidth - WIDTH)));
	}

	private static int clampY(int y, int screenHeight, boolean minimized) {
		int height = getWindowHeight(minimized);
		return Math.max(0, Math.min(y, Math.max(0, screenHeight - height)));
	}

	private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static String keyName(int keyCode) {
		return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
	}
}




