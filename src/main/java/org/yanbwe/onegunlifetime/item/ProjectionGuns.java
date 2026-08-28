package org.yanbwe.onegunlifetime.item;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.onegunlifetime.def.SoulGunId;

/**
 * Single source of truth for recognising projection guns and locating them
 * inside a player's inventory.
 *
 * <p>Before this class the same "gun id -&gt; soul gun id -&gt; owner" chain was
 * implemented separately in {@code AssimilationService},
 * {@code ContainerGuard} and {@code AdminUnbindCommand}. All recognition and
 * slot lookup now funnels through here, and the public API
 * ({@code OneGunLifetimeAPI#getOwnerOf}) exposes it to other mods.</p>
 */
public final class ProjectionGuns {

    private ProjectionGuns() {
    }

    /**
     * Returns the soul owner UUID carried by a projection gun, if any.
     *
     * @param stack the stack to inspect; may be {@code null} or empty
     * @return the parsed owner UUID, or empty when the stack is not a
     *         OneGunLifetime projection gun
     */
    public static Optional<UUID> ownerOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return ModularShootAPI.getGunId(stack)
                .filter(SoulGunId::isSoulGunId)
                .flatMap(SoulGunId::parse);
    }

    /**
     * Returns whether the stack is a projection gun owned by the given player.
     *
     * @param stack   the stack to inspect; may be {@code null}
     * @param ownerId the soul owner UUID; may be {@code null}
     */
    public static boolean isOwnedBy(ItemStack stack, UUID ownerId) {
        if (stack == null || ownerId == null) {
            return false;
        }
        return ownerOf(stack).filter(ownerId::equals).isPresent();
    }

    /**
     * Finds the player's first projection gun, searching main inventory,
     * armor and offhand slots in that order.
     *
     * @return the projection gun stack, or {@link ItemStack#EMPTY} when absent
     */
    public static ItemStack find(ServerPlayer player) {
        int slot = findSlot(player);
        return slot >= 0 ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
    }

    /**
     * Finds the absolute inventory slot index of the player's first
     * projection gun (0-35 main, 36-39 armor, 40 offhand).
     *
     * @return the slot index, or {@code -1} when absent
     */
    public static int findSlot(ServerPlayer player) {
        var inventory = player.getInventory();
        UUID ownerId = player.getUUID();

        int main = findSlot(inventory.items, 0, ownerId);
        if (main >= 0) {
            return main;
        }
        int armor = findSlot(inventory.armor, 36, ownerId);
        if (armor >= 0) {
            return armor;
        }
        return findSlot(inventory.offhand, 40, ownerId);
    }

    /**
     * Writes a (modified) projection gun stack back into the slot it occupies.
     * No-op when no projection gun is present or the given stack is empty.
     */
    public static void writeBack(ServerPlayer player, ItemStack projection) {
        if (projection == null || projection.isEmpty()) {
            return;
        }
        int slot = findSlot(player);
        if (slot >= 0) {
            player.getInventory().setItem(slot, projection);
        }
    }

    private static int findSlot(NonNullList<ItemStack> slots, int baseIndex, UUID ownerId) {
        for (int i = 0; i < slots.size(); i++) {
            if (isOwnedBy(slots.get(i), ownerId)) {
                return baseIndex + i;
            }
        }
        return -1;
    }
}
