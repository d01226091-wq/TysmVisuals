package ru.tysmvisuals.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.tysmvisuals.TysmVisualsClient;

@Mixin(Camera.class)
public class CameraFreeLookMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void tysmvisuals$freelook(net.minecraft.world.BlockView area, net.minecraft.entity.Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (TysmVisualsClient.isFreeLookActive()) {
            ((Camera)(Object)this).setRotation(TysmVisualsClient.getFreeLookYaw(), TysmVisualsClient.getFreeLookPitch());
        }
    }
}
