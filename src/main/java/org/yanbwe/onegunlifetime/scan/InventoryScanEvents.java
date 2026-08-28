package org.yanbwe.onegunlifetime.scan;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Immediate inventory scan triggers: player login and item entity pickup.
 *
 * <p>NeoForge 1.21.1 does not expose a generic
 * {@code InventoryChangedEvent}; container content changes are therefore
 * covered by the low-frequency tick fallback in
 * {@link LowFrequencyScanHandler}.</p>
 */
public final class InventoryScanEvents {

    private InventoryScanEvents() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (SoulDataManager.isBound(player)) {
            PlayerInventoryScanner.scan(player);
        }
    }

    public static void onItemEntityPickupPost(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (SoulDataManager.isBound(player)) {
            PlayerInventoryScanner.scan(player);
        }
    }
}