package ru.tysmvisuals.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.tysmvisuals.TysmVisualsClient;

@Mixin(ClientWorld.class)
public class ClientWorldSkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void tysmvisuals$customSkyColor(Vec3d cameraPos, float tickProgress,
                                             CallbackInfoReturnable<Integer> cir) {
        if (TysmVisualsClient.isSkyColorEnabled()) {
            cir.setReturnValue(TysmVisualsClient.getSkyColor());
        }
    }
}
