package org.yanbwe.onegunlifetime.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.yanbwe.modularshoot.component.PluginInstance;

/**
 * Codec and stream codec for {@link SoulData}.
 *
 * <p>{@link #CODEC} is used by the NeoForge attachment for NBT persistence.
 * {@link #STREAM_CODEC} is provided for the later network-sync stage and
 * reuses the same field order as the NBT codec.</p>
 */
public final class SoulDataCodec {
    private SoulDataCodec() {
    }

    public static final Codec<SoulData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner_id").forGetter(SoulData::ownerId),
            ResourceLocation.CODEC.fieldOf("template_gun_id").forGetter(SoulData::templateGunId),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE)
                    .optionalFieldOf("stat_overrides", Map.of())
                    .forGetter(SoulData::statOverrides),
            ResourceLocation.CODEC.listOf()
                    .xmap(list -> Set.copyOf(list), set -> List.copyOf(set))
                    .optionalFieldOf("traits", Set.of())
                    .forGetter(SoulData::traits),
            PluginInstance.CODEC.listOf()
                    .optionalFieldOf("plugins", List.of())
                    .forGetter(SoulData::plugins),
            CompoundTag.CODEC
                    .optionalFieldOf("gun_state", new CompoundTag())
                    .forGetter(SoulData::gunState),
            Codec.INT.optionalFieldOf("version", 1).forGetter(SoulData::version),
            UUIDUtil.CODEC.fieldOf("stable_gun_id").forGetter(SoulData::stableGunId)
    ).apply(instance, SoulData::new));

    /**
     * Stream codec for the later client-synchronisation stage.
     *
     * <p>There is no ModularShoot-provided stream codec for
     * {@link PluginInstance}, so plugin fields are written explicitly.</p>
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulData> STREAM_CODEC =
            StreamCodec.of(SoulDataCodec::encode, SoulDataCodec::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SoulData data) {
        UUIDUtil.STREAM_CODEC.encode(buf, data.ownerId());
        ResourceLocation.STREAM_CODEC.encode(buf, data.templateGunId());

        buf.writeVarInt(data.statOverrides().size());
        for (Map.Entry<ResourceLocation, Double> entry : data.statOverrides().entrySet()) {
            ResourceLocation.STREAM_CODEC.encode(buf, entry.getKey());
            ByteBufCodecs.DOUBLE.encode(buf, entry.getValue());
        }

        buf.writeVarInt(data.traits().size());
        for (ResourceLocation trait : data.traits()) {
            ResourceLocation.STREAM_CODEC.encode(buf, trait);
        }

        buf.writeVarInt(data.plugins().size());
        for (PluginInstance plugin : data.plugins()) {
            ResourceLocation.STREAM_CODEC.encode(buf, plugin.pluginId());
            UUIDUtil.STREAM_CODEC.encode(buf, plugin.instanceUuid());
            ResourceLocation.STREAM_CODEC.encode(buf, plugin.installedTypeId());
            ByteBufCodecs.BOOL.encode(buf, plugin.locked());
        }

        buf.writeNbt(data.gunState());
        buf.writeVarInt(data.version());
        UUIDUtil.STREAM_CODEC.encode(buf, data.stableGunId());
    }

    private static SoulData decode(RegistryFriendlyByteBuf buf) {
        UUID ownerId = UUIDUtil.STREAM_CODEC.decode(buf);
        ResourceLocation templateGunId = ResourceLocation.STREAM_CODEC.decode(buf);

        int statSize = buf.readVarInt();
        Map<ResourceLocation, Double> statOverrides = new HashMap<>();
        for (int i = 0; i < statSize; i++) {
            ResourceLocation key = ResourceLocation.STREAM_CODEC.decode(buf);
            double value = ByteBufCodecs.DOUBLE.decode(buf);
            statOverrides.put(key, value);
        }

        int traitSize = buf.readVarInt();
        Set<ResourceLocation> traits = new HashSet<>();
        for (int i = 0; i < traitSize; i++) {
            traits.add(ResourceLocation.STREAM_CODEC.decode(buf));
        }

        int pluginSize = buf.readVarInt();
        List<PluginInstance> plugins = new ArrayList<>();
        for (int i = 0; i < pluginSize; i++) {
            ResourceLocation pluginId = ResourceLocation.STREAM_CODEC.decode(buf);
            UUID instanceUuid = UUIDUtil.STREAM_CODEC.decode(buf);
            ResourceLocation installedTypeId = ResourceLocation.STREAM_CODEC.decode(buf);
            boolean locked = ByteBufCodecs.BOOL.decode(buf);
            plugins.add(new PluginInstance(pluginId, instanceUuid, installedTypeId, locked));
        }

        CompoundTag gunState = buf.readNbt();
        int version = buf.readVarInt();
        UUID stableGunId = UUIDUtil.STREAM_CODEC.decode(buf);

        return new SoulData(
                ownerId,
                templateGunId,
                statOverrides,
                traits,
                plugins,
                gunState != null ? gunState : new CompoundTag(),
                version,
                stableGunId
        );
    }
}