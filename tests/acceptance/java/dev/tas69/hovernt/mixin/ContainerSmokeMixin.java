package dev.tas69.hovernt.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerSmokeMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void hovernt$noWorldTick(CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) ci.cancel();
    }
}
