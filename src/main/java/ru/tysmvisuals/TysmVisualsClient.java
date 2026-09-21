package ru.tysmvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
        int red = 0xFFE83B3B;
        int white = 0xFFF2F2F2;

        ctx.fill(8, 8, 116, 34, 0xB0101016);
        ctx.fill(8, 8, 11, 34, red);
        ctx.drawText(client.textRenderer, Text.literal("TYSM"), 17, 13, red, true);
        ctx.drawText(client.textRenderer, Text.literal("VISUALS"), 55, 13, white, true);

        if (redCrosshair) {
            int cx = w / 2, cy = h / 2;
            ctx.fill(cx - 5, cy - 1, cx + 6, cy + 1, 0x66E83B3B);
            ctx.fill(cx - 1, cy - 5, cx + 1, cy + 6, 0x66E83B3B);
            ctx.fill(cx - 3, cy - 1, cx + 4, cy + 1, red);
            ctx.fill(cx - 1, cy - 3, cx + 1, cy + 4, red);
        }

        if (accentBar) {
            float a = (MathHelper.sin(pulse) + 1f) * 0.5f;
            int alpha = 0x30 + (int)(a * 0x20);
            ctx.fill(w / 2 - 70, h - 50, w / 2 + 70, h - 47, (alpha << 24) | 0xE83B3B);
        }
    }

    private static class TysmMenuScreen extends Screen {
        protected TysmMenuScreen() { super(Text.literal("TysmVisuals")); }

        @Override
        protected void init() {
            int x = width / 2 - 90, y = height / 2 - 45;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("HUD: " + (hudEnabled ? "ON" : "OFF")),
                    b -> { hudEnabled = !hudEnabled; b.setMessage(Text.literal("HUD: " + (hudEnabled ? "ON" : "OFF"))); })
                    .dimensions(x, y, 180, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Red Crosshair: " + (redCrosshair ? "ON" : "OFF")),
                    b -> { redCrosshair = !redCrosshair; b.setMessage(Text.literal("Red Crosshair: " + (redCrosshair ? "ON" : "OFF"))); })
                    .dimensions(x, y + 26, 180, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Accent Bar: " + (accentBar ? "ON" : "OFF")),
                    b -> { accentBar = !accentBar; b.setMessage(Text.literal("Accent Bar: " + (accentBar ? "ON" : "OFF"))); })
                    .dimensions(x, y + 52, 180, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                    .dimensions(x, y + 84, 180, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            int red = 0xFFE83B3B, white = 0xFFF2F2F2;
            context.fill(width / 2 - 125, height / 2 - 82, width / 2 + 125, height / 2 + 125, 0xD0101016);
            context.fill(width / 2 - 125, height / 2 - 82, width / 2 + 125, height / 2 - 78, red);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("TysmVisuals"), width / 2, height / 2 - 68, red);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Red visual theme"), width / 2, height / 2 - 55, white);
            super.render(context, mouseX, mouseY, delta);
        }
    }
}
