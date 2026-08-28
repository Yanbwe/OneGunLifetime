package org.yanbwe.onegunlifetime.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import org.yanbwe.onegunlifetime.soul.SoulData;

/**
 * Client-side mirror of the server's soul bindings.
 *
 * <p>Entries are installed by the {@code soul_data_sync} payload and read by
 * {@link org.yanbwe.onegunlifetime.def.ClientSoulDataResolver} when ModularShoot
 * queries a dynamic soul gun definition on the client.</p>
 */
public final class ClientSoulDataStore {
    private static final ConcurrentHashMap<UUID, SoulData> SOULS = new ConcurrentHashMap<>();

    private ClientSoulDataStore() {
    }

    public static void put(SoulData data) {
        SOULS.put(data.ownerId(), data);
    }

    public static void remove(UUID ownerId) {
        SOULS.remove(ownerId);
    }

    @Nullable
    public static SoulData get(UUID ownerId) {
        return SOULS.get(ownerId);
    }

    public static Map<UUID, SoulData> getAll() {
        return Map.copyOf(SOULS);
    }

    public static void clear() {
        SOULS.clear();
    }
}