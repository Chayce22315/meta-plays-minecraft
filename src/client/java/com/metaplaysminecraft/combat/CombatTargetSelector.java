package com.metaplaysminecraft.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public final class CombatTargetSelector {
    private CombatTargetSelector() {
    }

    public static Optional<LivingEntity> findHostileTarget(Player self) {
        Objects.requireNonNull(self, "self");

        return self.level().getEntitiesOfClass(
                        LivingEntity.class,
                        self.getBoundingBox().inflate(16.0),
                        entity -> isValidHostileTarget(self, entity)
                )
                .stream()
                .min(Comparator.comparingDouble(self::distanceToSqr));
    }

    /** player entities are never valid combat targets. */
    public static boolean isValidHostileTarget(Player self, Entity candidate) {
        if (!(candidate instanceof LivingEntity living)) {
            return false;
        }
        if (candidate instanceof Player) {
            return false;
        }
        return candidate != self && living.isAlive() && !candidate.isSpectator() && candidate.isPickable();
    }
}
