package org.yanbwe.onegunlifetime.plugin;

import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.component.PluginInstance;
import org.yanbwe.onegunlifetime.scan.AssimilationService;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Backfills soul plugin data from the physical projection gun in a player's
 * personal inventory.
 *
 * <p>ModularShoot normally posts install/uninstall events, but paths such as
 * creative-menu item edits or container modifications can change the gun's
 * final {@code GunData} without a server-side plugin event. This scanner makes
 * the projection gun's installed plugin list authoritative whenever a regular
 * inventory scan runs.</p>
 */
public final class PluginBackfillScanner {

    private PluginBackfillScanner() {
    }

    /**
     * Compares the bound player's soul plugin list with the installed plugins
     * on the player's projection gun and writes the gun's list back when they
     * differ.
     *
     * @param player the bound server player to check
     */
    public static void syncFromProjectionIfNeeded(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (!SoulDataManager.isBound(player)) {
            return;
        }

        UUID ownerId = player.getUUID();
        ItemStack projection = findProjection(player, ownerId);
        if (projection.isEmpty()) {
            return;
        }

        List<PluginInstance> gunPlugins = ModularShootAPI.getInstalledPlugins(projection);
        CompoundTag gunState = ModularShootAPI.getGunData(projection)
                .map(gunData -> gunData.state())
                .orElse(null);
        SoulData soul = SoulDataManager.get(player);
        if (soul == null) {
            return;
        }

        boolean pluginsDiffer = !gunPlugins.equals(soul.plugins());
        boolean stateDiffers = gunState != null && !gunState.equals(soul.gunState());
        if (!pluginsDiffer && !stateDiffers) {
            return;
        }

        if (pluginsDiffer) {
            SoulDataManager.setPlugins(player, gunPlugins);
        }
        if (stateDiffers) {
            SoulDataManager.setGunState(player, gunState.copy());
        }
        PluginRefresher.refresh(player);
    }

    private static ItemStack findProjection(ServerPlayer player, UUID ownerId) {
        Inventory inventory = player.getInventory();

        ItemStack found = findProjection(inventory.items, ownerId);
        if (!found.isEmpty()) {
            return found;
        }
        found = findProjection(inventory.armor, ownerId);
        if (!found.isEmpty()) {
            return found;
        }
        return findProjection(inventory.offhand, ownerId);
    }

    private static ItemStack findProjection(NonNullList<ItemStack> slots, UUID ownerId) {
        for (ItemStack stack : slots) {
            if (AssimilationService.isOwnedProjection(stack, ownerId)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}