package org.yanbwe.onegunlifetime.plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.component.PluginInstance;
import org.yanbwe.modularshoot.plugin.event.PostPluginInstallEvent;
import org.yanbwe.modularshoot.plugin.event.PostPluginUninstallEvent;
import org.yanbwe.onegunlifetime.def.SoulGunId;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Keeps soul data in sync with ModularShoot plugin changes on projection guns.
 *
 * <p>Both {@link PostPluginInstallEvent} and {@link PostPluginUninstallEvent}
 * fire on the client and the server. This handler performs writes only when it
 * can resolve the event to a {@link ServerPlayer}; client-side events are
 * deliberately ignored because the server-side event is authoritative.</p>
 */
public final class PluginSyncHandler {

    private PluginSyncHandler() {
    }

    /**
     * Listens for post-install events and writes the gun's final plugin list
     * back to the owner's soul data.
     */
    public static void onPluginInstall(PostPluginInstallEvent event) {
        handle(event.getGun(), event.getPlayer());
    }

    /**
     * Listens for post-uninstall events and writes the gun's final plugin list
     * back to the owner's soul data.
     */
    public static void onPluginUninstall(PostPluginUninstallEvent event) {
        handle(event.getGun(), event.getPlayer());
    }

    private static void handle(ItemStack gun, Player eventPlayer) {
        if (gun == null || gun.isEmpty()) {
            return;
        }

        Optional<UUID> ownerIdOpt = ModularShootAPI.getGunId(gun)
                .filter(SoulGunId::isSoulGunId)
                .flatMap(SoulGunId::parse);
        if (ownerIdOpt.isEmpty()) {
            return;
        }
        UUID ownerId = ownerIdOpt.get();

        ServerPlayer player = resolveServerPlayer(eventPlayer, ownerId);
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (!ownerId.equals(player.getUUID()) || !SoulDataManager.isBound(player)) {
            return;
        }

        List<PluginInstance> latestPlugins = ModularShootAPI.getInstalledPlugins(gun);
        SoulDataManager.setPlugins(player, latestPlugins);
        PluginRefresher.refresh(player);
    }

    /**
     * Resolves the authoritative server player for a plugin event.
     *
     * <p>An explicit non-server player means the event was fired on the client
     * side; it is skipped here and handled by the corresponding server-side
     * event. A {@code null} event player (possible on uninstall) falls back to
     * the owner UUID lookup.</p>
     */
    private static ServerPlayer resolveServerPlayer(Player eventPlayer, UUID ownerId) {
        if (eventPlayer instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        if (eventPlayer != null) {
            return null;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getPlayerList().getPlayer(ownerId) : null;
    }
}