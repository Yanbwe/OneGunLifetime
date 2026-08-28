package org.yanbwe.onegunlifetime.network;

import java.util.UUID;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.yanbwe.onegunlifetime.client.ClientSoulDataStore;
import org.yanbwe.onegunlifetime.soul.SoulData;

/**
 * Client handler for {@link SoulDataSyncPayload}.
 *
 * <p>Applies upserts and removals to {@link ClientSoulDataStore}. Work is
 * scheduled through {@link IPayloadContext#enqueueWork} to guarantee it runs
 * on the client main thread (matching the RarityCore payload pattern).</p>
 */
public final class SoulDataSyncHandler {
    private SoulDataSyncHandler() {
    }

    public static void handle(SoulDataSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            for (SoulData soul : payload.souls()) {
                ClientSoulDataStore.put(soul);
            }
            for (UUID ownerId : payload.removed()) {
                ClientSoulDataStore.remove(ownerId);
            }
        });
    }
}