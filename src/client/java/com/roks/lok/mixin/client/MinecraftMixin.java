package com.roks.lok.mixin.client;

import com.roks.lok.HotbarLockClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	@Nullable
	public LocalPlayer player;

	@Shadow
	@Final
	public Options options;

	@Inject(
		method = "handleKeybinds",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/Options;keySwapOffhand:Lnet/minecraft/client/KeyMapping;",
			opcode = Opcodes.GETFIELD
		),
		cancellable = true
	)
	private void hotbarlock$cancelOffhandSwapPacketWhenLocked(CallbackInfo ci) {
		if (this.player == null) {
			return;
		}

		int selectedSlot = this.player.getInventory().getSelectedSlot();
		if (!HotbarLockClient.isSlotLocked(selectedSlot)) {
			return;
		}

		if (hotbarlock$drainSwapOffhandClicks()) {
			ci.cancel();
		}
	}

	@Unique
	private boolean hotbarlock$drainSwapOffhandClicks() {
		boolean consumed = false;
		while (this.options.keySwapOffhand.consumeClick()) {
			consumed = true;
		}
		return consumed;
	}
}

