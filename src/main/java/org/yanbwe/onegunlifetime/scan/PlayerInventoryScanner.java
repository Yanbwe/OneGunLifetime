package org.yanbwe.onegunlifetime.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.yanbwe.onegunlifetime.lifecycle.DestroyRecoveryHandler;
import org.yanbwe.onegunlifetime.plugin.PluginBackfillScanner;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Scans a bound server player's personal inventory and enforces OneGun's
 * one-projection-gun rule.
 *
 * <p>The scanner covers the main inventory + hotbar ({@code items}), the armor
 * slots and the offhand slot. It removes duplicate projection guns, ensures a
 * projection exists, and hands every foreign ModularShoot gun to
 * {@link AssimilationService} for assimilation (clear or replace + plugin
 * transfer).</p>
 */
public final class PlayerInventoryScanner {

    private static final Set<UUID> SCANNING = ConcurrentHashMap.newKeySet();

    private PlayerInventoryScanner() {
    }

    /**
     * Runs a full personal-inventory scan for a bound server player.
     *
     * <p>This method is a no-op for unbound players, client-side calls, or
     * re-entrant calls made while another scan is already in progress.</p>
     *
     * @param player the server player to scan
     */
    public static void scan(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (!SoulDataManager.isBound(player)) {
            return;
        }
        UUID playerId = player.getUUID();
        if (!SCANNING.add(playerId)) {
            return;
        }
        if (DestroyRecoveryHandler.shouldSkipScan(player)) {
            SCANNING.remove(playerId);
            return;
        }

        try {
            scanInternal(player);
        } finally {
            SCANNING.remove(playerId);
        }
    }

    private static void scanInternal(ServerPlayer player) {
        RegistryAccess registryAccess = player.level().registryAccess();
        UUID ownerId = player.getUUID();
        Inventory inventory = player.getInventory();

        List<SlotEntry> entries = new ArrayList<>();
        collect(inventory.items, 0, entries);
        collect(inventory.armor, 36, entries);
        collect(inventory.offhand, 40, entries);

        // 1. Find the first owned projection gun.
        int firstProjectionSlot = -1;
        for (SlotEntry entry : entries) {
            if (AssimilationService.isOwnedProjection(entry.stack(), ownerId)) {
                firstProjectionSlot = entry.index();
                break;
            }
        }

        // 2. Deduplicate: keep the first projection, void every extra one.
        if (firstProjectionSlot >= 0) {
            for (SlotEntry entry : entries) {
                if (entry.index() != firstProjectionSlot
                        && AssimilationService.isOwnedProjection(entry.stack(), ownerId)) {
                    inventory.setItem(entry.index(), ItemStack.EMPTY);
                }
            }
        }

        // 3. Assimilate every foreign gun. Do not create/ensure a projection
        //    before this pass: assimilation can decide per-foreign-gun whether
        //    an existing projection is needed. Check the live inventory each
        //    iteration because previous entries may have replaced/cleared.
        for (SlotEntry entry : entries) {
            ItemStack current = inventory.getItem(entry.index());
            if (current.isEmpty() || !AssimilationService.isForeignGun(current, registryAccess, ownerId)) {
                continue;
            }
            boolean hasProjection = hasOwnedProjection(player, ownerId);
            AssimilationService.assimilateForeignGun(
                    player, entry.index(), current, registryAccess, hasProjection);
        }

        // 4. Finally ensure a projection exists (also the stage 6 recovery
        //    path). This runs after foreign-gun handling so a full inventory
        //    only drops when neither a backpack projection nor a nearby owned
        //    dropped projection exists.
        if (!hasOwnedProjection(player, ownerId)) {
            AssimilationService.ensureProjection(player, registryAccess);
        }

        // 5. Stage 7: make the projection gun's final plugin list authoritative
        //    for paths that bypass ModularShoot's server-side plugin events.
        PluginBackfillScanner.syncFromProjectionIfNeeded(player);
    }

    private static void collect(
            NonNullList<ItemStack> slots, int baseIndex, List<SlotEntry> entries) {
        for (int i = 0; i < slots.size(); i++) {
            entries.add(new SlotEntry(baseIndex + i, slots.get(i)));
        }
    }

    private static boolean hasOwnedProjection(ServerPlayer player, UUID ownerId) {
        Inventory inventory = player.getInventory();
        return hasOwnedProjection(inventory.items, ownerId)
                || hasOwnedProjection(inventory.armor, ownerId)
                || hasOwnedProjection(inventory.offhand, ownerId);
    }

    private static boolean hasOwnedProjection(NonNullList<ItemStack> slots, UUID ownerId) {
        for (ItemStack stack : slots) {
            if (AssimilationService.isOwnedProjection(stack, ownerId)) {
                return true;
            }
        }
        return false;
    }

    private record SlotEntry(int index, ItemStack stack) {
    }
}