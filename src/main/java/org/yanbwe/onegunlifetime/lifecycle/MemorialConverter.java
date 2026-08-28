package org.yanbwe.onegunlifetime.lifecycle;

import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import org.yanbwe.modularshoot.component.ModularShootDataComponents;
import org.yanbwe.onegunlifetime.scan.AssimilationService;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Converts a projection gun picked up by an unbound player into a memorial.
 *
 * <p>When a non-owner who is already bound picks up a projection gun, the
 * existing inventory scanner handles assimilation. When the picker is unbound,
 * the gun is stripped of its ModularShoot gun/plugin components so it is no
 * longer a functional gun; the original owner receives a fresh projection gun
 * if they are online and bound.</p>
 */
public final class MemorialConverter {

    /** Translation key for the memorial item's custom name. */
    public static final String MEMORIAL_NAME_KEY = "onegunlifetime.item.memorial";

    private MemorialConverter() {
    }

    public static void onItemEntityPickupPost(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer picker)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        // Bound pickers are deliberately left to PlayerInventoryScanner.
        if (SoulDataManager.isBound(picker)) {
            return;
        }

        UUID pickerId = picker.getUUID();
        Inventory inventory = picker.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            ContainerGuard.ownerOf(stack).ifPresent(owner -> {
                if (owner.equals(pickerId)) {
                    return;
                }
                toMemorial(stack);
                restoreOwnerProjection(picker, owner);
            });
        }
    }

    /**
     * Strips a stack of every ModularShoot gun/plugin component and names it
     * as a memorial.
     *
     * <p>After this call the stack no longer has {@code gun_data}, so
     * {@code ModularShootAPI.getGunId} returns empty and OneGunLifetime scan
     * helpers no longer treat it as a projection gun.</p>
     *
     * @param stack the stack to convert; must not be empty
     */
    public static void toMemorial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(ModularShootDataComponents.GUN_DATA.get());
        stack.remove(ModularShootDataComponents.PLUGIN_DATA.get());
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(MEMORIAL_NAME_KEY));
    }

    private static void restoreOwnerProjection(ServerPlayer picker, UUID ownerId) {
        MinecraftServer server = picker.server;
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null || !SoulDataManager.isBound(owner)) {
            return;
        }
        AssimilationService.ensureProjection(owner, owner.level().registryAccess());
    }
}