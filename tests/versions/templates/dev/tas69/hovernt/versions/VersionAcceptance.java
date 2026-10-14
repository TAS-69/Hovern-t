package dev.tas69.hovernt.versions;

import com.google.gson.GsonBuilder;
import dev.tas69.hovernt.BookNames;
import dev.tas69.hovernt.SidebarLayout;
import dev.tas69.hovernt.mixin.VersionMerchantAccessor;
import dev.tas69.hovernt.mixin.VersionScreenAccessor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class VersionAcceptance {
    private static final Path OUTPUT = Path.of("${outputDirectory}");
    private static final Map<String, Object> REPORT = new LinkedHashMap<>();
    private static final Map<Integer, String> DRAWN = new HashMap<>();
    private static final List<String> EXPECTED = new ArrayList<>();
    private static boolean started;
    private static boolean finished;
    private static int frames;
    private static int phase;
    private static int checks;
    private static volatile Throwable screenshotError;

    private VersionAcceptance() {}

    public static void tick(Minecraft mc) {
        if (started || finished || ${screen} == null || ${overlay} != null) return;
        started = true;
        try {
            Files.createDirectories(OUTPUT);
            for (String name : List.of("expanded.png", "scrolled.png", "collapsed.png", "reexpanded.png")) Files.deleteIfExists(OUTPUT.resolve(name));
            REPORT.put("minecraft", "${minecraft}");
            REPORT.put("loader", "${loader}");
            REPORT.put("java", System.getProperty("java.version"));
            require(Component.translatable("hovernt.books").getString().equals("Books"), "Language resources loaded");
            mc.options.guiScale().set(2);
            mc.${resize}();
            SidebarLayout.expanded = true;
            var inventory = ${inventory};
            var menu = new MerchantMenu(1, inventory);
            var offers = new MerchantOffers();
            var evidence = new ArrayList<Map<String, Object>>();
            for (var entry : VersionBooks.create()) {
                var names = BookNames.read(entry.stack());
                require(names.size() == entry.names().size(), "Enchantment count");
                var remaining = new ArrayList<>(entry.names());
                for (var actual : names) {
                    var expected = remaining.stream().filter(name -> name.getString().equals(actual.getString())).findFirst().orElse(null);
                    require(expected != null, "Enchantment name and level: " + actual.getString());
                    require(java.util.Objects.equals(actual.getStyle().getColor(), expected.getStyle().getColor()), "Curse and enchantment color");
                    remaining.remove(expected);
                }
                String expected = names.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("  /  "));
                EXPECTED.add(expected);
                evidence.add(Map.of("index", EXPECTED.size() - 1, "expected", expected, "enchantments", names.size()));
                var book = entry.stack();
                offers.add(${merchantOffer});
            }
            offers.get(3).setToOutOfStock();
            REPORT.put("offers", evidence);
            menu.setOffers(offers);
            ${setScreen}(new MerchantScreen(menu, inventory, Component.literal("Hovern't")));
        } catch (Throwable error) { fail(mc, error); }
    }

    public static void drawn(Component label, int index, int row, int left, int top, int panelWidth) {
        if (started && !finished) DRAWN.put(index, label.getString());
    }

    public static void frame(Minecraft mc) {
        if (!started || finished) return;
        try {
            if (screenshotError != null) throw screenshotError;
            if (++frames < 30) return;
            var screen = (VersionScreenAccessor) ${screen};
            var merchant = (VersionMerchantAccessor) ${screen};
            int sidebar = SidebarLayout.width(${screen}.width, screen.hoverntWidth());
            int leftMargin = screen.hoverntLeft() - sidebar;
            int rightMargin = ${screen}.width - screen.hoverntLeft() - screen.hoverntWidth();
            require(Math.abs(leftMargin - rightMargin) <= 1, "Complete frame is centered");
            if (phase == 0 || phase == 1 || phase == 3) {
                int scroll = merchant.hoverntScroll();
                for (int index = scroll; index < Math.min(scroll + 7, EXPECTED.size()); index++) {
                    String expected = EXPECTED.get(index);
                    require(expected.isEmpty() ? !DRAWN.containsKey(index) : expected.equals(DRAWN.get(index)), "Visible offer label " + index);
                }
            }
            if (phase == 0) {
                REPORT.put("geometry", Map.of("width", ${screen}.width, "height", ${screen}.height,
                    "left", screen.hoverntLeft(), "top", screen.hoverntTop(), "merchantWidth", screen.hoverntWidth(),
                    "merchantHeight", screen.hoverntHeight(), "sidebar", sidebar));
                capture(mc, "expanded.png");
                require(${screen}.mouseScrolled(${scrollArguments}), "Trade list scroll accepts input");
                require(merchant.hoverntScroll() == 2, "Trade list scroll position");
            } else if (phase == 1) {
                capture(mc, "scrolled.png");
                toggle(mc, ">");
                require(!SidebarLayout.expanded, "Collapse state");
                require(((VersionMerchantAccessor) ${screen}).hoverntScroll() == 2, "Collapse keeps scroll position");
            } else if (phase == 2) {
                require(DRAWN.isEmpty(), "Collapsed sidebar draws no labels");
                capture(mc, "collapsed.png");
                toggle(mc, "<");
                require(SidebarLayout.expanded, "Expand state");
                require(((VersionMerchantAccessor) ${screen}).hoverntScroll() == 2, "Expansion keeps scroll position");
            } else if (phase == 3) {
                capture(mc, "reexpanded.png");
            } else {
                for (String name : List.of("expanded.png", "scrolled.png", "collapsed.png", "reexpanded.png")) {
                    if (!Files.exists(OUTPUT.resolve(name))) return;
                }
                REPORT.put("status", "passed");
                REPORT.put("checks", checks);
                write();
                System.out.println("HOVERNT_VERSION_ACCEPTANCE_PASSED ${minecraft} ${loader} checks=" + checks);
                finished = true;
                mc.stop();
                return;
            }
            phase++;
            frames = 0;
            DRAWN.clear();
        } catch (Throwable error) { fail(mc, error); }
    }

    private static void toggle(Minecraft mc, String arrow) {
        var button = ${screen}.children().stream().filter(child -> child instanceof Button).map(child -> (Button) child)
            .filter(widget -> widget.getMessage().getString().equals(arrow)).findFirst().orElseThrow();
        require(${clickButton}, "Toggle mouse click");
    }

    private static void capture(Minecraft mc, String filename) throws Exception {
        ${captureScreenshot}
    }

    private static void require(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }

    private static void fail(Minecraft mc, Throwable error) {
        finished = true;
        REPORT.put("status", "failed");
        REPORT.put("checks", checks);
        REPORT.put("error", error.toString());
        error.printStackTrace();
        try { write(); } catch (Exception failure) { failure.printStackTrace(); }
        mc.stop();
    }

    private static void write() throws Exception {
        Files.createDirectories(OUTPUT);
        Files.writeString(OUTPUT.resolve("client-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(REPORT));
    }
}
