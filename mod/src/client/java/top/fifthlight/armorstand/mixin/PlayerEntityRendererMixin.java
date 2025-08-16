package top.fifthlight.armorstand.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.extension.internal.PlayerEntityRenderStateExtInternal;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("HEAD"))
    public void onUpdateRenderState(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        var stateInternal = ((PlayerEntityRenderStateExtInternal) state);
        stateInternal.armorstand$setUuid(entity.getUuid());
        var vehicle = entity.getVehicle();
        if (vehicle != null) {
            stateInternal.armorstand$setRidingEntityType(vehicle.getType());
        } else {
            stateInternal.armorstand$setRidingEntityType(null);
        }
        stateInternal.armorstand$setSprinting(entity.isSprinting());
        stateInternal.armorstand$setLimbSwingSpeed(entity.limbAnimator.getSpeed());
        stateInternal.armorstand$setDead(entity.isDead());
    }
}
