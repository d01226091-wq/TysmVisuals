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

/**
 * TysmVisuals is a cosmetic client visual pack.
 * It does not modify reach, movement, targeting, hitboxes, packets or combat logic.
 */
public class TysmVisualsClient implements ClientModInitializer {
    private static KeyBinding menuKey;
    private static float pulse;
    private static int themeIndex = 0;

    private static boolean hudEnabled = true;
    private static boolean crosshairEnabled = true;
    private static boolean accentBarEnabled = true;
    private static boolean ambientParticles = true;
    private static boolean vignetteEnabled = true;
    private static boolean hotbarGlow = true;
    private static boolean screenTint = false;

    private static final int[] THEMES = {
            0xFFFF2633, // Red
            0xFFFF4D6D, // Crimson
            0xFFFF7A18, // Fire
            0xFFB52BFF  // Magenta
    };

    private static final String[] THEME_NAMES = {"RED", "CRIMSON", "FIRE", "MAGENTA"};

    private static final int WHITE = 0xFFF2F2F2;
    private static final int MUTED = 0xFFB8B8C2;
    private static final int PANEL = 0xD90A0B10;
    private static final int ROW = 0x381B1C24;

    private static final String[][] CATEGORIES = {
            {"◉  Visual", "Crosshair", "Screen Glow", "Vignette", "Hotbar Glow", "Ambient Particles", "Color Theme", "HUD Branding", "Accent Bar", "Soft Tint"},
            {"◎  World", "Sky Tint", "Fog Tint", "Water Tint", "Night Accent", "Weather Overlay", "Biome Ambience", "Sunset Glow", "Moon Glow", "World Fade"},
            {"✦  Particles", "Ambient Dots", "Sparkles", "Soft Rings", "Trail Dots", "Landing Dust", "Screen Sparks", "Orbit Particles", "Glow Motes", "Particle Fade"},
            {"⚒  Interface", "HUD", "Hotbar Accent", "Item Highlight", "Status Cards", "Dynamic Island", "FPS Badge", "Ping Badge", "Clock Badge", "Clean UI"},
            {"●  Client", "Theme", "UI Scale", "Menu Blur", "Menu Animation", "Gui Sounds", "Red Edition", "Minimal Mode", "Reset Visuals", "About"}
    };

    private static final Map<String, Boolean> EXTRA = new HashMap<>();

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tysmvisuals.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.tysmvisuals"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> pulse += 0.055f);

