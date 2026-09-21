package ru.tysmvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class TysmVisualsClient implements ClientModInitializer {
    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tysmvisuals.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.tysmvisuals"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new TysmMenuScreen());
            }
        });
    }

    private static class TysmMenuScreen extends Screen {
        protected TysmMenuScreen() { super(Text.literal("TysmVisuals")); }

        @Override
        protected void init() {
            addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                    Text.literal("Close"), b -> close())
                    .dimensions(width / 2 - 50, height / 2 + 35, 100, 20).build());
        }

        @Override
        public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            int red = 0xFFFF3333;
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("TysmVisuals"), width / 2, height / 2 - 55, red);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("PvP visuals • Right Shift"), width / 2, height / 2 - 30, 0xFFFFFFFF);
            super.render(context, mouseX, mouseY, delta);
        }
    }
}
