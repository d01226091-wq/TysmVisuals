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
        private int selected = 0;

        private static final String[] NAV = {
                "Главная", "Визуал", "Интерфейс", "Эффекты", "Косметика", "Настройки"
        };

        private static final String[][] FEATURE_GROUPS = {
                {"Мягкие кольца", "Орбитальные частицы", "Искры экрана", "Свечение", "Ambient Dots", "Glow Motes"},
                {"Crosshair", "Screen Glow", "Vignette", "Hotbar Glow", "Accent Bar", "Soft Tint"},
                {"HUD", "Hotbar Accent", "FPS Badge", "Ping Badge", "Clock Badge", "Dynamic Island"},
                {"Soft Rings", "Orbit Particles", "Screen Sparks", "Glow Motes", "Trail Dots", "Particle Fade"},
                {"Landing Dust", "Sparkles", "Biome Ambience", "Sunset Glow", "Moon Glow", "Weather Overlay"},
                {"Menu Blur", "Menu Animation", "Gui Sounds", "Minimal Mode", "Red Edition", "Reset Visuals"}
        };

        private TysmMenuScreen() {
            super(Text.literal("TysmVisuals"));
        }

        @Override
        protected void init() {
            openedAt = System.currentTimeMillis();
        }

        private float animation() {
            float t = (System.currentTimeMillis() - openedAt) / 300f;
            t = MathHelper.clamp(t, 0f, 1f);
            return 1f - (float)Math.pow(1f - t, 3);
        }

        private int alpha(int color, float amount) {
            int a = (color >>> 24) & 0xFF;
            a = MathHelper.clamp((int)(a * amount), 0, 255);
            return (a << 24) | (color & 0xFFFFFF);
        }

        private int slide(int target, float anim, int distance) {
            return target + (int)((1f - anim) * distance);
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            float a = animation();
            int red = accent();

            // Darken the world first, then bring the UI in smoothly.
            ctx.fill(0, 0, width, height, alpha(0xB0000000, a));
            drawBackgroundGlow(ctx, red, a);

            int menuW = Math.min(820, width - 64);
            int menuH = Math.min(500, height - 64);
            int x = (width - menuW) / 2;
            int y = (height - menuH) / 2;
            int sx = slide(x, a, 28);
            int sy = slide(y, a, 18);

            // Shadow and main glass panel.
            ctx.fill(sx + 6, sy + 8, sx + menuW + 6, sy + menuH + 8, alpha(0x70000000, a));
            ctx.fill(sx, sy, sx + menuW, sy + menuH, alpha(0xE9080B16, a));
            ctx.fill(sx, sy, sx + menuW, sy + 2, alpha(red, a));

            drawSidebar(ctx, sx, sy, menuW, menuH, mouseX, mouseY, a);
            drawMain(ctx, sx, sy, menuW, menuH, mouseX, mouseY, a);

            if (a < 1f) {
                // A second tiny pass makes the opening feel softer without changing gameplay.
                ctx.fill(0, 0, width, height, alpha(0x18000000, 1f - a));
            }
            super.render(ctx, mouseX, mouseY, delta);
        }

        private void drawBackgroundGlow(DrawContext ctx, int red, float a) {
            int cx = width / 2;
            int cy = height / 2;
            int glow = alpha((0x22 << 24) | (red & 0xFFFFFF), a);
            for (int i = 0; i < 5; i++) {
                int r = 100 + i * 75;
                ctx.fill(cx - r, cy - 1, cx + r, cy + 1, glow);
                ctx.fill(cx - 1, cy - r, cx + 1, cy + r, glow);
            }
        }

        private void drawSidebar(DrawContext ctx, int x, int y, int menuW, int menuH,
                                 int mouseX, int mouseY, float a) {
            int red = accent();
            int sideW = 154;

            ctx.fill(x, y, x + sideW, y + menuH, alpha(0xC30A0D16, a));
            ctx.fill(x + sideW - 1, y + 20, x + sideW, y + menuH - 20, alpha(0x442A2D38, a));

            ctx.drawText(textRenderer, Text.literal("✦"), x + 20, y + 25, alpha(red, a), true);
            ctx.drawText(textRenderer, Text.literal("Tysm"), x + 38, y + 20, alpha(WHITE, a), true);
            ctx.drawText(textRenderer, Text.literal("Visuals"), x + 38, y + 34, alpha(red, a), true);
            ctx.drawText(textRenderer, Text.literal("COSMETIC CLIENT"), x + 20, y + 58, alpha(MUTED, a), false);

            for (int i = 0; i < NAV.length; i++) {
                int ny = y + 94 + i * 49;
                boolean hover = mouseX >= x + 12 && mouseX <= x + sideW - 12
                        && mouseY >= ny && mouseY < ny + 38;

                if (selected == i) {
                    ctx.fill(x + 12, ny, x + sideW - 12, ny + 38, alpha(0x552A2DFF, a));
                    ctx.fill(x + 12, ny, x + 15, ny + 38, alpha(red, a));
                } else if (hover) {
                    ctx.fill(x + 12, ny, x + sideW - 12, ny + 38, alpha(0x301A1D28, a));
                }

                String icon = switch (i) {
                    case 0 -> "◆";
                    case 1 -> "✦";
                    case 2 -> "▣";
                    case 3 -> "◈";
                    case 4 -> "◇";
                    default -> "⚙";
                };
                ctx.drawText(textRenderer, Text.literal(icon), x + 24, ny + 11,
                        alpha(selected == i ? red : MUTED, a), true);
                ctx.drawText(textRenderer, Text.literal(NAV[i]), x + 43, ny + 11,
                        alpha(selected == i ? WHITE : MUTED, a), selected == i);
            }

            ctx.fill(x + 14, y + menuH - 68, x + sideW - 14, y + menuH - 18, alpha(0x401A1D28, a));
            ctx.drawText(textRenderer, Text.literal("TysmVisuals"), x + 26, y + menuH - 57, alpha(WHITE, a), true);
            ctx.drawText(textRenderer, Text.literal("Fabric 1.21.4"), x + 26, y + menuH - 40, alpha(MUTED, a), false);
        }

        private void drawMain(DrawContext ctx, int x, int y, int menuW, int menuH,
                              int mouseX, int mouseY, float a) {
            int red = accent();
            int contentX = x + 174;
            int contentW = menuW - 196;

            ctx.drawText(textRenderer, Text.literal(NAV[selected]), contentX, y + 25,
                    alpha(WHITE, a), true);
            ctx.drawText(textRenderer, Text.literal(
                    selected == 0 ? "Быстрый доступ к визуальным эффектам" :
                    "Настройка " + NAV[selected].toLowerCase()),
                    contentX, y + 42, alpha(MUTED, a), false);
            // Compact description area: explains the feature under the cursor.
            String hovered = getHoveredFeature(mouseX, mouseY, x, y, menuW, menuH);
            String description = hovered == null ? "Наведи на функцию, чтобы увидеть краткое описание." : featureDescription(hovered);
            ctx.fill(contentX, y + 58, x + menuW - 18, y + 84, alpha(0x501A1D28, a));
            ctx.drawText(textRenderer, Text.literal(description), contentX + 9, y + 67, alpha(MUTED, a), false);

            // Top performance cards.
            int cardY = y + 92;
            drawStat(ctx, contentX, cardY, 118, "FPS",
                    String.valueOf(MinecraftClient.getInstance().getCurrentFps()), red, a);
            drawStat(ctx, contentX + 128, cardY, 118, "PING",
                    getPing(), red, a);
            drawStat(ctx, contentX + 256, cardY, 118, "THEME",
                    THEME_NAMES[themeIndex], red, a);

            int gridY = y + 145;
            int gap = 12;
            int colW = (contentW - gap) / 2;
            String[] features = FEATURE_GROUPS[Math.min(selected, FEATURE_GROUPS.length - 1)];

            for (int i = 0; i < features.length; i++) {
                int col = i % 2;
                int row = i / 2;
                int cx = contentX + col * (colW + gap);
                int cy = gridY + row * 66;
                drawFeatureCard(ctx, cx, cy, colW, 56, features[i], mouseX, mouseY, red, a);
            }

            int footerY = y + menuH - 28;
            ctx.drawText(textRenderer, Text.literal("RIGHT SHIFT  •  CLOSE"), contentX, footerY,
                    alpha(MUTED, a), false);
            ctx.drawText(textRenderer, Text.literal("COSMETIC ONLY"), x + menuW - 142, footerY,
                    alpha(red, a), true);
        }

        private String getHoveredFeature(int mouseX, int mouseY, int x, int y, int menuW, int menuH) {
            int contentX = x + 174;
            int contentW = menuW - 196;
            int gap = 10;
            int colW = (contentW - gap) / 2;
            String[] features = FEATURE_GROUPS[Math.min(selected, FEATURE_GROUPS.length - 1)];
            for (int i = 0; i < features.length; i++) {
                int col = i % 2, row = i / 2;
                int cx = contentX + col * (colW + gap);
                int cy = y + 145 + row * 60;
                if (mouseX >= cx && mouseX <= cx + colW && mouseY >= cy && mouseY <= cy + 52) return features[i];
            }
            return null;
        }

        private String featureDescription(String feature) {
            return switch (feature) {
                case "Crosshair" -> "Меняет внешний вид прицела.";
                case "Screen Glow" -> "Добавляет мягкое свечение поверх экрана.";
                case "Vignette" -> "Затемняет края экрана для атмосферы.";
                case "Hotbar Glow", "Hotbar Accent" -> "Добавляет подсветку панели быстрого доступа.";
                case "Ambient Particles", "Ambient Dots" -> "Показывает лёгкие декоративные частицы.";
                case "Accent Bar" -> "Добавляет тонкую цветную линию в HUD.";
                case "Soft Tint" -> "Накладывает лёгкий цветовой оттенок.";
                case "HUD" -> "Включает декоративные элементы интерфейса.";
                case "FPS Badge" -> "Показывает FPS в небольшом бейдже.";
                case "Ping Badge" -> "Показывает задержку соединения.";
                case "Clock Badge" -> "Показывает игровое время в HUD.";
                case "Dynamic Island" -> "Показывает компактный декоративный индикатор.";
                case "Soft Rings", "Мягкие кольца" -> "Рисует плавные светящиеся кольца.";
                case "Orbit Particles", "Орбитальные частицы" -> "Добавляет частицы, вращающиеся вокруг центра.";
                case "Screen Sparks", "Искры экрана" -> "Добавляет короткие декоративные искры.";
                case "Glow Motes" -> "Добавляет мягкие светящиеся точки.";
                case "Trail Dots" -> "Создаёт декоративный след из точек.";
                case "Particle Fade" -> "Плавно затухают декоративные частицы.";
                case "Landing Dust" -> "Показывает декоративную пыль при приземлении.";
                case "Sparkles" -> "Добавляет маленькие блёстки.";
                case "Biome Ambience" -> "Добавляет декоративную атмосферу биома.";
                case "Sunset Glow" -> "Добавляет мягкое свечение заката.";
                case "Moon Glow" -> "Добавляет декоративное свечение луны.";
                case "Weather Overlay" -> "Добавляет визуальный слой погоды.";
                case "Menu Blur" -> "Добавляет эффект размытия меню.";
                case "Menu Animation" -> "Включает плавную анимацию меню.";
                case "Gui Sounds" -> "Использует декоративные звуки интерфейса.";
                case "Minimal Mode" -> "Убирает часть второстепенных элементов HUD.";
                case "Red Edition" -> "Переключает цветовую тему клиента.";
                case "Reset Visuals" -> "Возвращает визуальные настройки по умолчанию.";
                default -> "Косметическая настройка внешнего вида клиента.";
            };
        }

        private String getPing() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.getNetworkHandler() == null) return "--";
            var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            return entry == null ? "--" : entry.getLatency() + "ms";
        }

        private void drawStat(DrawContext ctx, int x, int y, int w, String label,
                              String value, int red, float a) {
            ctx.fill(x + 2, y + 3, x + w + 2, y + 43, alpha(0x40000000, a));
            ctx.fill(x, y, x + w, y + 40, alpha(0xA80E111A, a));
            ctx.fill(x, y, x + 3, y + 40, alpha(red, a));
            ctx.drawText(textRenderer, Text.literal(label), x + 11, y + 7, alpha(MUTED, a), false);
            ctx.drawText(textRenderer, Text.literal(value), x + 11, y + 20, alpha(WHITE, a), true);
        }

        private void drawFeatureCard(DrawContext ctx, int x, int y, int w, int h,
                                     String feature, int mouseX, int mouseY, int red, float a) {
            boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            boolean on = getFeatureState(feature);

            int bg = hover ? 0xA51A1D29 : 0x8D0E111A;
            ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, alpha(0x50000000, a));
            ctx.fill(x, y, x + w, y + h, alpha(bg, a));
            ctx.fill(x, y, x + 3, y + h, alpha(on ? red : 0x30343F, a));

            ctx.drawText(textRenderer, Text.literal(feature), x + 13, y + 11,
                    alpha(on ? WHITE : MUTED, a), on);
            ctx.drawText(textRenderer, Text.literal(on ? "ВКЛ" : "ВЫКЛ"), x + 13, y + 29,
                    alpha(on ? red : 0x777984, a), false);

            int tx = x + w - 48;
            int ty = y + 17;
            ctx.fill(tx, ty, tx + 34, ty + 18, alpha(on ? red : 0x383B46, a));
            ctx.fill(tx + (on ? 18 : 3), ty + 3, tx + (on ? 31 : 16), ty + 15,
                    alpha(0xFFF2F2F2, a));
        }

        private boolean getFeatureState(String feature) {
            return switch (feature) {
                case "Crosshair" -> crosshairEnabled;
                case "Screen Glow", "Soft Tint" -> screenTint;
                case "Vignette" -> vignetteEnabled;
                case "Hotbar Glow", "Hotbar Accent" -> hotbarGlow;
                case "Ambient Particles" -> ambientParticles;
                case "Accent Bar" -> accentBarEnabled;
                case "HUD" -> hudEnabled;
                case "Theme", "Red Edition" -> true;
                case "FPS Badge", "Ping Badge", "Clock Badge", "Dynamic Island",
                     "Soft Rings", "Orbit Particles", "Screen Sparks", "Glow Motes" -> extra(feature);
                default -> extra(feature);
            };
        }

        private void handleFeature(String feature) {
            switch (feature) {
                case "Crosshair" -> crosshairEnabled = !crosshairEnabled;
                case "Screen Glow", "Soft Tint" -> screenTint = !screenTint;
                case "Vignette" -> vignetteEnabled = !vignetteEnabled;
                case "Hotbar Glow", "Hotbar Accent" -> hotbarGlow = !hotbarGlow;
                case "Ambient Particles" -> ambientParticles = !ambientParticles;
                case "Accent Bar" -> accentBarEnabled = !accentBarEnabled;
                case "HUD" -> hudEnabled = !hudEnabled;
                case "Theme", "Red Edition" -> themeIndex = (themeIndex + 1) % THEMES.length;
                case "Reset Visuals" -> resetVisuals();
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
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return super.mouseClicked(mouseX, mouseY, button);
            }

            int menuW = Math.min(820, width - 64);
            int menuH = Math.min(500, height - 64);
            int x = (width - menuW) / 2;
            int y = (height - menuH) / 2;
            int sideW = 190;

            for (int i = 0; i < NAV.length; i++) {
                int ny = y + 94 + i * 49;
                if (mouseX >= x + 12 && mouseX <= x + sideW - 12
                        && mouseY >= ny && mouseY < ny + 38) {
                    selected = i;
                    return true;
                }
            }

            int contentX = x + 174;
            int contentW = menuW - 196;
            int gap = 12;
            int colW = (contentW - gap) / 2;
            String[] features = FEATURE_GROUPS[Math.min(selected, FEATURE_GROUPS.length - 1)];

            for (int i = 0; i < features.length; i++) {
                int col = i % 2;
                int row = i / 2;
                int cx = contentX + col * (colW + gap);
                int cy = y + 125 + row * 66;
                if (mouseX >= cx && mouseX <= cx + colW && mouseY >= cy && mouseY <= cy + 56) {
                    handleFeature(features[i]);
                    return true;
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
