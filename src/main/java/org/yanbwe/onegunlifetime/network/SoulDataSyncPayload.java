package org.yanbwe.onegunlifetime.network;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.yanbwe.onegunlifetime.OneGunLifetime;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataCodec;

/**
 * Server-to-client soul data synchronisation payload.
 *
 * <p>The payload explicitly distinguishes two operations:</p>
 * <ul>
 *   <li>{@code souls} — bindings to insert/update. Each entry is keyed by its
 *       own {@link SoulData#ownerId()}, so a client simply replaces the
 *       existing entry for that owner.</li>
 *   <li>{@code removed} — owner UUIDs whose soul binding was deleted on the
 *       server (unbind). The client removes those entries from
 *       {@link org.yanbwe.onegunlifetime.client.ClientSoulDataStore}.</li>
 * </ul>
 *
 * @param souls   soul data entries to put/update
 * @param removed owner UUIDs to remove
 */
public record SoulDataSyncPayload(
        List<SoulData> souls,
        List<UUID> removed
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SoulDataSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(OneGunLifetime.MODID, "soul_data_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulDataSyncPayload> STREAM_CODEC =
            StreamCodec.of(SoulDataSyncPayload::encode, SoulDataSyncPayload::decode);

    public static SoulDataSyncPayload upsert(SoulData soul) {
        return new SoulDataSyncPayload(List.of(soul), List.of());
    }

    public static SoulDataSyncPayload upsertAll(List<SoulData> souls) {
        return new SoulDataSyncPayload(List.copyOf(souls), List.of());
    }

    public static SoulDataSyncPayload removal(UUID ownerId) {
        return new SoulDataSyncPayload(List.of(), List.of(ownerId));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, SoulDataSyncPayload payload) {
        buf.writeVarInt(payload.souls().size());
        for (SoulData soul : payload.souls()) {
            SoulDataCodec.STREAM_CODEC.encode(buf, soul);
        }
        buf.writeVarInt(payload.removed().size());
        for (UUID ownerId : payload.removed()) {
            UUIDUtil.STREAM_CODEC.encode(buf, ownerId);
        }
    }

    private static SoulDataSyncPayload decode(RegistryFriendlyByteBuf buf) {
        int soulCount = buf.readVarInt();
        var souls = new java.util.ArrayList<SoulData>(soulCount);
        for (int i = 0; i < soulCount; i++) {
            souls.add(SoulDataCodec.STREAM_CODEC.decode(buf));
        }

        int removedCount = buf.readVarInt();
        var removed = new java.util.ArrayList<UUID>(removedCount);
        for (int i = 0; i < removedCount; i++) {
            removed.add(UUIDUtil.STREAM_CODEC.decode(buf));
        }

        return new SoulDataSyncPayload(List.copyOf(souls), List.copyOf(removed));
    }
}