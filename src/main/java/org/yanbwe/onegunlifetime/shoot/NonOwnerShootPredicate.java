package org.yanbwe.onegunlifetime.shoot;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.shooting.ShootPredicate;
import org.yanbwe.modularshoot.shooting.ShootPredicateResult;
import org.yanbwe.onegunlifetime.def.SoulGunId;

/**
 * Prevents players from firing projection guns they do not own.
 *
 * <p>Non-soul guns (including regular ModularShoot guns) are allowed to keep
 * their normal behaviour. Only {@code onegunlifetime:soul_*} projection guns
 * are restricted: the shooter must be the soul owner encoded in the gun id.</p>
 */
public final class NonOwnerShootPredicate implements ShootPredicate {

    @Override
    public ShootPredicateResult test(Player player, ItemStack gun) {
        Optional<UUID> ownerId = ModularShootAPI.getGunId(gun)
                .filter(SoulGunId::isSoulGunId)
                .flatMap(SoulGunId::parse);

        if (ownerId.isEmpty()) {
            return ShootPredicateResult.success();
        }

        if (ownerId.get().equals(player.getUUID())) {
            return ShootPredicateResult.success();
        }

        return ShootPredicateResult.failure("lang:onegunlifetime.shoot.not_owner");
    }
}