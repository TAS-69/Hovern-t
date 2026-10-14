package dev.tas69.hovernt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.${graphics};
import net.minecraft.resources.${resourceType};

public final class SidebarSkin {
    private static final ${resourceType} CUSTOM = ${resourceCreate};

    private SidebarSkin() {}

    public static void draw(${graphics} graphics, ${resourceType} merchant, int left, int top, int width, int height) {
        int x = left - width;
        if (Minecraft.getInstance().getResourceManager().getResource(CUSTOM).isPresent()) {
            slice(graphics, CUSTOM, x, top, 4, 4, 0, 0, 4, 4, 194, 166);
            slice(graphics, CUSTOM, x, top + 4, 4, height - 8, 0, 4, 4, 158, 194, 166);
            slice(graphics, CUSTOM, x, top + height - 4, 4, 4, 0, 162, 4, 4, 194, 166);
            slice(graphics, CUSTOM, x + 4, top, width, 4, 4, 0, 190, 4, 194, 166);
            slice(graphics, CUSTOM, x + 4, top + 4, width, height - 8, 4, 4, 190, 158, 194, 166);
            slice(graphics, CUSTOM, x + 4, top + height - 4, width, 4, 4, 162, 190, 4, 194, 166);
        } else {
            slice(graphics, merchant, x + 4, top, width, height, 102, 0, 4, 166, 512, 256);
            slice(graphics, merchant, x, top, 4, height, 0, 0, 4, 166, 512, 256);
        }
    }

    private static void slice(${graphics} graphics, ${resourceType} texture,
            int x, int y, int width, int height, float u, float v,
            int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        ${blitCall};
    }
}
