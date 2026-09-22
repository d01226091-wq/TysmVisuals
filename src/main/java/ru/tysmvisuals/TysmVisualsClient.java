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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
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

    private static boolean hudEnabled = false;
    private static boolean crosshairEnabled = false;
    private static boolean accentBarEnabled = false;
    private static boolean ambientParticles = false;
    private static boolean vignetteEnabled = false;
    private static boolean hotbarGlow = false;
    private static boolean screenTint = false;
    // Safe PvP HUD visuals: informational only, no aim/attack/movement automation.
    private static boolean keystrokesHud = false;
    private static boolean statsHud = false;
    private static boolean coordinatesHud = false;
    private static boolean movementHud = false;
    private static boolean targetHud = false;
    private static boolean armorHud = false;
    private static boolean itemHud = false;
    private static boolean skyColorEnabled = false;
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

            // Replace the vanilla title screen with the TysmVisuals main screen.
            if (client.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen
                    && !(client.currentScreen instanceof TysmMainMenuScreen)) {
                client.setScreen(new TysmMainMenuScreen());
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

        drawPvPHud(ctx, client, w, h, red);

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
        if (extra("Sunset Glow") || extra("Moon Glow") || extra("Night Accent")) {
            drawWorldAccent(ctx, w, h, red);
        }
        if (extra("Fog Tint") || extra("World Fade")) {
            drawWorldFade(ctx, w, h, red);
        }
        if (extra("Water Tint")) {
            drawWaterStyle(ctx, w, h, red);
        }
        if (extra("Status Cards")) {
            drawStatusCards(ctx, client, w, h, red);
        }
        if (extra("Hotbar Accent")) {
            drawHotbarAccent(ctx, w, h, red);
        }
    }

    private static void drawPvPHud(DrawContext ctx, MinecraftClient client, int w, int h, int red) {
        if (client.player == null) return;

        int x = 8;
        int y = 48;

        if (keystrokesHud) {
            drawKeyBox(ctx, client, x, y, red);
            y += 54;
        }

        if (statsHud) {
            int fps = client.getCurrentFps();
            String ping = "--";
            if (client.getNetworkHandler() != null) {
                var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) ping = String.valueOf(entry.getLatency());
            }
            drawInfoBox(ctx, client, x, y, 112, 38, red,
                    "FPS " + fps + "   PING " + ping,
                    client.player.getHealth() + "/" + client.player.getMaxHealth() + " HP");
            y += 44;
        }

        if (coordinatesHud) {
            String coords = String.format("XYZ %.0f %.0f %.0f",
                    client.player.getX(), client.player.getY(), client.player.getZ());
            String direction = directionName(client.player.getYaw());
            drawInfoBox(ctx, client, x, y, 150, 38, red, coords, "DIR " + direction);
            y += 44;
        }

        if (movementHud) {
            double speed = Math.sqrt(
                    client.player.getVelocity().x * client.player.getVelocity().x +
                    client.player.getVelocity().z * client.player.getVelocity().z) * 20.0;
            String state = client.player.isSprinting() ? "SPRINT" :
                    (client.player.isSneaking() ? "SNEAK" :
                    (client.player.isOnGround() ? "GROUND" : "AIR"));
            drawInfoBox(ctx, client, x, y, 150, 38, red,
                    String.format("SPEED %.2f m/s", speed), state);
            y += 44;
        }

        if (targetHud && client.crosshairTarget != null &&
                client.targetedEntity instanceof LivingEntity living && living != client.player) {
            double distance = client.player.distanceTo(living);
            String name = living.getDisplayName().getString();
            if (name.length() > 18) name = name.substring(0, 18);
            drawInfoBox(ctx, client, x, y, 180, 48, red,
                    name + "  " + String.format("%.1fm", distance),
                    String.format("HP %.1f / %.1f", living.getHealth(), living.getMaxHealth()));
            y += 54;
        }

        if (armorHud) {
            int armorX = w - 120;
            int armorY = 48;
            ctx.fill(armorX, armorY, armorX + 112, armorY + 82, 0xB50A0B10);
            ctx.fill(armorX, armorY, armorX + 3, armorY + 82, red);
            ctx.drawText(client.textRenderer, Text.literal("ARMOR"), armorX + 10, armorY + 7, red, true);
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < slots.length; i++) {
                var stack = client.player.getEquippedStack(slots[i]);
                String value = stack.isEmpty() ? "--" :
                        (stack.isDamageable() ? String.valueOf(stack.getMaxDamage() - stack.getDamage()) : "∞");
                ctx.drawText(client.textRenderer, Text.literal(value), armorX + 10, armorY + 22 + i * 13, MUTED, false);
            }
        }

        if (itemHud) {
            var stack = client.player.getMainHandStack();
            String item = stack.isEmpty() ? "EMPTY HAND" : stack.getName().getString();
            if (item.length() > 17) item = item.substring(0, 17);
            String durability = stack.isDamageable()
                    ? String.valueOf(stack.getMaxDamage() - stack.getDamage()) : "∞";
            drawInfoBox(ctx, client, w - 120, h - 92, 112, 48, red, item, "DUR " + durability);
        }
    }

    private static void drawKeyBox(DrawContext ctx, MinecraftClient client, int x, int y, int red) {
        ctx.fill(x, y, x + 112, y + 48, 0xB50A0B10);
        ctx.fill(x, y, x + 3, y + 48, red);
        ctx.drawText(client.textRenderer, Text.literal("KEYSTROKES"), x + 9, y + 5, red, true);

        String[] keys = {"W", "A", "S", "D"};
        KeyBinding[] binds = {
                client.options.forwardKey, client.options.leftKey,
                client.options.backKey, client.options.rightKey
        };
        for (int i = 0; i < 4; i++) {
            int bx = x + 8 + i * 24;
            int c = binds[i].isPressed() ? red : 0x501B1C24;
            ctx.fill(bx, y + 20, bx + 20, y + 39, c);
            ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal(keys[i]), bx + 10, y + 26,
                    binds[i].isPressed() ? WHITE : MUTED);
        }
    }

    private static void drawInfoBox(DrawContext ctx, MinecraftClient client, int x, int y, int w, int h,
                                    int red, String title, String value) {
        ctx.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x40000000);
        ctx.fill(x, y, x + w, y + h, 0xB50A0B10);
        ctx.fill(x, y, x + 3, y + h, red);
        ctx.drawText(client.textRenderer, Text.literal(title), x + 9, y + 7, WHITE, true);
        ctx.drawText(client.textRenderer, Text.literal(value), x + 9, y + 22, MUTED, false);
    }

    private static String directionName(float yaw) {
        float a = MathHelper.wrapDegrees(yaw);
        if (a >= -22.5f && a < 22.5f) return "S";
        if (a >= 22.5f && a < 67.5f) return "SW";
        if (a >= 67.5f && a < 112.5f) return "W";
        if (a >= 112.5f && a < 157.5f) return "NW";
        if (a >= 157.5f || a < -157.5f) return "N";
        if (a >= -157.5f && a < -112.5f) return "NE";
        if (a >= -112.5f && a < -67.5f) return "E";
        return "SE";
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

    private static void drawWorldAccent(DrawContext ctx, int w, int h, int red) {
        float wave = (MathHelper.sin(pulse * 0.45f) + 1f) * 0.5f;
        int alpha = 10 + (int)(wave * 12f);
        if (extra("Sunset Glow")) {
            ctx.fill(0, h / 2, w, h, (alpha << 24) | (red & 0xFFFFFF));
        }
        if (extra("Moon Glow")) {
            int cx = w - 42;
            int cy = 42;
            ctx.fill(cx - 10, cy - 10, cx + 10, cy + 10, ((alpha + 8) << 24) | (red & 0xFFFFFF));
            ctx.fill(cx - 6, cy - 10, cx + 10, cy + 6, 0xB0000000);
        }
        if (extra("Night Accent")) {
            ctx.fill(0, 0, w, 2, ((alpha + 5) << 24) | (red & 0xFFFFFF));
        }
    }

    private static void drawWorldFade(DrawContext ctx, int w, int h, int red) {
        int alpha = extra("Fog Tint") ? 9 : 5;
        ctx.fill(0, h / 3, w, h * 2 / 3, (alpha << 24) | (red & 0xFFFFFF));
        if (extra("World Fade")) {
            ctx.fill(0, 0, w, 1, ((alpha + 8) << 24) | (red & 0xFFFFFF));
            ctx.fill(0, h - 2, w, h, ((alpha + 8) << 24) | (red & 0xFFFFFF));
        }
    }

    private static void drawWaterStyle(DrawContext ctx, int w, int h, int red) {
        int y = h / 2;
        int alpha = 7 + (int)((MathHelper.sin(pulse * 1.2f) + 1f) * 3f);
        ctx.fill(0, y, w, y + 1, (alpha << 24) | (red & 0xFFFFFF));
        ctx.fill(0, y + 2, w, y + 3, ((alpha / 2) << 24) | (red & 0xFFFFFF));
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
        private int selectedVisual = -1; private int visualScroll = 0;

        private static final String[] TABS = {
                "Главное", "Визуалы", "Утилиты", "Косметика", "Настройки"
        };

        private static final String[] VISUALS = {
                "Keystrokes", "FPS / Ping", "Coordinates", "Movement HUD",
                "Target HUD", "Armor HUD", "Item HUD", "Crosshair",
                "Particles", "Vignette", "Hotbar Glow", "HUD Branding",
                "Ambient Dots", "Sparkles", "Soft Rings", "Orbit Particles",
                "Glow Motes", "Screen Sparks", "FPS Badge", "Ping Badge",
                "Clock Badge", "Dynamic Island", "Sky Tint", "Fog Tint",
                "Water Tint", "Night Accent", "Sunset Glow", "Moon Glow",
                "World Fade", "Status Cards", "Hotbar Accent", "Clean UI"
        };

        private TysmMenuScreen() {
            super(Text.literal("TysmVisuals"));
        }

        @Override
        protected void init() {
            openedAt = System.currentTimeMillis();
        }

        private float animation() {
            float t = (System.currentTimeMillis() - openedAt) / 180f;
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

            ctx.fill(0, 0, width, height, alpha(0x78000000, a));

            // Small Pulse-inspired compact window.
            int menuW = Math.min(370, width - 28);
            int menuH = Math.min(218, height - 28);
            int x = (width - menuW) / 2;
            int y = (height - menuH) / 2;
            int sx = x + (int)((1f - a) * 12f);
            int sy = y + (int)((1f - a) * 7f);

            ctx.fill(sx + 4, sy + 5, sx + menuW + 4, sy + menuH + 5, alpha(0x55000000, a));
            ctx.fill(sx, sy, sx + menuW, sy + menuH, alpha(0xF0080910, a));
            ctx.fill(sx, sy, sx + menuW, sy + 2, alpha(red, a));

            int sideW = 94;
            ctx.fill(sx, sy, sx + sideW, sy + menuH, alpha(0xE90B0C13, a));
            ctx.fill(sx + sideW, sy, sx + sideW + 1, sy + menuH, alpha(0x402A2D38, a));

            ctx.drawText(textRenderer, Text.literal("TYSM"), sx + 13, sy + 12, alpha(red, a), true);
            ctx.drawText(textRenderer, Text.literal("VISUALS"), sx + 13, sy + 25, alpha(WHITE, a), true);

            for (int i = 0; i < TABS.length; i++) {
                int rowY = sy + 47 + i * 28;
                boolean selected = selectedTab == i;
                if (selected) {
                    ctx.fill(sx + 7, rowY - 3, sx + sideW - 7, rowY + 18, alpha(0x352A2D38, a));
                    ctx.fill(sx + 7, rowY - 3, sx + 9, rowY + 18, alpha(red, a));
                }
                ctx.drawText(textRenderer, Text.literal(TABS[i]), sx + 15, rowY + 2,
                        alpha(selected ? WHITE : MUTED, a), selected);
            }

            int contentX = sx + sideW + 15;
            int contentW = menuW - sideW - 25;

            if (selectedTab == 1) {
                drawVisualPanel(ctx, contentX, sy + 14, contentW, red, a);
            } else {
                ctx.drawText(textRenderer, Text.literal(TABS[selectedTab]), contentX, sy + 16,
                        alpha(WHITE, a), true);
                ctx.drawText(textRenderer, Text.literal(tabSubtitle(selectedTab)),
                        contentX, sy + 32, alpha(MUTED, a), false);
                drawSimpleCard(ctx, contentX, sy + 52, contentW, red, a,
                        tabCardTitle(selectedTab), tabCardText(selectedTab));
            }

            ctx.drawText(textRenderer, Text.literal("RSHIFT / ESC"), contentX,
                    sy + menuH - 17, alpha(MUTED, a), false);
        }

        private void drawVisualPanel(DrawContext ctx, int x, int y, int w, int red, float a) {
            ctx.drawText(textRenderer, Text.literal("Визуалы"), x, y + 2, alpha(WHITE, a), true);

            if (selectedVisual < 0) {
                ctx.drawText(textRenderer, Text.literal("ЛКМ — открыть • колесо — прокрутка"),
                        x, y + 18, alpha(MUTED, a), false);

                int rowH = 25;
                int visibleRows = 6;
                int listY = y + 35;
                int colW = (w - 5) / 2;

                for (int i = 0; i < VISUALS.length; i++) {
                    int row = i / 2;
                    int col = i % 2;
                    int visibleRow = row - visualScroll;
                    if (visibleRow < 0 || visibleRow >= visibleRows) continue;

                    int rowY = listY + visibleRow * rowH;
                    int colX = x + col * (colW + 5);
                    boolean on = visualEnabled(i);

                    ctx.fill(colX, rowY, colX + colW, rowY + 21,
                            alpha(on ? 0x302A2D38 : 0x181A1F26, a));
                    if (on) ctx.fill(colX, rowY, colX + 2, rowY + 21, alpha(red, a));

                    String label = VISUALS[i];
                    ctx.drawText(textRenderer, Text.literal(label),
                            colX + 7, rowY + 5, alpha(on ? WHITE : MUTED, a), on);
                    ctx.drawText(textRenderer, Text.literal(on ? "ON" : "OFF"),
                            colX + colW - 23, rowY + 5, alpha(on ? red : MUTED, a), false);
                }

                int maxScroll = Math.max(0, ((VISUALS.length + 1) / 2) - visibleRows);
                if (maxScroll > 0) {
                    int trackX = x + w - 2;
                    int trackY = listY;
                    int trackH = visibleRows * rowH - 4;
                    int thumbH = Math.max(12, trackH * visibleRows / (visibleRows + maxScroll));
                    int thumbY = trackY + (trackH - thumbH) * visualScroll / maxScroll;
                    ctx.fill(trackX, trackY, trackX + 2, trackY + trackH, alpha(0x30343A45, a));
                    ctx.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, alpha(red, a));
                }
            } else {
                String name = VISUALS[selectedVisual];
                boolean on = visualEnabled(selectedVisual);

                ctx.drawText(textRenderer, Text.literal(name), x, y + 22,
                        alpha(WHITE, a), true);
                ctx.drawText(textRenderer, Text.literal(on ? "ВКЛЮЧЕНО" : "ВЫКЛЮЧЕНО"),
                        x, y + 39, alpha(on ? red : MUTED, a), true);

                ctx.fill(x, y + 60, x + w, y + 96, alpha(0xB5101119, a));
                ctx.fill(x, y + 60, x + 3, y + 96, alpha(red, a));
                ctx.drawText(textRenderer, Text.literal("ЛКМ — включить / выключить"),
                        x + 11, y + 70, alpha(WHITE, a), false);
                ctx.drawText(textRenderer, Text.literal("ПКМ / ESC — назад"),
                        x + 11, y + 84, alpha(MUTED, a), false);
            }
        }

        private void drawSimpleCard(DrawContext ctx, int x, int y, int w, int red,
                                    float a, String title, String text) {
            ctx.fill(x + 3, y + 4, x + w + 3, y + 65, alpha(0x45000000, a));
            ctx.fill(x, y, x + w, y + 61, alpha(0xB5101119, a));
            ctx.fill(x, y, x + 3, y + 61, alpha(red, a));
            ctx.drawText(textRenderer, Text.literal(title), x + 11, y + 12,
                    alpha(WHITE, a), true);
            ctx.drawText(textRenderer, Text.literal(text), x + 11, y + 31,
                    alpha(MUTED, a), false);
        }

        private String tabSubtitle(int tab) {
            return switch (tab) {
                case 0 -> "Главная панель";
                case 2 -> "Визуальные инструменты";
                case 3 -> "Косметическое оформление";
                default -> "Настройки клиента";
            };
        }

        private String tabCardTitle(int tab) {
            return switch (tab) {
                case 0 -> "TysmVisuals";
                case 2 -> "Utilities";
                case 3 -> "Cosmetics";
                default -> "Settings";
            };
        }

        private String tabCardText(int tab) {
            return switch (tab) {
                case 0 -> "1.21.4 • Fabric • Visual Client";
                case 2 -> "Компактные визуальные элементы";
                case 3 -> "Тема, эффекты и оформление";
                default -> "Минималистичный интерфейс";
            };
        }

        private boolean visualEnabled(int i) {
            return switch (i) {
                case 0 -> keystrokesHud;
                case 1 -> statsHud;
                case 2 -> coordinatesHud;
                case 3 -> movementHud;
                case 4 -> targetHud;
                case 5 -> armorHud;
                case 6 -> itemHud;
                case 7 -> crosshairEnabled;
                case 8 -> ambientParticles;
                case 9 -> vignetteEnabled;
                case 10 -> hotbarGlow;
                case 11 -> hudEnabled;
                default -> extra(VISUALS[i]);
            };
        }

        private void toggleVisual(int i) {
            switch (i) {
                case 0 -> keystrokesHud = !keystrokesHud;
                case 1 -> statsHud = !statsHud;
                case 2 -> coordinatesHud = !coordinatesHud;
                case 3 -> movementHud = !movementHud;
                case 4 -> targetHud = !targetHud;
                case 5 -> armorHud = !armorHud;
                case 6 -> itemHud = !itemHud;
                case 7 -> crosshairEnabled = !crosshairEnabled;
                case 8 -> ambientParticles = !ambientParticles;
                case 9 -> vignetteEnabled = !vignetteEnabled;
                case 10 -> hotbarGlow = !hotbarGlow;
                case 11 -> hudEnabled = !hudEnabled;
                default -> setExtra(VISUALS[i], !extra(VISUALS[i]));
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int menuW = Math.min(370, width - 28);
            int menuH = Math.min(218, height - 28);
            int x = (width - menuW) / 2;
            int y = (height - menuH) / 2;
            int sideW = 94;

            if (button == 0) {
                if (mouseX >= x && mouseX <= x + sideW &&
                        mouseY >= y + 43 && mouseY <= y + 47 + TABS.length * 28) {
                    int index = (int)((mouseY - (y + 43)) / 28);
                    if (index >= 0 && index < TABS.length) {
                        selectedTab = index;
                        selectedVisual = -1;
                        return true;
                    }
                }

                if (selectedTab == 1) {
                    int contentX = x + sideW + 15;
                    int contentW = menuW - sideW - 25;
                    int listY = y + 14 + 35;

                    if (selectedVisual >= 0) {
                        toggleVisual(selectedVisual);
                        return true;
                    }

                    int rowH = 25;
                    int visibleRows = 6;
                    int colW = (contentW - 5) / 2;
                    for (int i = 0; i < VISUALS.length; i++) {
                        int row = i / 2;
                        int col = i % 2;
                        int visibleRow = row - visualScroll;
                        if (visibleRow < 0 || visibleRow >= visibleRows) continue;
                        int rowY = listY + visibleRow * rowH;
                        int colX = contentX + col * (colW + 5);
                        if (mouseX >= colX && mouseX <= colX + colW &&
                                mouseY >= rowY && mouseY <= rowY + 21) {
                            selectedVisual = i;
                            return true;
                        }
                    }
                }
            }

            if (button == 1 && selectedVisual >= 0) {
                selectedVisual = -1;
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            if (selectedTab == 1 && selectedVisual < 0) {
                int maxScroll = Math.max(0, ((VISUALS.length + 1) / 2) - 6);
                if (verticalAmount < 0) visualScroll = Math.min(maxScroll, visualScroll + 2);
                if (verticalAmount > 0) visualScroll = Math.max(0, visualScroll - 2);
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                if (selectedVisual >= 0) {
                    selectedVisual = -1;
                    return true;
                }
                close();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private static class TysmMainMenuScreen extends Screen {
        private float fade = 0f;
        private long openedAt;

        private TysmMainMenuScreen() {
            super(Text.literal("TysmVisuals Main Menu"));
        }

        @Override
        protected void init() {
            openedAt = System.currentTimeMillis();
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            MinecraftClient client = MinecraftClient.getInstance();
            int w = width;
            int h = height;
            int red = accent();

            float t = MathHelper.clamp((System.currentTimeMillis() - openedAt) / 500f, 0f, 1f);
            fade = 1f - (float)Math.pow(1f - t, 3);

            ctx.fill(0, 0, w, h, 0xFF08090E);
            for (int i = 0; i < 8; i++) {
                int yy = i * h / 8;
                int alpha = 18 + i * 3;
                ctx.fill(0, yy, w, yy + h / 8 + 1, (alpha << 24) | (red & 0xFFFFFF));
            }

            // Animated central visual panel.
            int panelW = Math.min(520, w - 40);
            int panelH = Math.min(270, h - 70);
            int px = (w - panelW) / 2;
            int py = Math.max(20, (h - panelH) / 2 - 8);

            ctx.fill(px + 6, py + 8, px + panelW + 6, py + panelH + 8, 0x55000000);
            ctx.fill(px, py, px + panelW, py + panelH, 0xD90A0B12);
            ctx.fill(px, py, px + panelW, py + 3, red);

            ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("TYSM"),
                    w / 2, py + 27, red);
            ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("VISUALS"),
                    w / 2, py + 42, WHITE);
            ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("1.21.4  •  FABRIC  •  RED EDITION"),
                    w / 2, py + 61, MUTED);

            int bw = Math.min(250, panelW - 70);
            int bx = (w - bw) / 2;
            String[] labels = {"Одиночная игра", "Сетевая игра", "Настройки", "Выйти"};
            for (int i = 0; i < labels.length; i++) {
                int by = py + 83 + i * 34;
                boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + 27;
                int bg = hover ? 0x4830333D : 0x28181A21;
                ctx.fill(bx, by, bx + bw, by + 27, bg);
                if (hover) ctx.fill(bx, by, bx + 3, by + 27, red);
                ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal(labels[i]),
                        bx + bw / 2, by + 9, hover ? WHITE : MUTED);
            }

            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal("RSHIFT — VISUAL MENU   •   TYSMVISUALS"),
                    w / 2, py + panelH - 19, MUTED);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return true;
            int w = width;
            int h = height;
            int panelW = Math.min(520, w - 40);
            int panelH = Math.min(270, h - 70);
            int py = Math.max(20, (h - panelH) / 2 - 8);
            int bw = Math.min(250, panelW - 70);
            int bx = (w - bw) / 2;

            for (int i = 0; i < 4; i++) {
                int by = py + 83 + i * 34;
                if (mouseX < bx || mouseX > bx + bw || mouseY < by || mouseY > by + 27) continue;

                MinecraftClient client = MinecraftClient.getInstance();
                switch (i) {
                    case 0 -> client.setScreen(new net.minecraft.client.gui.screen.world.SelectWorldScreen(this));
                    case 1 -> client.setScreen(new net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen(this));
                    case 2 -> client.setScreen(new net.minecraft.client.gui.screen.option.OptionsScreen(this, client.options));
                    case 3 -> client.scheduleStop();
                }
                return true;
            }
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                MinecraftClient.getInstance().setScreen(new TysmMenuScreen());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

}
