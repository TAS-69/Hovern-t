package dev.tas69.hovernt;

import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public final class BookNamesSmoke {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var sharpness = Enchantments.SHARPNESS;
        var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        frozen.setBoolean(BuiltInRegistries.ENCHANTMENT, false);
        var intrusive = net.minecraft.core.MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
        intrusive.setAccessible(true);
        if (!SharedConstants.getCurrentVersion().getName().equals("1.20.1") && intrusive.get(BuiltInRegistries.ENCHANTMENT) == null) intrusive.set(BuiltInRegistries.ENCHANTMENT, new java.util.IdentityHashMap<>());
        var custom = Registry.register(BuiltInRegistries.ENCHANTMENT, new ResourceLocation("othermod", "moonwalking"),
            new Enchantment(Enchantment.Rarity.RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.FEET}) {});
        BuiltInRegistries.ENCHANTMENT.freeze();
        check(BookNames.read(ItemStack.EMPTY).isEmpty(), "empty stack");
        check(BookNames.read(new ItemStack(Items.BOOK)).isEmpty(), "ordinary book");
        var book = stored(new ItemStack(Items.ENCHANTED_BOOK), "minecraft:sharpness", 5);
        check(BookNames.read(book).get(0).getString().equals(sharpness.getFullname(5).getString()), "translated name and level");
        var multi = stored(book, "minecraft:unbreaking", 3);
        check(BookNames.read(multi).size() == 2, "all enchantments on one book");
        var curse = stored(new ItemStack(Items.ENCHANTED_BOOK), "minecraft:binding_curse", 1);
        var color = BookNames.read(curse).get(0).getStyle().getColor();
        check(color != null && color.getValue() == 0xFF5555, "curse formatting");
        var modded = stored(new ItemStack(Items.BOOK), "othermod:moonwalking", 7);
        check(BookNames.read(modded).get(0).getString().equals(custom.getFullname(7).getString()), "registered modded enchantment and non-vanilla book format");
        var unknown = stored(new ItemStack(Items.ENCHANTED_BOOK), "missingmod:gone", 2);
        check(BookNames.read(unknown).isEmpty(), "unknown registry ID does not crash");
        var fallback = new ItemStack(Items.BOOK);
        fallback.getOrCreateTag().put("StoredEnchantments", new ListTag());
        fallback.enchant(custom, 4);
        check(BookNames.read(fallback).get(0).getString().equals(custom.getFullname(4).getString()), "modded book using regular enchantment data");
        stored(book, "minecraft:mending", 1);
        check(BookNames.read(book).size() == 3, "live offer mutation");
        check(Marquee.offset(10, 20, 5000) == 0, "short text remains still");
        check(Marquee.offset(30, 20, 1800) == 0, "leading pause");
        check(Marquee.offset(30, 20, 2250) == 10, "long text reaches its final character");
        check(Marquee.offset(30, 20, 4500) == 0, "long text returns to its start");
        check(Marquee.offset(30, 20, Long.MAX_VALUE) >= 0, "monotonic-clock overflow safety");
        check(SidebarLayout.width(427, 276) == 139, "sidebar fits a standard window");
        int merchantLeft = SidebarLayout.merchantLeft(427, 276);
        check(427 - (merchantLeft + 276) == merchantLeft - 139, "complete frame is centered");
        check(SidebarLayout.width(960, 276) == 190, "wide-screen width cap");
        check(SidebarLayout.width(200, 276) == 0, "narrow-screen bounds");
        SidebarLayout.expanded = false;
        check(SidebarLayout.width(427, 276) == 0 && SidebarLayout.merchantLeft(427, 276) == 75, "collapsed frame is centered");
        SidebarLayout.expanded = true;
        System.out.println("Hovern't: 19 book, marquee, and layout checks passed, including a custom registered enchantment.");
    }

    private static ItemStack stored(ItemStack stack, String id, int level) {
        ListTag list = stack.getOrCreateTag().getList("StoredEnchantments", 10);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putShort("lvl", (short) level);
        list.add(tag);
        stack.getOrCreateTag().put("StoredEnchantments", list);
        return stack;
    }

    private static void check(boolean success, String name) {
        if (!success) throw new AssertionError(name);
    }
}
