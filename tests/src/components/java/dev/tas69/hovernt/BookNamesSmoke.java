package dev.tas69.hovernt;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class BookNamesSmoke {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        check(BookNames.read(ItemStack.EMPTY).isEmpty(), "empty stack");
        check(BookNames.read(new ItemStack(Items.BOOK)).isEmpty(), "ordinary book");
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(Enchantments.SHARPNESS, 5);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        check(BookNames.read(book).get(0).getString().equals(Enchantments.SHARPNESS.getFullname(5).getString()), "name and level");
        enchantments.set(Enchantments.UNBREAKING, 3);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        check(BookNames.read(book).size() == 2, "multiple enchantments");
        var curse = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        curse.set(Enchantments.BINDING_CURSE, 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, curse.toImmutable());
        check(BookNames.read(book).get(0).getStyle().getColor().getValue() == 0xFF5555, "curse color");
        book.set(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        book.set(DataComponents.ENCHANTMENTS, curse.toImmutable());
        check(BookNames.read(book).size() == 1, "applied enchantment fallback");
        check(Marquee.offset(10, 20, 5000) == 0, "short text");
        check(Marquee.offset(30, 20, 2250) == 10, "complete long name");
        System.out.println("Hovern't: 8 component-enchantment and marquee checks passed.");
    }

    private static void check(boolean success, String name) {
        if (!success) throw new AssertionError(name);
    }
}
