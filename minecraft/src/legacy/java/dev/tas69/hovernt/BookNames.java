package dev.tas69.hovernt;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class BookNames {
    private BookNames() {}

    public static List<Component> read(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        var result = new ArrayList<Component>();
        if (stack.getTag() != null && stack.getTag().contains("StoredEnchantments", 9)) {
            EnchantmentHelper.deserializeEnchantments(stack.getTag().getList("StoredEnchantments", 10))
                .forEach((enchantment, level) -> result.add(enchantment.getFullname(level)));
        }
        if (result.isEmpty()) {
            EnchantmentHelper.deserializeEnchantments(stack.getEnchantmentTags())
                .forEach((enchantment, level) -> result.add(enchantment.getFullname(level)));
        }
        return result;
    }
}
