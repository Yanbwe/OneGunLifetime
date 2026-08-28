package org.yanbwe.onegunlifetime.lifecycle;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

import org.yanbwe.onegunlifetime.scan.AssimilationService;

/**
 * Prevents bound players from throwing away their projection gun.
 *
 * <p>Drops are cancelled and immediately replaced by a fresh projection gun in
 * the player's inventory. Death drops do not go through {@link ItemTossEvent},
 * so they are intentionally left alone.</p>
 */
public final class DropHandler {

    private DropHandler() {
    }

    public static void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack dropped = event.getEntity().getItem();
        if (!AssimilationService.isOwnedProjection(dropped, serverPlayer.getUUID())) {
            return;
        }

        event.setCanceled(true);
        event.getEntity().discard();
        AssimilationService.ensureProjection(serverPlayer, serverPlayer.level().registryAccess());
        serverPlayer.displayClientMessage(
                Component.translatable("onegunlifetime.actionbar.cannot_drop"), true);
    }
}