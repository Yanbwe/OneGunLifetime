package org.yanbwe.onegunlifetime.lifecycle;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import org.jetbrains.annotations.Nullable;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.onegunlifetime.def.SoulGunId;
import org.yanbwe.onegunlifetime.scan.AssimilationService;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Guards projection guns against entering world containers.
 *
 * <p>NeoForge 1.21.1 does not provide a pre-click event for every ordinary
 * container click or shift-click. This guard therefore uses the available
 * interception points plus a post-change container listener attached while a
 * menu is open:</p>
 *
 * <ul>
 *   <li>{@link ItemStackedOnOtherEvent} cancels bundle-style interactions that
 *       would place a carried projection gun into a container slot.</li>
 *   <li>{@link PlayerContainerEvent.Open} scans the already-open container and
 *       installs a {@link ContainerListener}; whenever a non-player slot in
 *       that menu receives a projection gun, it is removed immediately and
 *       returned to its owner (or voided when the owner is offline).</li>
 * </ul>
 *
 * <p>Automation paths such as hoppers, droppers and modded pipes have no
 * NeoForge 1.21.1 cancellation event. Those are covered by
 * {@link ContainerRecoveryScanner} as a low-frequency fallback.</p>
 */
public final class ContainerGuard {

    private static final Map<UUID, MenuGuard> ACTIVE_GUARDS = new ConcurrentHashMap<>();

    private ContainerGuard() {
    }

    /**
     * Returns the soul owner UUID carried by a projection gun, if any.
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
     * Returns whether the stack is a projection gun, optionally filtering by
     * owner.
     *
     * @param stack the stack to inspect
     * @param owner the expected owner, or {@code null} to accept any projection
     *              gun
     */
    public static boolean isProjectionGun(ItemStack stack, @Nullable UUID owner) {
        UUID parsed = ownerOf(stack).orElse(null);
        return parsed != null && (owner == null || owner.equals(parsed));
    }

    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }

        AbstractContainerMenu menu = event.getContainer();
        MenuGuard previous = ACTIVE_GUARDS.remove(player.getUUID());
        if (previous != null) {
            previous.menu().removeSlotListener(previous.listener());
        }

        removeProjectionGunsFromNonPlayerSlots(player, menu);

        ContainerListener listener = new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu changedMenu, int slotIndex, ItemStack itemStack) {
                onSlotChanged(player, changedMenu, slotIndex);
            }

            @Override
            public void dataChanged(AbstractContainerMenu changedMenu, int dataSlotIndex, int value) {
                // No data-slot action needed.
            }
        };
        menu.addSlotListener(listener);
        ACTIVE_GUARDS.put(player.getUUID(), new MenuGuard(menu, listener));
    }

    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MenuGuard guard = ACTIVE_GUARDS.remove(player.getUUID());
        if (guard != null) {
            guard.menu().removeSlotListener(guard.listener());
        }
    }

    public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Slot slot = event.getSlot();
        if (slot == null || slot.container instanceof Inventory) {
            return;
        }

        ItemStack carried = event.getCarriedItem();
        Optional<UUID> owner = ownerOf(carried);
        if (owner.isEmpty()) {
            return;
        }

        event.setCanceled(true);
        if (serverPlayer.getInventory().add(carried.copy())) {
            event.getCarriedSlotAccess().set(ItemStack.EMPTY);
            serverPlayer.displayClientMessage(
                    Component.translatable("onegunlifetime.actionbar.returned"), true);
        }
    }

    private static void onSlotChanged(ServerPlayer player, AbstractContainerMenu menu, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.getSlot(slotIndex);
        if (slot.container instanceof Inventory) {
            return;
        }

        ItemStack stack = slot.getItem();
        ownerOf(stack).ifPresent(owner -> {
            slot.set(ItemStack.EMPTY);
            returnToOwner(player, stack, owner);
        });
    }

    private static void removeProjectionGunsFromNonPlayerSlots(ServerPlayer player, AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            ownerOf(stack).ifPresent(owner -> {
                slot.set(ItemStack.EMPTY);
                returnToOwner(player, stack, owner);
            });
        }
    }

    private static void returnToOwner(ServerPlayer operator, ItemStack projection, UUID ownerId) {
        ServerPlayer owner = operator.server.getPlayerList().getPlayer(ownerId);
        if (owner == null || !SoulDataManager.isBound(owner)) {
            // Offline or unbound owners cannot receive the item; void it to
            // preserve the one-projection-gun invariant.
            return;
        }

        if (!owner.getInventory().add(projection.copy())) {
            // Inventory full: void the recovered stack and let the recovery
            // path re-create a gun if the owner actually has none.
            AssimilationService.ensureProjection(owner, owner.level().registryAccess());
        }
        owner.displayClientMessage(
                Component.translatable("onegunlifetime.actionbar.returned"), true);
    }

    private record MenuGuard(AbstractContainerMenu menu, ContainerListener listener) {
    }
}