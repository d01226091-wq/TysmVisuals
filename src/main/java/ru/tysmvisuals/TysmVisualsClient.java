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
    private static long visualTime;

    private static boolean hudEnabled = true;
    private static boolean crosshairEnabled = true;
    private static boolean accentBarEnabled = true;
    private static boolean ambientParticles = true;
    private static boolean vignetteEnabled = true;
    private static boolean hotbarGlow = true;
    private static boolean screenTint = false;
    private static boolean skyColorEnabled = true;
    private static int skyColorIndex = 0;
    private static final int[] SKY_COLORS = {0xFF4D7CFF,0xFFB52BFF,0xFFFF4D6D,0xFFFF8A3D,0xFF35D6A5,0xFF20C8FF,0xFF6D5CFF,0xFFE8E8F2};
    private static final String[] SKY_COLOR_NAMES = {"BLUE","PURPLE","CRIMSON","SUNSET","MINT","CYAN","VIOLET","LIGHT"};

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pulse += 0.055f;
            visualTime++;

            // Open the cosmetic menu with Right Shift when no other screen is open.
            if (menuKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new TysmMenuScreen());
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> renderVisuals(context));
    }

    private static int accent() {
        return THEMES[themeIndex];
    }
    public static boolean isSkyColorEnabled() {
        return skyColorEnabled;
    }

    public static int getSkyColor() {
        return SKY_COLORS[skyColorIndex] & 0xFFFFFF;
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

        if (ambientParticles || extra("Ambient Dots") || extra("Sparkles") || extra("Glow Motes")
                || extra("Trail Dots") || extra("Particle Fade")) {
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

        // Pulse-inspired cosmetic effects: soft rings, orbit motes and compact status cards.
        if (extra("Soft Rings") || extra("Screen Sparks")) {
            drawPulseRings(ctx, w, h, red);
        }
        if (extra("Orbit Particles") || extra("Glow Motes")) {
            drawOrbitMotes(ctx, w, h, red);
        }
        if (extra("FPS Badge") || extra("Ping Badge") || extra("Clock Badge")) {
            drawStatusCards(ctx, client, w, h, red);
        }
        if (extra("Dynamic Island")) {
            drawDynamicIsland(ctx, w, red);
        }
    }

    private static void drawPulseRings(DrawContext ctx, int w, int h, int red) {
        float t = visualTime * 0.035f;
        int cx = w / 2;
        int cy = h / 2;
        for (int i = 0; i < 3; i++) {
            float phase = (t + i * 2.1f) % 5.0f;
            int radius = 10 + (int)(phase * 12f);
            int alpha = Math.max(8, 38 - (int)(phase * 6f));
            int c = (alpha << 24) | (red & 0xFFFFFF);
            ctx.fill(cx - radius, cy - radius, cx + radius, cy - radius + 1, c);
            ctx.fill(cx - radius, cy + radius - 1, cx + radius, cy + radius, c);
            ctx.fill(cx - radius, cy - radius, cx - radius + 1, cy + radius, c);
            ctx.fill(cx + radius - 1, cy - radius, cx + radius, cy + radius, c);
        }
    }

    private static void drawOrbitMotes(DrawContext ctx, int w, int h, int red) {
        float t = visualTime * 0.022f;
        float cx = w * 0.5f;
        float cy = h * 0.52f;
        for (int i = 0; i < 12; i++) {
            float a = t + i * 0.5236f;
            float r = 24f + (i % 3) * 11f;
            int x = (int)(cx + MathHelper.cos(a) * r);
            int y = (int)(cy + MathHelper.sin(a * 1.25f) * r * 0.65f);
            int alpha = 28 + (i % 4) * 12;
            int size = 1 + (i % 2);
            ctx.fill(x, y, x + size, y + size, (alpha << 24) | (red & 0xFFFFFF));
        }
    }

    private static void drawStatusCards(DrawContext ctx, MinecraftClient client, int w, int h, int red) {
        int x = 8;
        int y = h - 70;
        int cardW = 68;
        int index = 0;

        if (extra("FPS Badge")) {
            drawStatusCard(ctx, client, x + index++ * (cardW + 5), y, cardW,
                    "FPS", String.valueOf(client.getCurrentFps()), red);
        }
        if (extra("Ping Badge") && client.player != null && client.getNetworkHandler() != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            String ping = entry == null ? "--" : String.valueOf(entry.getLatency());
            drawStatusCard(ctx, client, x + index++ * (cardW + 5), y, cardW, "PING", ping, red);
        }
        if (extra("Clock Badge")) {
            java.time.LocalTime now = java.time.LocalTime.now();
            String time = String.format("%02d:%02d", now.getHour(), now.getMinute());
            drawStatusCard(ctx, client, x + index * (cardW + 5), y, cardW, "TIME", time, red);
        }
    }

    private static void drawStatusCard(DrawContext ctx, MinecraftClient client, int x, int y, int w,
                                       String label, String value, int red) {
        ctx.fill(x + 2, y + 2, x + w + 2, y + 25, 0x40000000);
        ctx.fill(x, y, x + w, y + 23, 0xB00A0B10);
        ctx.fill(x, y, x + 2, y + 23, red);
        ctx.drawText(client.textRenderer, Text.literal(label), x + 7, y + 4, MUTED, false);
        ctx.drawText(client.textRenderer, Text.literal(value), x + 7, y + 13, WHITE, true);
    }

    private static void drawDynamicIsland(DrawContext ctx, int w, int red) {
        int width = 150;
        int x = w / 2 - width / 2;
        int y = 8;
        float wave = (MathHelper.sin(pulse * 0.8f) + 1f) * 0.5f;
        int alpha = 0xB0 + (int)(wave * 0x20);
        ctx.fill(x + 3, y + 3, x + width + 3, y + 28, 0x50000000);
        ctx.fill(x, y, x + width, y + 25, (alpha << 24) | 0x08090D);
        ctx.fill(x, y, x + 3, y + 25, red);
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                Text.literal("TYSM  •  VISUALS"), x + width / 2, y + 8, red);
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
        private long openedAt;
        private int selectedTab = 0;
        private int selectedVisual = -1;

        private static final String[] TABS = {
                "Главное", "Визуалы", "Утилиты", "Косметика", "Настройки"
        };

        private TysmMenuScreen() {
            super(Text.literal("TysmVisuals"));
        }

        @Override
        protected void init() {
            openedAt = System.currentTimeMillis();
        }

        private float animation() {
            float t = (System.currentTimeMillis() - openedAt) / 220f;
            t = MathHelper.clamp(t, 0f, 1f);
            return 1f - (float)Math.pow(1f - t, 3);
        }

        private int alpha(int color, float amount) {
            int a = (color >>> 24) & 0xFF;
            a = MathHelper.clamp((int)(a * amount), 0, 255);
            return (a << 24) | (color & 0xFFFFFF);
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            float a = animation();
            int red = accent();

            ctx.fill(0, 0, width, height, alpha(0x90000000, a));

            int menuW = Math.min(430, width - 32);
            int menuH = Math.min(230, height - 32);
            int x = (width - menuW) / 2;
            int y = (height - menuH) / 2;
            int sx = x + (int)((1f - a) * 18f);
            int sy = y + (int)((1f - a) * 10f);

            // Main glass panel.
            ctx.fill(sx + 5, sy + 6, sx + menuW + 5, sy + menuH + 6, alpha(0x65000000, a));
            ctx.fill(sx, sy, sx + menuW, sy + menuH, alpha(0xEC080A11, a));

            // Sidebar.
            int sideW = 112;
            ctx.fill(sx, sy, sx + sideW, sy + menuH, alpha(0xE20B0C14, a));
            ctx.fill(sx + sideW, sy, sx + sideW + 1, sy + menuH, alpha(0x382A2D38, a));
            ctx.fill(sx, sy, sx + menuW, sy + 2, alpha(red, a));

            ctx.drawText(textRenderer, Text.literal("TYSM"), sx + 16, sy + 16,
                    alpha(red, a), true);
            ctx.drawText(textRenderer, Text.literal("VISUALS"), sx + 16, sy + 29,
                    alpha(WHITE, a), true);

            for (int i = 0; i < TABS.length; i++) {
                int rowY = sy + 55 + i * 29;
                boolean selected = selectedTab == i;

                if (selected) {
                    ctx.fill(sx + 8, rowY - 4, sx + sideW - 8, rowY + 19,
                            alpha(0x302A2D38, a));
                    ctx.fill(sx + 8, rowY - 4, sx + 10, rowY + 19,
                            alpha(red, a));
                }

                ctx.drawText(textRenderer, Text.literal(TABS[i]), sx + 18, rowY + 2,
                        alpha(selected ? WHITE : MUTED, a), selected);
            }

            int contentX = sx + sideW + 22;
            int contentW = menuW - sideW - 38;

            String title = TABS[selectedTab];
            ctx.drawText(textRenderer, Text.literal(title), contentX, sy + 20,
                    alpha(WHITE, a), true);
            ctx.drawText(textRenderer, Text.literal(tabSubtitle(selectedTab)),
                    contentX, sy + 36, alpha(MUTED, a), false);

            ctx.fill(contentX, sy + 58, contentX + contentW, sy + 59,
                    alpha(0x402A2D38, a));

            // Clean category preview, without toggles or gameplay features.
            if (selectedTab == 1) drawVisualsCard(ctx, contentX, sy + 70, contentW, red, a);
            else drawInfoCard(ctx, contentX, sy + 75, contentW, red, a, tabCardTitle(selectedTab), tabCardText(selectedTab));

            ctx.drawText(textRenderer, Text.literal("RIGHT SHIFT / ESC — CLOSE"),
                    contentX, sy + menuH - 25, alpha(MUTED, a), false);
        }

        private String tabSubtitle(int tab) {
            return switch (tab) {
                case 0 -> "Главная панель TysmVisuals";
                case 1 -> "Визуальные эффекты клиента";
                case 2 -> "Полезные визуальные инструменты";
                case 3 -> "Косметические элементы";
                default -> "Оформление и параметры интерфейса";
            };
        }

        private String tabCardTitle(int tab) {
            return switch (tab) {
                case 0 -> "Добро пожаловать";
                case 1 -> "Visual Effects";
                case 2 -> "Utilities";
                case 3 -> "Cosmetics";
                default -> "Client Settings";
            };
        }

        private String tabCardText(int tab) {
            return switch (tab) {
                case 0 -> "TysmVisuals • 1.21.4 • Fabric";
                case 1 -> "Чистые эффекты без изменения игрового процесса";
                case 2 -> "Компактные элементы для визуального интерфейса";
                case 3 -> "Стиль, тема и косметическое оформление";
                default -> "Минималистичный интерфейс и анимация";
            };
        }

        private void drawInfoCard(DrawContext ctx, int x, int y, int w, int red,
                                  float a, String title, String text) {
            ctx.fill(x + 3, y + 4, x + w + 3, y + 65, alpha(0x45000000, a));
            ctx.fill(x, y, x + w, y + 61, alpha(0xB5101119, a));
            ctx.fill(x, y, x + 3, y + 61, alpha(red, a));
            ctx.drawText(textRenderer, Text.literal(title), x + 13, y + 12,
                    alpha(WHITE, a), true);
            ctx.drawText(textRenderer, Text.literal(text), x + 13, y + 31,
                    alpha(MUTED, a), false);
        }

        private void drawVisualsCard(DrawContext ctx, int x, int y, int w, int red, float a) {
            String[] visuals = {"Sky Color", "Crosshair", "Vignette", "Hotbar Glow"};
            ctx.fill(x + 3, y + 4, x + w + 3, y + 142, alpha(0x45000000, a));
            ctx.fill(x, y, x + w, y + 138, alpha(0xB5101119, a));
            ctx.fill(x, y, x + 3, y + 138, alpha(red, a));

            ctx.drawText(textRenderer, Text.literal("Визуалы"), x + 13, y + 10, alpha(WHITE, a), true);
            ctx.drawText(textRenderer, Text.literal("ЛКМ по визуалу — открыть его настройки"), x + 13, y + 25, alpha(MUTED, a), false);

            for (int i = 0; i < visuals.length; i++) {
                int rowY = y + 43 + i * 22;
                boolean selected = selectedVisual == i;
                ctx.fill(x + 10, rowY - 3, x + w - 10, rowY + 16,
                        alpha(selected ? 0x382A2D38 : 0x181A1F28, a));
                if (selected) {
                    ctx.fill(x + 10, rowY - 3, x + 12, rowY + 16, alpha(red, a));
                }
                ctx.drawText(textRenderer, Text.literal(visuals[i]), x + 18, rowY + 2,
                        alpha(selected ? WHITE : MUTED, a), selected);
            }

            if (selectedVisual >= 0) {
                String setting = switch (selectedVisual) {
                    case 0 -> "Настройки Sky Color";
                    case 1 -> "Настройки Crosshair";
                    case 2 -> "Настройки Vignette";
                    default -> "Настройки Hotbar Glow";
                };
                ctx.drawText(textRenderer, Text.literal(setting), x + 13, y + 132,
                        alpha(red, a), true);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                int menuW = Math.min(430, width - 32);
                int menuH = Math.min(230, height - 32);
                int x = (width - menuW) / 2;
                int y = (height - menuH) / 2;
                int sideW = 112;

                if (mouseX >= x && mouseX <= x + sideW &&
                        mouseY >= y + 51 && mouseY <= y + 55 + TABS.length * 29) {
                    int index = (int)((mouseY - (y + 51)) / 29);
                    if (index >= 0 && index < TABS.length) {
                        selectedTab = index;
                        selectedVisual = -1;
                        return true;
                    }
                }

                if (selectedTab == 1) {
                    int contentX = x + sideW + 22;
                    int cardY = y + 70;
                    int w = menuW - sideW - 38;
                    for (int i = 0; i < 4; i++) {
                        int rowY = cardY + 43 + i * 22;
                        if (mouseX >= contentX + 10 && mouseX <= contentX + w - 10 &&
                                mouseY >= rowY - 3 && mouseY <= rowY + 16) {
                            selectedVisual = i;
                            return true;
                        }
                    }
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
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
