package dev.tas69.hovernt.mixin;

import dev.tas69.hovernt.compat.CompatibilityFixture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class CompatibilityClientMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void hovernt$startCompatibility(CallbackInfo ci) {
        CompatibilityFixture.tick((Minecraft) (Object) this);
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void hovernt$verifyCompatibility(boolean render, CallbackInfo ci) {
        if (render) CompatibilityFixture.frame((Minecraft) (Object) this);
    }
}
