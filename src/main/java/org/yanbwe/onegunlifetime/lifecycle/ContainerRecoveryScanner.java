package org.yanbwe.onegunlifetime.lifecycle;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import org.yanbwe.onegunlifetime.scan.AssimilationService;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Low-frequency fallback scanner for projection guns that end up inside world
 * containers through paths the container guard cannot cancel (hoppers,
 * droppers, modded pipes, etc.).
 *
 * <p>NeoForge 1.21.1 has no generic event for hopper/automation insertion into
 * arbitrary containers. Instead of iterating every loaded block entity (which
 * is expensive and API-heavy), this scanner checks all containers within 8
 * blocks of every online player every 200 ticks. It only covers loaded chunks
 * near players, so containers far away are not swept; the menu listener in
 * {@link ContainerGuard} covers the player-opened container paths.</p>
 *
 * <p><strong>Known deviation:</strong> the current coverage is deliberately
 * “all containers within 8 blocks of online players”, not every loaded
 * container. A broad loaded-chunk BlockEntity sweep was not adopted because
 * NeoForge 1.21.1 does not expose a simple public API for enumerating all
 * loaded chunks/block entities, and a reflection/scan-every-chunk pass every
 * few seconds is too risky for performance. This design deviation is known and
 * accepted for now.</p>
 */
public final class ContainerRecoveryScanner {

    private static final int SCAN_INTERVAL_TICKS = 200;
    private static final int SCAN_RADIUS = 8;

    private ContainerRecoveryScanner() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            scanAroundPlayer(player);
        }
    }

    private static void scanAroundPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int radius = SCAN_RADIUS;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.getX() - radius,
                center.getY() - radius,
                center.getZ() - radius,
                center.getX() + radius,
                center.getY() + radius,
                center.getZ() + radius)) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!level.getBlockState(pos).hasBlockEntity()) {
                continue;
            }

            IItemHandler handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, pos, (Direction) null);
            if (handler == null) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                int currentSlot = slot;
                ContainerGuard.ownerOf(stack).ifPresent(owner -> {
                    ItemStack extracted = handler.extractItem(currentSlot, stack.getCount(), false);
                    if (!extracted.isEmpty()) {
                        returnToOwnerOrVoid(level, extracted, owner);
                    }
                });
            }
        }
    }

    private static void returnToOwnerOrVoid(ServerLevel level, ItemStack projection, UUID ownerId) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !SoulDataManager.isBound(owner)) {
            // Offline/unbound owners cannot receive the item; void it to keep
            // the world free of orphaned projection guns.
            return;
        }

        if (!owner.getInventory().add(projection.copy())) {
            // Inventory full: void the stack and let recovery re-create a gun
            // only if the owner truly has none.
            AssimilationService.ensureProjection(owner, owner.level().registryAccess());
        }
        owner.displayClientMessage(
                Component.translatable("onegunlifetime.actionbar.returned"), true);
    }
}