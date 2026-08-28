package org.yanbwe.onegunlifetime.soul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mojang.serialization.DataResult;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.yanbwe.modularshoot.component.PluginInstance;

class SoulDataCodecTest {
    @Test
    void codecRoundTripsEmptySoulData() {
        SoulData original = SoulData.create(
                UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("modularshoot", "test_gun")
        );

        SoulData decoded = codecRoundTrip(original);

        assertEquals(original.ownerId(), decoded.ownerId());
        assertEquals(original.templateGunId(), decoded.templateGunId());
        assertEquals(original.statOverrides(), decoded.statOverrides());
        assertEquals(original.traits(), decoded.traits());
        assertEquals(original.plugins(), decoded.plugins());
        assertEquals(original.gunState(), decoded.gunState());
        assertEquals(original.version(), decoded.version());
        assertEquals(original.stableGunId(), decoded.stableGunId());
    }

    @Test
    void codecRoundTripsFullyPopulatedSoulData() {
        UUID ownerId = UUID.randomUUID();
        ResourceLocation template = ResourceLocation.fromNamespaceAndPath("modularshoot", "test_gun");
        ResourceLocation statKey = ResourceLocation.fromNamespaceAndPath("modularshoot", "damage");
        ResourceLocation traitA = ResourceLocation.fromNamespaceAndPath("modularshoot", "fast");
        ResourceLocation traitB = ResourceLocation.fromNamespaceAndPath("modularshoot", "heavy");
        ResourceLocation pluginId = ResourceLocation.fromNamespaceAndPath("modularshoot", "scope");
        ResourceLocation installedType = ResourceLocation.fromNamespaceAndPath("modularshoot", "sight");
        UUID pluginUuid = UUID.randomUUID();
        UUID stableGunId = UUID.randomUUID();

        CompoundTag gunState = new CompoundTag();
        gunState.putString("state", "value");
        gunState.putInt("heat", 7);

        SoulData original = new SoulData(
                ownerId,
                template,
                Map.of(statKey, 2.5d),
                Set.of(traitA, traitB),
                List.of(new PluginInstance(pluginId, pluginUuid, installedType, true)),
                gunState,
                1,
                stableGunId
        );

        SoulData decoded = codecRoundTrip(original);

        assertEquals(original, decoded);
        assertEquals(Set.of(statKey), decoded.statOverrides().keySet());
        assertEquals(2.5d, decoded.statOverrides().get(statKey));
        assertEquals(Set.of(traitA, traitB), decoded.traits());
        assertEquals(1, decoded.plugins().size());
        assertEquals(pluginId, decoded.plugins().get(0).pluginId());
        assertEquals(pluginUuid, decoded.plugins().get(0).instanceUuid());
        assertEquals(installedType, decoded.plugins().get(0).installedTypeId());
        assertEquals(true, decoded.plugins().get(0).locked());
        assertEquals(gunState, decoded.gunState());
    }

    @Test
    void streamCodecRoundTripsEmptyAndFullSoulData() {
        SoulData empty = SoulData.create(
                UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("modularshoot", "test_gun")
        );
        SoulData full = new SoulData(
                UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("modularshoot", "test_gun"),
                Map.of(ResourceLocation.fromNamespaceAndPath("modularshoot", "damage"), 3.0d),
                Set.of(ResourceLocation.fromNamespaceAndPath("modularshoot", "fast")),
                List.of(new PluginInstance(
                        ResourceLocation.fromNamespaceAndPath("modularshoot", "scope"),
                        UUID.randomUUID(),
                        ResourceLocation.fromNamespaceAndPath("modularshoot", "sight"),
                        false
                )),
                new CompoundTag(),
                1,
                UUID.randomUUID()
        );

        assertEquals(empty, streamRoundTrip(empty));
        assertEquals(full, streamRoundTrip(full));
    }

    private static SoulData codecRoundTrip(SoulData original) {
        DataResult<net.minecraft.nbt.Tag> encoded = SoulDataCodec.CODEC.encodeStart(NbtOps.INSTANCE, original);
        assertNotNull(encoded.result().orElse(null), "CODEC encode failed: " + encoded.error());
        DataResult<SoulData> decoded = SoulDataCodec.CODEC.parse(NbtOps.INSTANCE, encoded.result().orElseThrow());
        return decoded.result().orElseThrow();
    }

    private static SoulData streamRoundTrip(SoulData original) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        SoulDataCodec.STREAM_CODEC.encode(buf, original);
        return SoulDataCodec.STREAM_CODEC.decode(buf);
    }
}