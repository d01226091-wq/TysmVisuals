package ru.tysmvisuals.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyReturnValue;
import ru.tysmvisuals.TysmVisualsClient;

@Mixin(GameRenderer.class)
public class GameRendererZoomMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float tysmvisuals$zoom(float fov) {
        return TysmVisualsClient.isZoomActive() ? fov * 0.35f : fov;
    }
}
