package org.yanbwe.onegunlifetime.lifecycle;

import net.minecraft.server.level.ServerPlayer;

/**
 * Coordinates destroy recovery with command-clear suppression.
 *
 * <p>Recovery itself is already implemented by
 * {@link org.yanbwe.onegunlifetime.scan.PlayerInventoryScanner}: its low-
 * frequency scan calls {@code AssimilationService.ensureProjection} whenever a
 * bound player has no projection gun. This class only guards that scan so a
 * {@code /clear} does not immediately resurrect the cleared gun.</p>
 */
public final class DestroyRecoveryHandler {

    private DestroyRecoveryHandler() {
    }

    /**
     * Consumes a pending {@code /clear} suppression and tells the caller to
     * skip the current inventory scan.
     *
     * @param player the server player about to be scanned
     * @return {@code true} when the scan should be skipped for this tick
     */
    public static boolean shouldSkipScan(ServerPlayer player) {
        if (CommandClearSuppression.isSuppressed(player)) {
            CommandClearSuppression.consume(player);
            return true;
        }
        return false;
    }
}