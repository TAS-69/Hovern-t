package dev.tas69.hovernt.versions;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
${bookImports}

public final class VersionBooks {
    public record Entry(ItemStack stack, List<Component> names) {}

    public static List<Entry> create() throws Exception {
        ${registrySetup}
        var enchantments = List.of(${enchantmentValues});
        int[] levels = {5, 3, 1, 1, 4, 4, 3, 1, 3};
        var entries = new ArrayList<Entry>();
        for (int i = 0; i < enchantments.size(); i++) {
            var enchantment = enchantments.get(i);
            var stack = new ItemStack(Items.ENCHANTED_BOOK);
            add(stack, enchantment, levels[i]);
            var names = new ArrayList<Component>();
            names.add(name(enchantment, levels[i]));
            if (i == 4) {
                add(stack, enchantments.get(1), 3);
                names.add(name(enchantments.get(1), 3));
            }
            entries.add(new Entry(stack, names));
        }
        ${customBook}
        entries.add(1, new Entry(new ItemStack(Items.APPLE), List.of()));
        return entries;
    }

    private static Component name(${enchantmentType} enchantment, int level) {
        return ${fullName};
    }

    private static void add(ItemStack stack, ${enchantmentType} enchantment, int level) {
        ${addEnchantment}
    }

    ${customClass}
}
