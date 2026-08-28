package org.yanbwe.onegunlifetime.scan;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Low-frequency fallback scanner for bound server players.
 *
 * <p>Because NeoForge 1.21.1 has no generic inventory-changed event, this
 * handler periodically scans every online bound player's personal inventory.
 * The interval is deliberately relaxed (every 100 ticks) so hot paths remain
 * cheap; login and pickup remain the immediate triggers.</p>
 */
public final class LowFrequencyScanHandler {

    /** Scan every 100 ticks (5 seconds) per player. */
    private static final int SCAN_INTERVAL_TICKS = 100;

    private static final Map<UUID, Long> LAST_SCAN_TICK = new ConcurrentHashMap<>();

    private LowFrequencyScanHandler() {
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SCAN_TICK.remove(player.getUUID());
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!SoulDataManager.isBound(player)) {
            LAST_SCAN_TICK.remove(player.getUUID());
            return;
        }

        long now = player.level().getGameTime();
        Long last = LAST_SCAN_TICK.get(player.getUUID());
        if (last != null && now - last < SCAN_INTERVAL_TICKS) {
            return;
        }

        LAST_SCAN_TICK.put(player.getUUID(), now);
        PlayerInventoryScanner.scan(player);
    }
}