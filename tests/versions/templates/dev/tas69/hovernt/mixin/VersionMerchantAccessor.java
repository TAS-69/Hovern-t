package dev.tas69.hovernt.mixin;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantScreen.class)
public interface VersionMerchantAccessor {
    @Accessor("scrollOff") int hoverntScroll();
}
