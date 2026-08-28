package org.yanbwe.onegunlifetime.soul;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.yanbwe.modularshoot.component.PluginInstance;

class SoulDataVersionTest {
    @Test
    void createUsesCurrentVersion() {
        SoulData data = SoulData.create(
                UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("modularshoot", "test_gun")
        );

        assertEquals(SoulDataVersion.CURRENT, data.version());
    }

    @Test
    void currentVersionIsReturnedUnchanged() {
        SoulData current = SoulData.create(
                UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("modularshoot", "test_gun")
        );

        assertSame(current, SoulDataMigration.upgrade(current));
    }

    @Test
    void olderVersionIsLeftUntouchedForStageOne() {
        SoulData older = new SoulData(
                UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("modularshoot", "test_gun"),
                Map.of(ResourceLocation.fromNamespaceAndPath("modularshoot", "damage"), 1.0d),
                Set.of(ResourceLocation.fromNamespaceAndPath("modularshoot", "fast")),
                List.of(new PluginInstance(
                        ResourceLocation.fromNamespaceAndPath("modularshoot", "scope"),
                        UUID.randomUUID(),
                        ResourceLocation.fromNamespaceAndPath("modularshoot", "sight"),
                        false
                )),
                new CompoundTag(),
                0,
                UUID.randomUUID()
        );

        assertSame(older, SoulDataMigration.upgrade(older));
    }

    @Test
    void unknownFutureVersionIsDowngradedButKeepsBindingIdentity() {
        UUID ownerId = UUID.randomUUID();
        ResourceLocation templateGunId = ResourceLocation.fromNamespaceAndPath("modularshoot", "test_gun");
        UUID stableGunId = UUID.randomUUID();
        SoulData future = new SoulData(
                ownerId,
                templateGunId,
                Map.of(ResourceLocation.fromNamespaceAndPath("modularshoot", "damage"), 99.0d),
                Set.of(ResourceLocation.fromNamespaceAndPath("modularshoot", "fast")),
                List.of(new PluginInstance(
                        ResourceLocation.fromNamespaceAndPath("modularshoot", "scope"),
                        UUID.randomUUID(),
                        ResourceLocation.fromNamespaceAndPath("modularshoot", "sight"),
                        false
                )),
                new CompoundTag(),
                999,
                stableGunId
        );

        assertDoesNotThrow(() -> SoulDataMigration.upgrade(future));
        SoulData upgraded = SoulDataMigration.upgrade(future);

        assertEquals(SoulDataVersion.CURRENT, upgraded.version());
        assertEquals(ownerId, upgraded.ownerId());
        assertEquals(templateGunId, upgraded.templateGunId());
        assertEquals(stableGunId, upgraded.stableGunId());
        assertTrue(upgraded.statOverrides().isEmpty());
        assertTrue(upgraded.traits().isEmpty());
        assertTrue(upgraded.plugins().isEmpty());
        assertTrue(upgraded.gunState().isEmpty());
    }
}