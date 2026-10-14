package dev.tas69.hovernt.compat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.tas69.hovernt.BookNames;
import dev.tas69.hovernt.SidebarLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class CompatibilityFixture {
    private static final Path OUTPUT = Path.of(System.getProperty("hovernt.compat.output"));
    private static final Map<String, Object> REPORT = new LinkedHashMap<>();
    private static final List<Map<String, Object>> CASES = new ArrayList<>();
    private static final List<String> EXPECTED = new ArrayList<>();
    private static final Set<String> RENDERED = new HashSet<>();
    private static boolean started;
    private static boolean finished;
    private static int frames;
    private static int page;
    private static int pageCount;
    private static int phase;
    private static int checks;

    private CompatibilityFixture() {}

    public static void tick(Minecraft mc) {
        if (started || finished || mc.screen == null || mc.getOverlay() != null) return;
        started = true;
        try {
            Files.createDirectories(OUTPUT);
            var profile = JsonParser.parseString(Files.readString(Path.of(System.getProperty("hovernt.compat.profile")))).getAsJsonObject();
            REPORT.put("profile", profile);
            REPORT.put("java", System.getProperty("java.version"));
            var ids = BuiltInRegistries.ENCHANTMENT.keySet().stream()
                .filter(id -> !id.getNamespace().equals("minecraft")).sorted(java.util.Comparator.comparing(ResourceLocation::toString)).toList();
            REPORT.put("allModdedRegistryIds", BuiltInRegistries.ENCHANTMENT.keySet().stream()
                .filter(id -> !id.getNamespace().equals("minecraft")).map(ResourceLocation::toString).sorted().toList());
            require(!ids.isEmpty(), "The selected profile registered no modded enchantments");
            require(Component.translatable("hovernt.books").getString().equals("Books"), "Sidebar language resources");
            REPORT.put("enchantmentCount", ids.size());
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            SidebarLayout.expanded = true;
            var offers = new MerchantOffers();
            for (var id : ids) {
                Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
                var levels = new LinkedHashSet<>(Arrays.asList(1, Math.max(1, enchantment.getMaxLevel()), Math.min(32767, Math.max(2, enchantment.getMaxLevel() + 1))));
                for (int level : levels) {
                    var book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
                    int effectiveLevel = book.getTag().getList("StoredEnchantments", 10).getCompound(0).getShort("lvl");
                    var expected = enchantment.getFullname(effectiveLevel);
                    validate(book, expected, id + " level " + level);
                    var restored = ItemStack.of(book.save(new CompoundTag()));
                    validate(restored, expected, id + " NBT round-trip level " + level);
                    var entry = new LinkedHashMap<String, Object>();
                    entry.put("id", id.toString());
                    entry.put("class", enchantment.getClass().getName());
                    entry.put("level", level);
                    entry.put("storedLevel", effectiveLevel);
                    entry.put("maxLevel", enchantment.getMaxLevel());
                    entry.put("curse", enchantment.isCurse());
                    entry.put("name", expected.getString());
                    entry.put("translated", !expected.getString().contains(enchantment.getDescriptionId()));
                    entry.put("bookNbt", restored.save(new CompoundTag()).toString());
                    CASES.add(entry);
                    EXPECTED.add(expected.getString());
                    offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 12), new ItemStack(Items.BOOK), restored, 12, 1, 0.05F));
                }
            }
            var first = BuiltInRegistries.ENCHANTMENT.get(ids.get(0));
            var secondId = ids.size() > 1 ? ids.get(ids.size() - 1) : new ResourceLocation("minecraft", "mending");
            var second = BuiltInRegistries.ENCHANTMENT.get(secondId);
            var combined = new ItemStack(Items.ENCHANTED_BOOK);
            var stored = new ListTag();
            stored.add(enchantmentTag(ids.get(0), 1));
            stored.add(enchantmentTag(secondId, 1));
            combined.getOrCreateTag().put("StoredEnchantments", stored);
            var combinedNames = BookNames.read(combined);
            require(combinedNames.size() == 2, "Multiple enchantments on one book");
            require(combinedNames.get(0).getString().equals(first.getFullname(1).getString()), "First combined enchantment");
            require(combinedNames.get(1).getString().equals(second.getFullname(1).getString()), "Second combined enchantment");
            EXPECTED.add(first.getFullname(1).getString() + "  /  " + second.getFullname(1).getString());
            offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 32), new ItemStack(Items.BOOK), combined, 12, 1, 0.05F));
            var inventory = new Inventory(null);
            var menu = new MerchantMenu(1, inventory);
            menu.setOffers(offers);
            mc.setScreen(new MerchantScreen(menu, inventory, Component.literal(profile.get("title").getAsString())));
            pageCount = (offers.size() + 6) / 7;
            REPORT.put("bookCount", offers.size());
            REPORT.put("pages", pageCount);
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    public static void rendered(Component component) {
        if (started && !finished) RENDERED.add(component.getString());
    }

    public static void frame(Minecraft mc) {
        if (!started || finished || pageCount == 0) return;
        try {
            frames++;
            if (frames < 18) return;
            if (phase == 0) {
                if (page == 0 || page == pageCount - 1) screenshot(mc, page == 0 ? "first-page.png" : "last-page.png");
                page++;
                if (page < pageCount) {
                    mc.screen.mouseScrolled(0, 0, -7);
                } else {
                    for (String label : EXPECTED) require(RENDERED.contains(label), "A book label was not rendered: " + label);
                    toggle(mc);
                    require(!SidebarLayout.expanded, "Sidebar collapses");
                    phase = 1;
                }
            } else if (phase == 1) {
                screenshot(mc, "collapsed.png");
                toggle(mc);
                require(SidebarLayout.expanded, "Sidebar expands");
                phase = 2;
            } else {
                screenshot(mc, "reexpanded.png");
                REPORT.put("status", "passed");
                REPORT.put("checks", checks);
                REPORT.put("renderedUniqueLabels", RENDERED.size());
                REPORT.put("cases", CASES);
                writeReport();
                System.out.println("HOVERNT_COMPATIBILITY_PASSED enchantments=" + REPORT.get("enchantmentCount") + " checks=" + checks);
                finished = true;
                mc.stop();
            }
            frames = 0;
        } catch (Throwable error) {
            fail(mc, error);
        }
    }

    private static void toggle(Minecraft mc) {
        var button = mc.screen.children().stream().filter(child -> child instanceof Button)
            .map(child -> (Button) child).filter(widget -> widget.getMessage().getString().equals(SidebarLayout.expanded ? ">" : "<"))
            .findFirst().orElseThrow();
        require(mc.screen.mouseClicked(button.getX() + 8, button.getY() + 6, 0), "Toggle accepts its mouse click");
    }

    private static void validate(ItemStack book, Component expected, String name) {
        var actual = BookNames.read(book);
        require(actual.size() == 1, name + " contains exactly one enchantment");
        require(actual.get(0).getString().equals(expected.getString()), name + " expected " + expected.getString() + " but read " + actual.get(0).getString());
        require(java.util.Objects.equals(actual.get(0).getStyle().getColor(), expected.getStyle().getColor()), name + " preserves enchantment color");
    }

    private static CompoundTag enchantmentTag(ResourceLocation id, int level) {
        var tag = new CompoundTag();
        tag.putString("id", id.toString());
        tag.putShort("lvl", (short) level);
        return tag;
    }

    private static void screenshot(Minecraft mc, String name) throws Exception {
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(OUTPUT.resolve(name));
        }
    }

    private static void require(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }

    private static void fail(Minecraft mc, Throwable error) {
        finished = true;
        REPORT.put("status", "failed");
        REPORT.put("error", error.toString());
        REPORT.put("checks", checks);
        REPORT.put("cases", CASES);
        error.printStackTrace();
        try { writeReport(); } catch (Exception writeError) { writeError.printStackTrace(); }
        mc.stop();
    }

    private static void writeReport() throws Exception {
        Files.createDirectories(OUTPUT);
        Files.writeString(OUTPUT.resolve("result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(REPORT));
    }
}
