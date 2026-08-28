package org.yanbwe.onegunlifetime.shoot;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.shooting.ShootPredicate;
import org.yanbwe.modularshoot.shooting.ShootPredicateResult;

import org.yanbwe.onegunlifetime.def.SoulGunId;
import org.yanbwe.onegunlifetime.scan.LowFrequencyScanHandler;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Denies bound players from firing foreign ModularShoot guns and fast-tracks
 * their assimilation to the next player tick.
 *
 * <p>This closes the conversion window: previously a foreign gun obtained via
 * containers, creative menu or commands stayed firable until the low-frequency
 * scan caught it (up to 5 seconds). With this predicate the trigger pull on a
 * foreign gun always aborts the shot, so a bound player can never fire a gun
 * that is about to be assimilated.</p>
 *
 * <p>ModularShoot's shoot contract requires predicates to be side-effect free.
 * This implementation therefore does not touch the inventory: it only records
 * a one-shot scan request in {@link LowFrequencyScanHandler} (OneGun's own
 * bookkeeping) and returns a failing result. The next player tick converts the
 * gun through the regular {@code PlayerInventoryScanner} pipeline.</p>
 *
 * <p>Registered before {@link NonOwnerShootPredicate} so that, for a bound
 * player, every foreign gun — including another player's soul gun — is claimed
 * by the assimilation path. Unbound players fall through to the non-owner
 * check, preserving the normal behaviour of regular ModularShoot guns.</p>
 */
public final class AssimilationShootPredicate implements ShootPredicate {

    /** Action-bar reason shown when the trigger pull is denied. */
    public static final String DENIED_MESSAGE =
            "lang:onegunlifetime.shoot.assimilating";

    @Override
    public ShootPredicateResult test(Player player, ItemStack gun) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ShootPredicateResult.success();
        }

        Optional<UUID> soulOwner = ModularShootAPI.getGunId(gun)
                .filter(SoulGunId::isSoulGunId)
                .flatMap(SoulGunId::parse);
        if (soulOwner.isPresent() && soulOwner.get().equals(player.getUUID())) {
            return ShootPredicateResult.success();
        }

        if (!SoulDataManager.isBound(serverPlayer)) {
            return ShootPredicateResult.success();
        }

        if (!ModularShootAPI.isGun(gun, serverPlayer.registryAccess())) {
            return ShootPredicateResult.success();
        }

        LowFrequencyScanHandler.requestScan(serverPlayer);
        return ShootPredicateResult.failure(DENIED_MESSAGE);
    }
}
