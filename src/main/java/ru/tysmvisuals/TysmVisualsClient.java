package ru.tysmvisuals;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class TysmVisualsClient implements ClientModInitializer {
    private static KeyBinding menuKey;

    // Visual settings. These are cosmetic/UI settings only.
    private static boolean hudEnabled = true;
    private static boolean redCrosshair = true;
    private static boolean accentBar = true;
    private static int themeIndex = 0;
    private static float pulse;

    private static final Map<String, Boolean> SETTINGS = new HashMap<>();

    private static final int[] THEMES = {
            0xFFFF2633, // Red
            0xFFFF4D6D, // Crimson
            0xFFFF7A18, // Red-orange
            0xFFB52BFF  // Magenta
    };

    private static final String[] THEME_NAMES = {
            "RED", "CRIMSON", "FIRE", "MAGENTA"
    };

    private static final int WHITE = 0xFFF2F2F2;
    private static final int MUTED = 0xFFB8B8C2;
    private static final int PANEL = 0xD90A0B10;
    private static final int ROW = 0x381B1C24;

    private static final String[][] CATEGORIES = {
            {"◉  Visual", "Blink", "Chams", "ChinaHat", "Custom Crystals", "Dog", "Ghost Hand", "Hands", "Hit Bubbles", "Hit Color", "HitBox"},
            {"◎  World", "Ambience", "Bad Trip", "Block Highlight", "Bright", "CrystalColor", "Custom Fog", "Custom Weather", "Item Physics", "Sky Color", "WaveEffect"},
            {"✦  Particles", "Ambient Particles", "Block Particle", "Cursor Particle", "Damage Particle", "Hit Particles", "Jump Particles", "Kill Particle", "Particle Trail", "Totem Particle", "Track Particle"},
            {"⚒  Utils", "Armor Durability", "Arrows", "AspectRatio", "Auto Sprint", "Cape", "Click Friend", "Dynamic Island", "Fake Player", "HitSound", "Kill Sound"},
            {"●  Client", "Theme", "Config Manager", "Custom loading screen", "Friends Manager", "Globals", "GuiSounds", "Interface", "Item Highlighter", "Language", "LiquidGlass"}
    };

    @Override
    public void onInitializeClient() {
        initializeDefaults();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tysmvisuals.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.tysmvisuals"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pulse += 0.08f;
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new TysmMenuScreen());
                }
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (hudEnabled) renderHud(context);
        });
    }

    private static void initializeDefaults() {
        String[] enabled = {
                "Chams", "ChinaHat", "Custom Crystals", "Hit Bubbles",
                "Block Highlight", "Custom Fog", "Sky Color",
                "Cursor Particle", "Damage Particle", "Hit Particles",
                "Kill Particle", "Particle Trail", "Totem Particle",
                "Armor Durability", "Arrows", "Dynamic Island",
                "HitSound", "Kill Sound", "LiquidGlass"
        };
        for (String name : enabled) SETTINGS.put(name, true);
    }

    private static int accent() {
        return THEMES[themeIndex];
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        int red = accent();

        ctx.fill(8, 8, 122, 36, 0xB0101016);
        ctx.fill(8, 8, 11, 36, red);
        ctx.drawText(client.textRenderer, Text.literal("TYSM"), 18, 13, red, true);
        ctx.drawText(client.textRenderer, Text.literal("VISUALS"), 58, 13, WHITE, true);

        if (redCrosshair) {
            int cx = w / 2;
            int cy = h / 2;
            ctx.fill(cx - 6, cy - 1, cx + 7, cy + 1, (0x55 << 24) | (red & 0xFFFFFF));
            ctx.fill(cx - 1, cy - 6, cx + 1, cy + 7, (0x55 << 24) | (red & 0xFFFFFF));
            ctx.fill(cx - 4, cy - 1, cx + 5, cy + 1, red);
            ctx.fill(cx - 1, cy - 4, cx + 1, cy + 5, red);
        }

        if (accentBar) {
            float a = (MathHelper.sin(pulse) + 1f) * 0.5f;
            int alpha = 0x30 + (int) (a * 0x30);
            ctx.fill(w / 2 - 90, h - 52, w / 2 + 90, h - 49,
                    (alpha << 24) | (red & 0xFFFFFF));
        }
    }

    private static boolean enabled(String name) {
        return SETTINGS.getOrDefault(name, false);
    }

    private static void toggle(String name) {
        SETTINGS.put(name, !enabled(name));
    }

    private static class TysmMenuScreen extends Screen {
        private TysmMenuScreen() {
            super(Text.literal("TysmVisuals"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);

            int screenW = width;
            int screenH = height;
            int gap = screenW >= 1100 ? 12 : 8;
            int side = screenW >= 1100 ? 215 : 0;
            int left = 24;
            int top = Math.max(56, screenH / 2 - 150);
            int available = screenW - left - 24 - side - gap * 4;
            int panelW = Math.max(120, Math.min(190, available / 5));
            int panelH = Math.min(440, screenH - top - 42);

            drawBrand(context, 28, 18);
            context.drawText(textRenderer, Text.literal("Fabric 1.21.4"), screenW - 118, 14, accent(), true);
            context.drawText(textRenderer, Text.literal("Client"), screenW - 67, 29, MUTED, false);

            for (int i = 0; i < CATEGORIES.length; i++) {
                int x = left + i * (panelW + gap);
                drawPanel(context, x, top, panelW, panelH, CATEGORIES[i], mouseX, mouseY);
            }

            if (side > 0) {
                int sx = left + 5 * (panelW + gap);
                drawScoreboard(context, sx, top + 68, side - 10, 270);
            }

            drawControls(context, screenW - 245, screenH - 72);
            super.render(context, mouseX, mouseY, delta);
        }

        private void drawBrand(DrawContext ctx, int x, int y) {
            int red = accent();
            ctx.fill(x, y + 3, x + 150, y + 5, red);
            ctx.drawText(textRenderer, Text.literal("TYSM"), x + 8, y + 9, WHITE, true);
            ctx.drawText(textRenderer, Text.literal("VISUALS"), x + 53, y + 9, red, true);
            ctx.drawText(textRenderer, Text.literal("1.21.4  •  " + THEME_NAMES[themeIndex]), x + 9, y + 26, MUTED, false);
        }

        private void drawPanel(DrawContext ctx, int x, int y, int w, int h, String[] rows, int mouseX, int mouseY) {
            int red = accent();
            ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x50000000);
            ctx.fill(x, y, x + w, y + h, PANEL);
            ctx.fill(x, y, x + w, y + 2, red);
            ctx.fill(x, y + 43, x + w, y + 45, (0x88 << 24) | (red & 0xFFFFFF));

            ctx.drawText(textRenderer, Text.literal(rows[0]), x + 14, y + 14, WHITE, true);
            ctx.drawText(textRenderer, Text.literal("⚙"), x + w - 19, y + 14, red, true);

            int rowGap = h < 425 ? 31 : 35;
            for (int i = 1; i < rows.length; i++) {
                int ry = y + 52 + (i - 1) * rowGap;
                boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= ry - 5 && mouseY < ry + rowGap;

                if (hover) {
                    ctx.fill(x + 5, ry - 5, x + w - 5, ry + rowGap - 2, ROW);
                    ctx.fill(x + 5, ry - 5, x + 8, ry + rowGap - 2, red);
                }

                String feature = rows[i];
                ctx.drawText(textRenderer, Text.literal(feature), x + 10, ry + 3,
                        hover ? WHITE : MUTED, false);

                if (enabled(feature)) {
                    ctx.drawText(textRenderer, Text.literal("✓"), x + w - 19, ry + 3, red, true);
                } else if (hasSubmenu(feature)) {
                    ctx.drawText(textRenderer, Text.literal("›"), x + w - 17, ry + 3, red, true);
                } else if (isClickable(feature)) {
                    ctx.drawText(textRenderer, Text.literal("○"), x + w - 19, ry + 3, 0xFF666670, false);
                }
            }
        }

        private boolean isClickable(String name) {
            return name != null && !name.equals("Theme");
        }

        private boolean hasSubmenu(String name) {
            return switch (name) {
                case "Theme", "Config Manager", "Custom loading screen", "Friends Manager",
                     "Globals", "GuiSounds", "Interface", "Item Highlighter", "Language" -> true;
                default -> false;
            };
        }

        private void drawScoreboard(DrawContext ctx, int x, int y, int w, int h) {
            int red = accent();
            ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x50000000);
            ctx.fill(x, y, x + w, y + h, PANEL);
            ctx.fill(x, y, x + w, y + 2, red);

            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("TYSM VISUALS"), x + w / 2, y + 14, red);

            String[] lines = {
                    "♙  Ник:  Player",
                    "✪  Ранг:  Игрок",
                    "▥  Пинг:  42 ms",
                    "",
                    "⚔  Убийств:  3",
                    "☠  Смертей:  1",
                    "$  Баланс:  0$",
                    "",
                    "▣  Сервер:  Practice",
                    "♟  Онлайн:  78",
                    "",
                    "◉  TysmWorld.ru"
            };

            int yy = y + 39;
            for (String line : lines) {
                ctx.drawText(textRenderer, Text.literal(line), x + 12, yy,
                        line.contains("Player") || line.contains("Practice") || line.contains("TysmWorld")
                                ? red : MUTED, false);
                yy += 21;
            }
        }

        private void drawControls(DrawContext ctx, int x, int y) {
            int red = accent();
            ctx.fill(x, y, x + 116, y + 48, 0xC50A0B10);
            ctx.fill(x, y, x + 116, y + 2, red);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("ПРАВ ШИФТ"), x + 58, y + 16, WHITE);

            ctx.fill(x + 126, y, x + 226, y + 48, 0xC50A0B10);
            ctx.fill(x + 126, y, x + 226, y + 2, red);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("MOUSE"), x + 176, y + 16, WHITE);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int screenW = width;
            int gap = screenW >= 1100 ? 12 : 8;
            int side = screenW >= 1100 ? 215 : 0;
            int left = 24;
            int top = Math.max(56, height / 2 - 150);
            int available = screenW - left - 24 - side - gap * 4;
            int panelW = Math.max(120, Math.min(190, available / 5));
            int panelH = Math.min(440, height - top - 42);
            int rowGap = panelH < 425 ? 31 : 35;

            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return super.mouseClicked(mouseX, mouseY, button);
            }

            for (int i = 0; i < CATEGORIES.length; i++) {
                int x = left + i * (panelW + gap);
                for (int row = 1; row < CATEGORIES[i].length; row++) {
                    int ry = top + 52 + (row - 1) * rowGap;
                    if (mouseX >= x + 5 && mouseX <= x + panelW - 5
                            && mouseY >= ry - 5 && mouseY < ry + rowGap) {
                        handleFeatureClick(CATEGORIES[i][row]);
                        return true;
                    }
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        private void handleFeatureClick(String feature) {
            switch (feature) {
                case "Theme" -> {
                    themeIndex = (themeIndex + 1) % THEMES.length;
                }
                case "Interface", "HUD" -> {
                    hudEnabled = !hudEnabled;
                }
                case "Hit Color" -> {
                    // Cosmetic: cycle the visual accent theme.
                    themeIndex = (themeIndex + 1) % THEMES.length;
                }
                case "GuiSounds" -> {
                    toggle("GuiSounds");
                }
                case "Config Manager" -> {
                    // Reset all cosmetic settings to their defaults.
                    SETTINGS.clear();
                    initializeDefaults();
                    hudEnabled = true;
                    redCrosshair = true;
                    accentBar = true;
                    themeIndex = 0;
                }
                case "Custom loading screen", "Friends Manager", "Globals", "Item Highlighter", "Language" -> {
                    toggle(feature);
                }
                default -> {
                    if ("Chams".equals(feature) || "Blink".equals(feature) || "Auto Sprint".equals(feature)
                            || "Fake Player".equals(feature) || "Click Friend".equals(feature)) {
                        // Kept as UI-only toggles; no gameplay automation is performed.
                        toggle(feature);
                    } else {
                        toggle(feature);
                    }

                    if ("HitBox".equals(feature)) {
                        // UI toggle only; does not change collision or targeting.
                        toggle(feature);
                    }
                    if ("Accent Bar".equals(feature)) {
                        accentBar = !accentBar;
                    }
                    if ("Red Crosshair".equals(feature)) {
                        redCrosshair = !redCrosshair;
                    }
                }
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                close();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
