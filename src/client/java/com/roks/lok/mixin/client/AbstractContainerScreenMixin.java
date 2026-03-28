package com.roks.lok.mixin.client;

import com.roks.lok.HotbarLockClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected int leftPos;

	@Shadow
	protected int topPos;

	@Shadow
	protected AbstractContainerMenu menu;

	@Shadow
	@Nullable
	protected Slot hoveredSlot;

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void hotbarlock$cancelMouseClickOnLockedHotbarSlot(MouseButtonEvent context, boolean handled, CallbackInfoReturnable<Boolean> cir) {
		Slot slot = hotbarlock$resolveSlotAt(context.x(), context.y());
		if (slot == null) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		int hotbarIndex = hotbarlock$toHotbarIndex(slot, minecraft.player.getInventory());
		if (hotbarIndex >= 0 && HotbarLockClient.isSlotLocked(hotbarIndex)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void hotbarlock$cancelKeyActionsOnLockedHotbarSlot(KeyEvent context, CallbackInfoReturnable<Boolean> cir) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		for (int i = 0; i < 9; i++) {
			if (minecraft.options.keyHotbarSlots[i].matches(context) && HotbarLockClient.isSlotLocked(i)) {
				cir.setReturnValue(true);
				return;
			}
		}

		if (minecraft.options.keySwapOffhand.matches(context)) {
			int hoveredHotbarIndex = hotbarlock$toHotbarIndex(this.hoveredSlot, minecraft.player.getInventory());
			if (hoveredHotbarIndex >= 0 && HotbarLockClient.isSlotLocked(hoveredHotbarIndex)) {
				cir.setReturnValue(true);
			}
		}
	}

	@Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
	private void hotbarlock$cancelSlotClickedForLockedHotbar(@Nullable Slot slot, int slotId, int mouseButton, ClickType clickType, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || slot == null) {
			return;
		}

		int hotbarIndex = hotbarlock$toHotbarIndex(slot, minecraft.player.getInventory());
		if (hotbarIndex >= 0 && HotbarLockClient.isSlotLocked(hotbarIndex)) {
			ci.cancel();
		}
	}

	@Unique
	private static int hotbarlock$toHotbarIndex(Slot slot, Inventory playerInventory) {
		if (slot == null) {
			return -1;
		}

		if (slot.container == playerInventory) {
			int containerSlot = slot.getContainerSlot();
			if (containerSlot >= 0 && containerSlot <= 8) {
				return containerSlot;
			}
		}

		int slotIndex = slot.index;
		if (slotIndex < 36 || slotIndex > 44) {
			return -1;
		}
		return slotIndex - 36;
	}

	@Unique
	@Nullable
	private Slot hotbarlock$resolveSlotAt(double mouseX, double mouseY) {
		for (int i = this.menu.slots.size() - 1; i >= 0; i--) {
			Slot slot = this.menu.slots.get(i);
			if (!slot.isActive()) {
				continue;
			}

			int slotX = this.leftPos + slot.x;
			int slotY = this.topPos + slot.y;
			if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
				return slot;
			}
		}

		return this.hoveredSlot;
	}
}

