package top.fifthlight.armorstand.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fifthlight.armorstand.extension.internal.PlayerEntityRenderStateExtInternal;

import java.util.UUID;

@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements PlayerEntityRenderStateExtInternal {
    @Unique
    private UUID uuid;

    @Unique
    @Nullable
    private EntityType<?> ridingEntityType;

    @Unique
    private boolean sprinting;

    @Unique
    private float limbSwingSpeed;

    @Unique
    private boolean dead;

    @Override
    public void armorstand$setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID armorstand$getUuid() {
        return uuid;
    }

    @Override
    public void armorstand$setRidingEntityType(@Nullable EntityType<?> entityType) {
        this.ridingEntityType = entityType;
    }

    @Nullable
    @Override
    public EntityType<?> armorstand$getRidingEntityType() {
        return ridingEntityType;
    }

    @Override
    public void armorstand$setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    @Override
    public boolean armorstand$isSprinting() {
        return sprinting;
    }

    @Override
    public void armorstand$setLimbSwingSpeed(float limbSwingSpeed) {
        this.limbSwingSpeed = limbSwingSpeed;
    }

    @Override
    public float armorstand$getLimbSwingSpeed() {
        return limbSwingSpeed;
    }

    @Override
    public void armorstand$setDead(boolean dead) {
        this.dead = dead;
    }

    @Override
    public boolean armorstand$isDead() {
        return dead;
    }
}