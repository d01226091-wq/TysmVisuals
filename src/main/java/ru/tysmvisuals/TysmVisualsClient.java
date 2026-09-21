package ru.tysmvisuals;

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
    private static boolean hudEnabled = true;
    private static boolean redCrosshair = true;
    private static boolean accentBar = true;
    private static float pulse;

    private static final int RED = 0xFFFF2633;
    private static final int RED_SOFT = 0x88FF2633;
    private static final int WHITE = 0xFFF2F2F2;
    private static final int MUTED = 0xFFB8B8C2;
    private static final int PANEL = 0xD90A0B10;
    private static final int ROW = 0x281B1C24;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tysmvisuals.menu", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, "category.tysmvisuals"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pulse += 0.08f;
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new TysmMenuScreen());
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (hudEnabled) renderHud(context);
        });
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();

        ctx.fill(8, 8, 122, 36, 0xB0101016);
        ctx.fill(8, 8, 11, 36, RED);
        ctx.drawText(client.textRenderer, Text.literal("TYSM"), 18, 13, RED, true);
        ctx.drawText(client.textRenderer, Text.literal("VISUALS"), 58, 13, WHITE, true);

        if (redCrosshair) {
            int cx = w / 2, cy = h / 2;
            ctx.fill(cx - 6, cy - 1, cx + 7, cy + 1, 0x55FF2633);
            ctx.fill(cx - 1, cy - 6, cx + 1, cy + 7, 0x55FF2633);
            ctx.fill(cx - 4, cy - 1, cx + 5, cy + 1, RED);
            ctx.fill(cx - 1, cy - 4, cx + 1, cy + 5, RED);
        }

        if (accentBar) {
            float a = (MathHelper.sin(pulse) + 1f) * 0.5f;
            int alpha = 0x30 + (int) (a * 0x30);
            ctx.fill(w / 2 - 90, h - 52, w / 2 + 90, h - 49, (alpha << 24) | 0xFF2633);
        }
    }

    private static class TysmMenuScreen extends Screen {
        private static final String[][] CATEGORIES = {
                {"◉  Visual", "Blink", "Chams", "ChinaHat", "Custom Crystals", "Dog", "Ghost Hand", "Hands", "Hit Bubbles", "Hit Color", "HitBox"},
                {"◎  World", "Ambience", "Bad Trip", "Block Highlight", "Bright", "CrystalColor", "Custom Fog", "Custom Weather", "Item Physics", "Sky Color", "WaveEffect"},
                {"✦  Particles", "Ambient Particles", "Block Particle", "Cursor Particle", "Damage Particle", "Hit Particles", "Jump Particles", "Kill Particle", "Particle Trail", "Totem Particle", "Track Particle"},
                {"⚒  Utils", "Armor Durability", "Arrows", "AspectRatio", "Auto Sprint", "Cape", "Click Friend", "Dynamic Island", "Fake Player", "HitSound", "Kill Sound"},
                {"●  Client", "Theme", "Config Manager", "Custom loading screen", "Friends Manager", "Globals", "GuiSounds", "Interface", "Item Highlighter", "Language", "LiquidGlass"}
        };

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
            context.drawText(textRenderer, Text.literal("Fabric 1.21.4"), screenW - 118, 14, RED, true);
            context.drawText(textRenderer, Text.literal("Client"), screenW - 67, 29, MUTED, true);

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
            ctx.fill(x, y + 3, x + 150, y + 5, RED);
            ctx.drawText(textRenderer, Text.literal("TYSM"), x + 8, y + 9, WHITE, true);
            ctx.drawText(textRenderer, Text.literal("VISUALS"), x + 53, y + 9, RED, true);
            ctx.drawText(textRenderer, Text.literal("1.21.4  •  RED EDITION"), x + 9, y + 26, MUTED, false);
        }

        private void drawPanel(DrawContext ctx, int x, int y, int w, int h, String[] rows, int mouseX, int mouseY) {
            ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x50000000);
            ctx.fill(x, y, x + w, y + h, PANEL);
            ctx.fill(x, y, x + w, y + 2, RED);
            ctx.fill(x, y + 43, x + w, y + 45, RED_SOFT);

            ctx.drawText(textRenderer, Text.literal(rows[0]), x + 14, y + 14, WHITE, true);
            ctx.drawText(textRenderer, Text.literal("⚙"), x + w - 19, y + 14, RED, true);

            for (int i = 1; i < rows.length; i++) {
                int ry = y + 52 + (i - 1) * 35;
                boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= ry - 5 && mouseY < ry + 28;
                if (hover) ctx.fill(x + 5, ry - 5, x + w - 5, ry + 28, ROW);

                ctx.drawText(textRenderer, Text.literal(rows[i]), x + 10, ry + 3, hover ? WHITE : MUTED, false);

                if (isEnabled(rows[i])) {
                    ctx.drawText(textRenderer, Text.literal("✓"), x + w - 19, ry + 3, RED, true);
                } else if (hasSubmenu(rows[i])) {
                    ctx.drawText(textRenderer, Text.literal("›"), x + w - 17, ry + 3, RED, true);
                }
            }
        }

        private boolean isEnabled(String name) {
            return switch (name) {
                case "Chams", "ChinaHat", "Custom Crystals", "Hit Bubbles", "Block Highlight",
                     "Custom Fog", "Sky Color", "Cursor Particle", "Damage Particle", "Hit Particles",
                     "Kill Particle", "Particle Trail", "Totem Particle", "Armor Durability",
                     "Arrows", "Auto Sprint", "Dynamic Island", "HitSound", "Kill Sound", "LiquidGlass" -> true;
                default -> false;
            };
        }

        private boolean hasSubmenu(String name) {
            return switch (name) {
                case "Theme", "Config Manager", "Custom loading screen", "Friends Manager",
                     "Globals", "GuiSounds", "Interface", "Item Highlighter", "Language" -> true;
                default -> false;
            };
        }

        private void drawScoreboard(DrawContext ctx, int x, int y, int w, int h) {
            ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x50000000);
            ctx.fill(x, y, x + w, y + h, PANEL);
            ctx.fill(x, y, x + w, y + 2, RED);

            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("TYSM VISUALS"), x + w / 2, y + 14, RED);
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
                ctx.drawText(textRenderer, Text.literal(line), x + 12, yy, line.contains("Player") || line.contains("Practice") || line.contains("TysmWorld") ? RED : MUTED, false);
                yy += 21;
            }
        }

        private void drawControls(DrawContext ctx, int x, int y) {
            ctx.fill(x, y, x + 116, y + 48, 0xC50A0B10);
            ctx.fill(x, y, x + 116, y + 2, RED);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("ПРАВ ШИФТ"), x + 58, y + 16, WHITE);

            ctx.fill(x + 126, y, x + 226, y + 48, 0xC50A0B10);
            ctx.fill(x + 126, y, x + 226, y + 2, RED);
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

            for (int i = 0; i < CATEGORIES.length; i++) {
                int x = left + i * (panelW + gap);
                if (mouseX >= x && mouseX <= x + panelW && mouseY >= top + 52 && mouseY <= top + panelH) {
                    int row = (int) ((mouseY - (top + 52)) / 35) + 1;
                    if (row >= 1 && row < CATEGORIES[i].length) {
                        String feature = CATEGORIES[i][row];
                        if (feature.equals("Theme")) {
                            redCrosshair = !redCrosshair;
                        } else if (feature.equals("GuiSounds")) {
                            accentBar = !accentBar;
                        } else if (feature.equals("HUD") || feature.equals("Interface")) {
                            hudEnabled = !hudEnabled;
                        }
                        return true;
                    }
                }
            }

            if (mouseX >= width - 245 && mouseY >= height - 72) {
                close();
                return true;
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
