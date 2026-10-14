package dev.tas69.hovernt.mixin;

import dev.tas69.hovernt.versions.VersionAcceptance;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class VersionAcceptanceMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void hoverntStartVersion(CallbackInfo ci) { VersionAcceptance.tick((Minecraft) (Object) this); }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void hoverntCaptureVersion(boolean render, CallbackInfo ci) {
        if (render) VersionAcceptance.frame((Minecraft) (Object) this);
    }
}
