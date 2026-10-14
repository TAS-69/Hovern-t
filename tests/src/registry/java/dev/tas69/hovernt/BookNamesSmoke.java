package dev.tas69.hovernt;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class BookNamesSmoke {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        try {
            var bindComponents = Holder.Reference.class.getDeclaredMethod("bindComponents", net.minecraft.core.component.DataComponentMap.class);
            bindComponents.invoke(Items.BOOK.builtInRegistryHolder(), net.minecraft.core.component.DataComponentMap.EMPTY);
            bindComponents.invoke(Items.ENCHANTED_BOOK.builtInRegistryHolder(), net.minecraft.core.component.DataComponentMap.EMPTY);
        } catch (NoSuchMethodException ignored) {}
        java.lang.reflect.Method lookup;
        try { lookup = VanillaRegistries.class.getMethod("createLookup"); }
        catch (NoSuchMethodException ignored) { lookup = VanillaRegistries.class.getMethod("createWorldLookup"); }
        var registry = ((net.minecraft.core.HolderLookup.Provider) lookup.invoke(null)).lookupOrThrow(Registries.ENCHANTMENT);
        var bindTags = Holder.Reference.class.getDeclaredMethod("bindTags", java.util.Collection.class);
        bindTags.setAccessible(true);
        for (var holder : registry.listElements().toList()) bindTags.invoke(holder, java.util.List.of());
        var sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
        var curse = registry.getOrThrow(Enchantments.BINDING_CURSE);
        bindTags.invoke(curse, java.util.List.of(net.minecraft.tags.EnchantmentTags.CURSE));
        check(BookNames.read(ItemStack.EMPTY).isEmpty(), "empty stack");
        check(BookNames.read(new ItemStack(Items.BOOK)).isEmpty(), "ordinary book");
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(sharpness, 5);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        check(BookNames.read(book).get(0).getString().equals(Enchantment.getFullname(sharpness, 5).getString()), "translated name and level");
        enchantments.set(registry.getOrThrow(Enchantments.UNBREAKING), 3);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        check(BookNames.read(book).size() == 2, "multiple enchantments");
        var base = sharpness.value();
        var custom = Holder.direct(new Enchantment(Component.literal("Moonwalking"), base.definition(), base.exclusiveSet(), base.effects()));
        enchantments.set(custom, 7);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        check(BookNames.read(book).stream().anyMatch(name -> name.getString().contains("Moonwalking")), "custom enchantment description");
        var cursed = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        cursed.set(curse, 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, cursed.toImmutable());
        check(BookNames.read(book).get(0).getStyle().getColor().getValue() == 0xFF5555, "curse color");
        check(Marquee.offset(10, 20, 5000) == 0, "short text");
        check(Marquee.offset(30, 20, 2250) == 10, "complete long name");
        System.out.println("Hovern't: 8 component-enchantment and marquee checks passed.");
    }

    private static void check(boolean success, String name) {
        if (!success) throw new AssertionError(name);
    }
}