        HudRenderCallback.EVENT.register((context, tickDelta) -> renderVisuals(context));
    }

    private static int accent() {
        return THEMES[themeIndex];
    }

    private static boolean extra(String name) {
        return EXTRA.getOrDefault(name, false);
    }

    private static void setExtra(String name, boolean value) {
        EXTRA.put(name, value);
    }

    private static void renderVisuals(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        int red = accent();

        if (screenTint) {
            ctx.fill(0, 0, w, h, (0x18 << 24) | (red & 0xFFFFFF));
        }

        if (vignetteEnabled) {
            drawVignette(ctx, w, h, red);
        }

        if (ambientParticles || extra("Ambient Dots") || extra("Sparkles") || extra("Glow Motes")) {
            drawAmbientParticles(ctx, w, h, red, client.player.age);
        }

        if (hudEnabled) {
            drawBrand(ctx, client);
        }

        if (crosshairEnabled) {
            drawCrosshair(ctx, w, h, red);
        }

        if (accentBarEnabled) {
            float a = (MathHelper.sin(pulse) + 1f) * 0.5f;
            int alpha = 0x30 + (int) (a * 0x30);
            ctx.fill(w / 2 - 90, h - 52, w / 2 + 90, h - 49,
                    (alpha << 24) | (red & 0xFFFFFF));
        }

        if (hotbarGlow) {
            drawHotbarAccent(ctx, w, h, red);
        }
    }

    private static void drawBrand(DrawContext ctx, MinecraftClient client) {
        int red = accent();
        ctx.fill(8, 8, 122, 36, 0xB0101016);
        ctx.fill(8, 8, 11, 36, red);
        ctx.drawText(client.textRenderer, Text.literal("TYSM"), 18, 13, red, true);
        ctx.drawText(client.textRenderer, Text.literal("VISUALS"), 58, 13, WHITE, true);
        ctx.drawText(client.textRenderer, Text.literal(THEME_NAMES[themeIndex]), 18, 27, MUTED, false);
    }

    private static void drawCrosshair(DrawContext ctx, int w, int h, int red) {
        int cx = w / 2;
        int cy = h / 2;
        ctx.fill(cx - 6, cy - 1, cx + 7, cy + 1, (0x55 << 24) | (red & 0xFFFFFF));
        ctx.fill(cx - 1, cy - 6, cx + 1, cy + 7, (0x55 << 24) | (red & 0xFFFFFF));
        ctx.fill(cx - 4, cy - 1, cx + 5, cy + 1, red);
        ctx.fill(cx - 1, cy - 4, cx + 1, cy + 5, red);
    }

    private static void drawHotbarAccent(DrawContext ctx, int w, int h, int red) {
        int y = h - 24;
        float a = (MathHelper.sin(pulse * 1.4f) + 1f) * 0.5f;
        int alpha = 0x20 + (int)(a * 0x25);
        ctx.fill(w / 2 - 91, y, w / 2 + 91, y + 2, (alpha << 24) | (red & 0xFFFFFF));
    }

    private static void drawAmbientParticles(DrawContext ctx, int w, int h, int red, int age) {
        for (int i = 0; i < 18; i++) {
            float phase = age * 0.018f + i * 1.73f;
            int x = (int)(w * (0.5f + 0.46f * MathHelper.sin(phase * 0.71f + i)));
            int y = (int)(h * (0.5f + 0.43f * MathHelper.cos(phase * 0.53f + i * 0.37f)));
            int size = 1 + (i % 2);
            int alpha = 35 + (i % 4) * 12;
            ctx.fill(x, y, x + size, y + size, (alpha << 24) | (red & 0xFFFFFF));
        }
    }

    private static void drawVignette(DrawContext ctx, int w, int h, int red) {
        int a = 20 + (int)((MathHelper.sin(pulse * 0.6f) + 1f) * 7f);
        ctx.fill(0, 0, w, 3, (a << 24) | (red & 0xFFFFFF));
        ctx.fill(0, h - 3, w, h, (a << 24) | (red & 0xFFFFFF));
        ctx.fill(0, 0, 3, h, (a << 24) | (red & 0xFFFFFF));
        ctx.fill(w - 3, 0, w, h, (a << 24) | (red & 0xFFFFFF));
    }

    private static class TysmMenuScreen extends Screen {
        private TysmMenuScreen() {
            super(Text.literal("TysmVisuals"));
        }

        @Override
        protected void init() {
            // Right Shift is registered globally; the menu itself uses direct mouse interaction.
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
            int rowGap = panelH < 425 ? 31 : 35;

            drawBrand(context, 28, 18);
            context.drawText(textRenderer, Text.literal("Fabric 1.21.4"), screenW - 118, 14, accent(), true);
            context.drawText(textRenderer, Text.literal("COSMETIC CLIENT"), screenW - 118, 29, MUTED, false);

            for (int i = 0; i < CATEGORIES.length; i++) {
                int x = left + i * (panelW + gap);
                drawPanel(context, x, top, panelW, panelH, CATEGORIES[i], mouseX, mouseY, rowGap);
            }

            if (side > 0) {
                int sx = left + 5 * (panelW + gap);
                drawInfo(context, sx, top + 68, side - 10, 270);
            }

            drawControls(context, screenW - 245, screenH - 72);
            super.render(context, mouseX, mouseY, delta);
        }

        private void drawBrand(DrawContext ctx, int x, int y) {
            int red = accent();
            ctx.fill(x, y + 3, x + 150, y + 5, red);
            ctx.drawText(textRenderer, Text.literal("TYSM"), x + 8, y + 9, WHITE, true);
            ctx.drawText(textRenderer, Text.literal("VISUALS"), x + 53, y + 9, red, true);
            ctx.drawText(textRenderer, Text.literal("COSMETIC  •  " + THEME_NAMES[themeIndex]), x + 9, y + 26, MUTED, false);
        }

        private void drawPanel(DrawContext ctx, int x, int y, int w, int h, String[] rows,
                               int mouseX, int mouseY, int rowGap) {
            int red = accent();
            ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x50000000);
            ctx.fill(x, y, x + w, y + h, PANEL);
            ctx.fill(x, y, x + w, y + 2, red);
            ctx.fill(x, y + 43, x + w, y + 45, (0x88 << 24) | (red & 0xFFFFFF));

            ctx.drawText(textRenderer, Text.literal(rows[0]), x + 14, y + 14, WHITE, true);
            ctx.drawText(textRenderer, Text.literal("⚙"), x + w - 19, y + 14, red, true);

            for (int i = 1; i < rows.length; i++) {
                int ry = y + 52 + (i - 1) * rowGap;
                boolean hover = mouseX >= x && mouseX <= x + w
                        && mouseY >= ry - 5 && mouseY < ry + rowGap;

                if (hover) {
                    ctx.fill(x + 5, ry - 5, x + w - 5, ry + rowGap - 2, ROW);
                    ctx.fill(x + 5, ry - 5, x + 8, ry + rowGap - 2, red);
                }

                String feature = rows[i];
                ctx.drawText(textRenderer, Text.literal(feature), x + 10, ry + 3,
                        hover ? WHITE : MUTED, false);

                boolean on = getFeatureState(feature);
                if (on) {
                    ctx.drawText(textRenderer, Text.literal("✓"), x + w - 19, ry + 3, red, true);
                } else {
                    ctx.drawText(textRenderer, Text.literal("○"), x + w - 19, ry + 3, 0xFF666670, false);
                }
            }
        }

        private boolean getFeatureState(String feature) {
            return switch (feature) {
                case "Crosshair" -> crosshairEnabled;
                case "Screen Glow", "Vignette" -> vignetteEnabled;
                case "Hotbar Glow", "Hotbar Accent" -> hotbarGlow;
                case "Ambient Particles" -> ambientParticles;
                case "Color Theme", "Theme", "Red Edition" -> true;
                case "HUD Branding", "HUD" -> hudEnabled;
                case "Accent Bar" -> accentBarEnabled;
                case "Soft Tint", "Sky Tint", "Fog Tint", "Water Tint", "Night Accent",
                     "Weather Overlay", "Biome Ambience", "Sunset Glow", "Moon Glow", "World Fade",
                     "Ambient Dots", "Sparkles", "Soft Rings", "Trail Dots", "Landing Dust",
                     "Screen Sparks", "Orbit Particles", "Glow Motes", "Particle Fade",
                     "Item Highlight", "Status Cards", "Dynamic Island", "FPS Badge", "Ping Badge",
                     "Clock Badge", "Clean UI", "UI Scale", "Menu Blur", "Menu Animation",
                     "Gui Sounds", "Minimal Mode", "About" -> extra(feature);
                case "Reset Visuals" -> false;
                default -> false;
            };
        }

        private void drawInfo(DrawContext ctx, int x, int y, int w, int h) {
            int red = accent();
            ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x50000000);
            ctx.fill(x, y, x + w, y + h, PANEL);
            ctx.fill(x, y, x + w, y + 2, red);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("TYSM VISUALS"), x + w / 2, y + 14, red);

            String[] lines = {
                    "COSMETIC ONLY",
                    "No reach",
                    "No aim assist",
                    "No hitbox changes",
                    "No movement changes",
                    "",
                    "Theme: " + THEME_NAMES[themeIndex],
                    "Particles: " + (ambientParticles ? "ON" : "OFF"),
                    "Crosshair: " + (crosshairEnabled ? "ON" : "OFF"),
                    "HUD: " + (hudEnabled ? "ON" : "OFF")
            };

            int yy = y + 42;
            for (String line : lines) {
                ctx.drawText(textRenderer, Text.literal(line), x + 12, yy,
                        line.equals("COSMETIC ONLY") ? red : MUTED, false);
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
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);

            int screenW = width;
            int gap = screenW >= 1100 ? 12 : 8;
            int side = screenW >= 1100 ? 215 : 0;
            int left = 24;
            int top = Math.max(56, height / 2 - 150);
            int available = screenW - left - 24 - side - gap * 4;
            int panelW = Math.max(120, Math.min(190, available / 5));
            int panelH = Math.min(440, height - top - 42);
            int rowGap = panelH < 425 ? 31 : 35;

            for (int i = 0; i < CATEGORIES.length; i++) {
                int x = left + i * (panelW + gap);
                for (int row = 1; row < CATEGORIES[i].length; row++) {
                    int ry = top + 52 + (row - 1) * rowGap;
                    if (mouseX >= x + 5 && mouseX <= x + panelW - 5
                            && mouseY >= ry - 5 && mouseY < ry + rowGap) {
                        handleClick(CATEGORIES[i][row]);
                        return true;
                    }
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        private void handleClick(String feature) {
            switch (feature) {
                case "Crosshair" -> crosshairEnabled = !crosshairEnabled;
                case "Screen Glow" -> screenTint = !screenTint;
                case "Vignette" -> vignetteEnabled = !vignetteEnabled;
                case "Hotbar Glow", "Hotbar Accent" -> hotbarGlow = !hotbarGlow;
                case "Ambient Particles" -> ambientParticles = !ambientParticles;
                case "Color Theme", "Theme", "Red Edition" ->
                        themeIndex = (themeIndex + 1) % THEMES.length;
                case "HUD Branding", "HUD" -> hudEnabled = !hudEnabled;
                case "Accent Bar" -> accentBarEnabled = !accentBarEnabled;
                case "Soft Tint" -> screenTint = !screenTint;
                case "Reset Visuals" -> resetVisuals();
                case "Menu Animation", "Menu Blur", "UI Scale", "Gui Sounds", "About" ->
                        setExtra(feature, !extra(feature));
                default -> setExtra(feature, !extra(feature));
            }
        }

        private void resetVisuals() {
            hudEnabled = true;
            crosshairEnabled = true;
            accentBarEnabled = true;
            ambientParticles = true;
            vignetteEnabled = true;
            hotbarGlow = true;
            screenTint = false;
            themeIndex = 0;
            EXTRA.clear();
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
