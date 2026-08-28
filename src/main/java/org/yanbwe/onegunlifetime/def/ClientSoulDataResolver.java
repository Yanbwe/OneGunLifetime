package org.yanbwe.onegunlifetime.def;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.Nullable;
import org.yanbwe.onegunlifetime.client.ClientSoulDataStore;
import org.yanbwe.onegunlifetime.soul.SoulData;

/**
 * Client-side {@link SoulDataResolver}.
 *
 * <p>This class references Minecraft client classes and must only be loaded
 * on the physical client. It is installed from
 * {@link org.yanbwe.onegunlifetime.OneGunLifetimeClient}, whose
 * {@code @Mod(dist = Dist.CLIENT)} class is only constructed on the client
 * distribution.</p>
 */
public final class ClientSoulDataResolver implements SoulDataResolver {

    @Override
    @Nullable
    public SoulData resolve(UUID ownerId) {
        return ClientSoulDataStore.get(ownerId);
    }

    @Override
    @Nullable
    public RegistryAccess registryAccess() {
        var level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess();
    }
}