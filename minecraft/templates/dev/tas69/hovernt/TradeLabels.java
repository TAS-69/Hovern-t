package dev.tas69.hovernt;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.${graphics};
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.trading.MerchantOffers;

public final class TradeLabels {
    private static final float SCALE = 1.0F;

    private TradeLabels() {}

    public static void render(${graphics} graphics, Font font, MerchantOffers offers, int scroll, int left, int top, long elapsedMs, int panelWidth) {
        if (panelWidth < 8) return;
        int x = -panelWidth;
        if (panelWidth > font.width(Component.translatable("hovernt.books")) + 32) {
            graphics.${drawText}(font, Component.translatable("hovernt.books"), x + 26, 5, 0xFF3F3F3F, false);
        }
        int first = Math.max(0, Math.min(scroll, Math.max(0, offers.size() - 7)));
        for (int row = 0; row < 7 && first + row < offers.size(); row++) {
            var names = BookNames.read(offers.get(first + row).getResult());
            if (names.isEmpty()) continue;
            MutableComponent label = Component.empty();
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) label.append(Component.literal("  /  ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                label.append(names.get(i).copy().withStyle(style -> {
                    var color = style.getColor();
                    return color == null || color.getValue() == 0xAAAAAA ? style.withColor(0x3F3F3F) : style;
                }));
            }
            int y = 17 + row * 20;
            graphics.pose().${push}();
            graphics.pose().translate(${translateArgs});
            graphics.pose().scale(${scaleArgs});
            int available = Math.max(1, panelWidth - 10);
            int textWidth = font.width(label);
            int offset = Marquee.offset(textWidth, available, elapsedMs);
            graphics.enableScissor(${scissor});
            graphics.${drawText}(font, label, 6 - offset, 6, 0xFF3F3F3F, false);
            ${observeLabel}
            graphics.disableScissor();
            graphics.pose().${pop}();
        }
    }
}
