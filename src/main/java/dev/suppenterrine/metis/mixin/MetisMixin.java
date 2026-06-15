package dev.suppenterrine.metis.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Empty common-side template mixin — a hook point for future server-side perception.
 */
@Mixin(MinecraftServer.class)
public class MetisMixin {

	@Inject(at = @At("HEAD"), method = "loadWorld")
	private void init(CallbackInfo info) {
		// Reserved for future common-side injections.
	}
}
