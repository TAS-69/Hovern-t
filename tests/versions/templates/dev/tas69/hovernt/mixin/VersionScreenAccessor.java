package dev.tas69.hovernt.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface VersionScreenAccessor {
    @Accessor("leftPos") int hoverntLeft();
    @Accessor("topPos") int hoverntTop();
    @Accessor("imageWidth") int hoverntWidth();
    @Accessor("imageHeight") int hoverntHeight();
}
