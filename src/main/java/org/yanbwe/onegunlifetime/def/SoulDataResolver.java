package org.yanbwe.onegunlifetime.def;

import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.Nullable;
import org.yanbwe.onegunlifetime.soul.SoulData;

/**
 * Side adapter used by {@link DynamicGunDefinitionProvider} to obtain soul
 * data and the current registry view.
 *
 * <p>{@link net.minecraft.resources.ResourceLocation resource-location based}
 * gun definition queries carry no {@link RegistryAccess}, so the active side
 * must supply both the soul data source and the runtime registry access it
 * would have used for a normal gun lookup. The server resolver reads the
 * authoritative attachment data; the client resolver reads the store filled by
 * {@link org.yanbwe.onegunlifetime.network.SoulDataSyncPayload}.</p>
 */
public interface SoulDataResolver {
    /**
     * Returns the soul data for an owner, or {@code null} when unbound or the
     * current side has no such data.
     *
     * @param ownerId the owner's UUID
     * @return the soul data, or {@code null}
     */
    @Nullable
    SoulData resolve(UUID ownerId);

    /**
     * Returns the runtime registry view for the current side, or {@code null}
     * when no world/server is available (e.g. the main menu).
     *
     * @return the registry access, or {@code null}
     */
    @Nullable
    RegistryAccess registryAccess();
}