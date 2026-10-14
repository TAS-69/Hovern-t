package dev.tas69.hovernt.mixin;

import dev.tas69.hovernt.TradeLabels;
import dev.tas69.hovernt.SidebarLayout;
import dev.tas69.hovernt.SidebarSkin;
import net.minecraft.client.gui.${graphics};
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.${resourceType};
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
    @Shadow private int scrollOff;
    @Shadow @Final private static ${resourceType} VILLAGER_LOCATION;
    @Unique private int hovernt\$lastScroll = Integer.MIN_VALUE;
    @Unique private long hovernt\$motionStart;

    protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Redirect(method = {"init", "${bgMethod}", "${contentsMethod}", "mouseClicked"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;width:I"))
    private int hovernt\$centerWithSidebar(MerchantScreen screen) {
        return width + SidebarLayout.width(width, imageWidth);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void hovernt\$addToggle(CallbackInfo ci) {
        leftPos = SidebarLayout.merchantLeft(width, imageWidth);
        int sidebar = SidebarLayout.width(width, imageWidth);
        var message = Component.translatable(SidebarLayout.expanded ? "hovernt.collapse" : "hovernt.expand");
        addRenderableWidget(Button.builder(Component.literal(SidebarLayout.expanded ? ">" : "<"), button -> {
                SidebarLayout.expanded = !SidebarLayout.expanded;
                hovernt\$lastScroll = Integer.MIN_VALUE;
                init(${initArgs});
            })
            .bounds(leftPos - sidebar + 5, topPos + 3, 16, 12)
            .tooltip(Tooltip.create(message))
            .createNarration(supplier -> message.copy())
            .build());
    }

    @Inject(method = "${bgMethod}", at = @At("TAIL"))
    private void hovernt\$drawFrame(${graphics} graphics, ${bgArguments}, CallbackInfo ci) {
        int sidebar = SidebarLayout.width(width, imageWidth);
        if (sidebar > 0) SidebarSkin.draw(graphics, VILLAGER_LOCATION, leftPos, topPos, sidebar, imageHeight);
    }

    @Override
    protected boolean hasClickedOutside(${outsideArgs}) {
        int sidebar = SidebarLayout.width(width, imageWidth);
        if (mouseX >= left - sidebar && mouseX < left && mouseY >= top && mouseY < top + imageHeight) return false;
        return super.hasClickedOutside(${outsideCall});
    }

    @Inject(method = "${labelsMethod}", at = @At("TAIL"))
    private void hovernt\$drawLabels(${graphics} graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (hovernt\$lastScroll != scrollOff) {
            hovernt\$lastScroll = scrollOff;
            hovernt\$motionStart = System.nanoTime();
        }
        TradeLabels.render(graphics, font, menu.getOffers(), scrollOff, leftPos, topPos,
            (System.nanoTime() - hovernt\$motionStart) / 1_000_000L, SidebarLayout.width(width, imageWidth));
    }
}
