package com.hitboxscaler.mixin;

import com.hitboxscaler.HitboxScaleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Подменяем итоговый EntityDimensions у игрока в зависимости от
 * выбранного им множителя. Работает и на клиенте (визуально/коллизии
 * локально), и на сервере (если мод стоит на сервере — тогда меняется
 * и реальный хитбокс для попаданий/коллизий у всех).
 */
@Mixin(Entity.class)
public abstract class EntityDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void hitboxscaler$scale(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity player) {
            float scale = HitboxScaleManager.getScale(player.getUuid());
            if (scale != 1.0f) {
                cir.setReturnValue(cir.getReturnValue().scaled(scale));
            }
        }
    }
}
