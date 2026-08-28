package org.yanbwe.onegunlifetime.scan;

import java.util.Map;
import java.util.Set;
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
 *
 * <p>Additionally, {@link #requestScan} lets immediate paths (such as the
 * shoot-time assimilation predicate) bypass the relaxed interval: the flagged
 * player is scanned on their very next tick instead of waiting for the next
 * interval window.</p>
 */
public final class LowFrequencyScanHandler {

    /** Scan every 100 ticks (5 seconds) per player. */
    private static final int SCAN_INTERVAL_TICKS = 100;

    private static final Map<UUID, Long> LAST_SCAN_TICK = new ConcurrentHashMap<>();

    private static final Set<UUID> SCAN_REQUESTED = ConcurrentHashMap.newKeySet();

    private LowFrequencyScanHandler() {
    }

    /**
     * Flags the player for an immediate scan on their next tick, bypassing
     * the relaxed interval. Safe to call repeatedly; the flag is one-shot.
     *
     * @param player the server player whose inventory should be scanned soon
     */
    public static void requestScan(ServerPlayer player) {
        if (player != null) {
            SCAN_REQUESTED.add(player.getUUID());
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            LAST_SCAN_TICK.remove(playerId);
            SCAN_REQUESTED.remove(playerId);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        UUID playerId = player.getUUID();
        if (!SoulDataManager.isBound(player)) {
            LAST_SCAN_TICK.remove(playerId);
            SCAN_REQUESTED.remove(playerId);
            return;
        }

        long now = player.level().getGameTime();

        if (SCAN_REQUESTED.remove(playerId)) {
            LAST_SCAN_TICK.put(playerId, now);
            PlayerInventoryScanner.scan(player);
            return;
        }

        Long last = LAST_SCAN_TICK.get(playerId);
        if (last != null && now - last < SCAN_INTERVAL_TICKS) {
            return;
        }

        LAST_SCAN_TICK.put(playerId, now);
        PlayerInventoryScanner.scan(player);
    }
}
