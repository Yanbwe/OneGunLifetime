package org.yanbwe.onegunlifetime.scan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.component.PluginInstance;
import org.yanbwe.modularshoot.plugin.PluginInstallService.InstallResult;
import org.yanbwe.modularshoot.plugin.PluginRegistry;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeModifierService;
import org.yanbwe.onegunlifetime.def.SoulGunId;
import org.yanbwe.onegunlifetime.item.ProjectionGunFactory;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Server-side service that converts foreign ModularShoot guns into the
 * player's soul projection gun.
 *
 * <p>The service is intentionally side-effect free for its recognition
 * helpers ({@link #isOwnedProjection} and {@link #isForeignGun}) so they can
 * be unit-tested without a running game. The mutating methods operate on a
 * {@link ServerPlayer}'s inventory and soul data only.</p>
 */
public final class AssimilationService {

    private AssimilationService() {
    }

    /**
     * Returns whether a stack is a projection gun owned by the given player.
     *
     * @param stack   the stack to inspect
     * @param ownerId the soul owner UUID
     * @return {@code true} when the stack carries a OneGunLifetime soul gun id
     *         whose parsed owner matches {@code ownerId}
     */
    public static boolean isOwnedProjection(ItemStack stack, UUID ownerId) {
        if (stack == null || stack.isEmpty() || ownerId == null) {
            return false;
        }
        return ModularShootAPI.getGunId(stack)
                .filter(SoulGunId::isSoulGunId)
                .flatMap(SoulGunId::parse)
                .filter(ownerId::equals)
                .isPresent();
    }

    /**
     * Returns whether a stack is a foreign ModularShoot gun from the
     * perspective of the given soul owner.
     *
     * <p>A stack is foreign when ModularShoot recognises it as a gun but it
     * is not the owner's own projection gun. This deliberately includes
     * another player's projection gun or a regular ModularShoot gun, and it
     * treats a stacked pile as a single foreign gun entry.</p>
     *
     * @param stack   the stack to inspect
     * @param access  the runtime registry view
     * @param ownerId the soul owner UUID
     * @return {@code true} when the stack is a gun and not owned by the player
     */
    public static boolean isForeignGun(ItemStack stack, RegistryAccess access, UUID ownerId) {
        return stack != null
                && !stack.isEmpty()
                && ModularShootAPI.isGun(stack, access)
                && !isOwnedProjection(stack, ownerId);
    }

    /**
     * Ensures a bound player has exactly one projection gun in their
     * inventory. If there already is one this is a no-op.
     *
     * <p>The projection is placed into the first free main-inventory slot via
     * {@code Inventory.add}. If the inventory is full it is dropped at the
     * player's feet. This method is also the backbone for stage 6 recovery:
     * it creates a fresh projection whenever the inventory scan finds none.</p>
     *
     * @param player         the bound server player
     * @param registryAccess the runtime registry view
     */
    public static void ensureProjection(ServerPlayer player, RegistryAccess registryAccess) {
        if (player == null || registryAccess == null) {
            return;
        }
        if (hasProjectionInInventory(player)) {
            return;
        }
        if (hasOwnedProjectionNearby(player)) {
            return;
        }
        SoulData data = SoulDataManager.get(player);
        if (data == null) {
            return;
        }
        ItemStack projection = ProjectionGunFactory.create(data, registryAccess);
        if (!player.getInventory().add(projection)) {
            player.drop(projection, false);
        }
    }

    /**
     * Assimilates one foreign gun at the given absolute inventory slot.
     *
     * <p>When {@code hasProjection} is {@code true} the foreign slot is
     * cleared and its plugins are transferred into the existing backpack
     * projection. When it is {@code false} the foreign slot is replaced by a
     * fresh projection gun and the plugins are installed into that new
     * projection. In both cases the final plugin list is written back to the
     * player's soul data and the player-mounted attributes are refreshed.</p>
     *
     * @param player         the bound server player
     * @param slotIndex      absolute inventory slot index (0-35 main, 36-39
     *                       armor, 40 offhand)
     * @param foreignGun     the foreign gun stack currently in that slot
     * @param registryAccess the runtime registry view
     * @param hasProjection  whether a projection gun exists in the player's
     *                       inventory before this call
     */
    public static void assimilateForeignGun(
            ServerPlayer player,
            int slotIndex,
            ItemStack foreignGun,
            RegistryAccess registryAccess,
            boolean hasProjection) {
        if (player == null || registryAccess == null || foreignGun == null || foreignGun.isEmpty()) {
            return;
        }
        if (!ModularShootAPI.isGun(foreignGun, registryAccess)) {
            return;
        }

        // Another player's soul gun is not a source of plugin items: it must
        // never be read, merged or refunded. Only clear/replace it.
        List<PluginInstance> foreignPlugins = isOtherPlayerSoulGun(foreignGun, player.getUUID())
                ? List.of()
                : ModularShootAPI.getInstalledPlugins(foreignGun);
        ItemStack projection;

        if (hasProjection) {
            // Existing projection: the foreign gun is voided, its slot cleared.
            player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
            projection = findProjection(player);
            if (projection.isEmpty()) {
                ensureProjection(player, registryAccess);
                projection = findProjection(player);
            }
            if (projection.isEmpty()) {
                // No backpack projection to merge into (for example it was
                // dropped because the inventory was full). Return the plugins
                // rather than silently losing them.
                refundPlugins(player, foreignPlugins);
                return;
            }
        } else {
            // No projection: this foreign gun slot itself becomes the soul
            // gun. This is the "replace the slot" path.
            SoulData data = SoulDataManager.get(player);
            if (data == null) {
                return;
            }
            projection = ProjectionGunFactory.create(data, registryAccess);
            player.getInventory().setItem(slotIndex, projection);
        }

        projection = mergePlugins(player, projection, foreignPlugins, registryAccess);
        writeBackProjection(player, projection);
        finishAssimilation(player);

        player.displayClientMessage(
                Component.translatable("onegunlifetime.actionbar.assimilated"), true);
    }

    private static ItemStack mergePlugins(
            ServerPlayer player,
            ItemStack projection,
            List<PluginInstance> foreignPlugins,
            RegistryAccess registryAccess) {
        for (PluginInstance pluginInstance : foreignPlugins) {
            ItemStack pluginStack = PluginRegistry.createPluginStack(pluginInstance.pluginId());
            InstallResult result = ModularShootAPI.installPlugin(projection, pluginStack, player);
            if (result.success()) {
                projection = result.installedGun();
                int slot = findProjectionSlot(player);
                if (slot >= 0) {
                    player.getInventory().setItem(slot, projection);
                }
            } else {
                refundPluginStack(player, pluginStack);
            }
        }
        return projection;
    }

    private static void refundPlugins(ServerPlayer player, List<PluginInstance> plugins) {
        for (PluginInstance pluginInstance : plugins) {
            refundPluginStack(player, PluginRegistry.createPluginStack(pluginInstance.pluginId()));
        }
    }

    private static void refundPluginStack(ServerPlayer player, ItemStack pluginStack) {
        if (player.getInventory().add(pluginStack)) {
            return;
        }
        player.drop(pluginStack, false);
        player.displayClientMessage(
                Component.translatable("onegunlifetime.actionbar.plugin_returned"), true);
    }

    private static void writeBackProjection(ServerPlayer player, ItemStack projection) {
        int slot = findProjectionSlot(player);
        if (slot >= 0) {
            player.getInventory().setItem(slot, projection);
        }
    }

    private static void finishAssimilation(ServerPlayer player) {
        ItemStack projection = findProjection(player);
        if (projection.isEmpty()) {
            return;
        }
        List<PluginInstance> latestPlugins = ModularShootAPI.getInstalledPlugins(projection);
        SoulDataManager.setPlugins(player, latestPlugins);
        PlayerGunAttributeModifierService.refresh(player);
    }

    private static boolean hasOwnedProjectionNearby(ServerPlayer player) {
        AABB radius = player.getBoundingBox().inflate(8.0);
        return !player.serverLevel().getEntitiesOfClass(
                ItemEntity.class,
                radius,
                item -> isOwnedProjection(item.getItem(), player.getUUID())
        ).isEmpty();
    }

    private static boolean isOtherPlayerSoulGun(ItemStack stack, UUID ownerId) {
        Optional<UUID> parsedOwner = ModularShootAPI.getGunId(stack)
                .filter(SoulGunId::isSoulGunId)
                .flatMap(SoulGunId::parse);
        return parsedOwner.isPresent() && !parsedOwner.get().equals(ownerId);
    }

    private static boolean hasProjectionInInventory(ServerPlayer player) {
        return findProjectionSlot(player) >= 0;
    }

    private static ItemStack findProjection(ServerPlayer player) {
        int slot = findProjectionSlot(player);
        return slot >= 0 ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
    }

    private static int findProjectionSlot(ServerPlayer player) {
        var inventory = player.getInventory();
        UUID ownerId = player.getUUID();

        int main = findProjectionSlot(inventory.items, 0, ownerId);
        if (main >= 0) {
            return main;
        }
        int armor = findProjectionSlot(inventory.armor, 36, ownerId);
        if (armor >= 0) {
            return armor;
        }
        return findProjectionSlot(inventory.offhand, 40, ownerId);
    }

    private static int findProjectionSlot(
            net.minecraft.core.NonNullList<ItemStack> slots, int baseIndex, UUID ownerId) {
        for (int i = 0; i < slots.size(); i++) {
            if (isOwnedProjection(slots.get(i), ownerId)) {
                return baseIndex + i;
            }
        }
        return -1;
    }
}