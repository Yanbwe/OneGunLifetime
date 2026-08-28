package org.yanbwe.onegunlifetime.def;

import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Server-side {@link SoulDataResolver}.
 *
 * <p>Data is read from the authority: the {@link SoulDataManager} attachment
 * on the {@link ServerLifecycleHooks current server's} player list. The
 * registry access is likewise the live server registry, so dynamic definitions
 * are always synthesised from the same datapack/Java-API view the server
 * uses.</p>
 */
public final class ServerSoulDataResolver implements SoulDataResolver {

    @Override
    @Nullable
    public SoulData resolve(UUID ownerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        var player = server.getPlayerList().getPlayer(ownerId);
        return player == null ? null : SoulDataManager.get(player);
    }

    @Override
    @Nullable
    public RegistryAccess registryAccess() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }
}