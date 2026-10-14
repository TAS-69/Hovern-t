package dev.tas69.hovernt;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class BookNames {
    private BookNames() {}

    public static List<Component> read(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        var enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var result = new ArrayList<Component>();
        for (var entry : enchantments.entrySet()) {
            result.add(entry.getKey().value().getFullname(entry.getIntValue()));
        }
        return result;
    }
}
