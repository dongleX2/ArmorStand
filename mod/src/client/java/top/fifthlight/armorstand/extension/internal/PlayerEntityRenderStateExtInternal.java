package top.fifthlight.armorstand.extension.internal;

import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerEntityRenderStateExtInternal {
    void armorstand$setUuid(UUID uuid);

    UUID armorstand$getUuid();

    void armorstand$setRidingEntityType(@Nullable EntityType<?> ridingEntityType);

    @Nullable
    EntityType<?> armorstand$getRidingEntityType();

    void armorstand$setSprinting(boolean sprinting);

    boolean armorstand$isSprinting();

    void armorstand$setLimbSwingSpeed(float limbSwingSpeed);

    float armorstand$getLimbSwingSpeed();

    void armorstand$setDead(boolean dead);

    boolean armorstand$isDead();
}
