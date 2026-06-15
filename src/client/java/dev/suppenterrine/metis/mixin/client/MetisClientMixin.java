package dev.suppenterrine.metis.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Empty client-side template mixin — a hook point for future client-side perception.
 */
@Mixin(MinecraftClient.class)
public class MetisClientMixin {

	@Inject(at = @At("HEAD"), method = "run")
	private void init(CallbackInfo info) {
		// Reserved for future client-side injections.
	}
}
