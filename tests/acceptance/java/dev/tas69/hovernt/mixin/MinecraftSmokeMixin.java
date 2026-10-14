package dev.tas69.hovernt.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import dev.tas69.hovernt.SidebarLayout;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftSmokeMixin {
    @Unique private int hovernt$frames;
    @Unique private boolean hovernt$opened;

    @Inject(method = "tick", at = @At("TAIL"))
    private void hovernt$openFixture(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (hovernt$opened || mc.screen == null || mc.getOverlay() != null) return;
        if (!Component.translatable("hovernt.books").getString().equals("Books")) throw new AssertionError("Sidebar translations did not load");
        mc.options.guiScale().set(2);
        mc.resizeDisplay();
        mc.options.onboardAccessibility = false;
        var inventory = new Inventory(null);
        var menu = new MerchantMenu(1, inventory);
        var offers = new MerchantOffers();
        var enchantments = new net.minecraft.world.item.enchantment.Enchantment[]{
            Enchantments.SHARPNESS, Enchantments.UNBREAKING, Enchantments.BINDING_CURSE,
            Enchantments.MENDING, Enchantments.ALL_DAMAGE_PROTECTION,
            Enchantments.FALL_PROTECTION, Enchantments.BLOCK_FORTUNE,
            Enchantments.SILK_TOUCH, Enchantments.MOB_LOOTING};
        for (int i = 0; i < enchantments.length; i++) {
            var book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantments[i], Math.max(1, enchantments[i].getMaxLevel())));
            if (i == 4) EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(Enchantments.UNBREAKING, 3));
            var offer = new MerchantOffer(new ItemStack(Items.EMERALD, 12 + i), new ItemStack(Items.BOOK), book, 12, 1, 0.05F);
            if (i == 2) offer.setToOutOfStock();
            offers.add(offer);
        }
        menu.setOffers(offers);
        mc.setScreen(new MerchantScreen(menu, inventory, Component.literal("Hovern't — trade labels")));
        hovernt$opened = true;
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void hovernt$captureFixture(boolean render, CallbackInfo ci) throws Exception {
        if (!hovernt$opened || !render) return;
        Minecraft mc = (Minecraft) (Object) this;
        hovernt$frames++;
        if (hovernt$frames == 100 || hovernt$frames == 200 || hovernt$frames == 250 || hovernt$frames == 300) {
            Path path = Path.of(System.getProperty("hovernt.screenshots", "screenshots"));
            Files.createDirectories(path);
            try (var screenshot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                String filename = switch (hovernt$frames) {
                    case 100 -> "merchant.png";
                    case 200 -> "merchant-scrolled.png";
                    case 250 -> "merchant-collapsed.png";
                    default -> "merchant-reexpanded.png";
                };
                screenshot.writeToFile(path.resolve(filename));
            }
            if (hovernt$frames == 100) mc.screen.mouseScrolled(0, 0, -2);
            if (hovernt$frames == 200 || hovernt$frames == 250) {
                var toggle = mc.screen.children().stream().filter(child -> child instanceof Button)
                    .map(child -> (Button) child)
                    .filter(button -> button.getMessage().getString().equals(SidebarLayout.expanded ? ">" : "<"))
                    .findFirst().orElseThrow();
                if (!mc.screen.mouseClicked(toggle.getX() + 8, toggle.getY() + 6, 0)) throw new AssertionError("Toggle hitbox");
                if (SidebarLayout.expanded != (hovernt$frames == 250)) throw new AssertionError("Toggle state");
            }
        }
        if (hovernt$frames > 310) {
            System.out.println("Hovern't: merchant rendering, scrolling, collapse, and expansion passed.");
            mc.stop();
        }
    }
}
