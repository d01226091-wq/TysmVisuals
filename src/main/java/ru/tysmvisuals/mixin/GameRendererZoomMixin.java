package ru.tysmvisuals.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.tysmvisuals.TysmVisualsClient;

@Mixin(GameRenderer.class)
public class GameRendererZoomMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void tysmvisuals$zoom(Camera camera, float tickDelta, boolean changingFov,
                                   CallbackInfoReturnable<Float> cir) {
        if (TysmVisualsClient.isZoomActive()) {
            cir.setReturnValue(cir.getReturnValue() * 0.35f);
        }
    }
}
