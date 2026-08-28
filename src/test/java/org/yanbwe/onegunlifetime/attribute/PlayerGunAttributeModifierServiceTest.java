package org.yanbwe.onegunlifetime.attribute;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mojang.serialization.Lifecycle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.junit.jupiter.api.Test;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.modularshoot.component.PluginInstance;
import org.yanbwe.modularshoot.plugin.PluginDefinition;
import org.yanbwe.modularshoot.plugin.PluginModifier;
import org.yanbwe.modularshoot.registry.ModularShootRegistries;
import org.yanbwe.modularshoot.registry.attribute.AttributeMeta;
import org.yanbwe.modularshoot.registry.gun.GunDefinition;
import org.yanbwe.modularshoot.registry.gun.ShootTextureMode;
import org.yanbwe.modularshoot.registry.gun.TextureScaleMode;

class PlayerGunAttributeModifierServiceTest {

    static {
        // FML shim + game-version shim, then full vanilla registry bootstrap.
        // Needed by AttributeMeta's BuiltInRegistries touch and by Attributes
        // holders used in the remove-path unit tests.
        net.neoforged.fml.loading.LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.setVersion(net.minecraft.DetectedVersion.BUILT_IN);
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static final ResourceLocation LOGICAL_DAMAGE =
            ResourceLocation.parse("modularshoot:hit_damage");
    private static final ResourceLocation BOUND_ATTACK =
            ResourceLocation.parse("minecraft:generic.attack_damage");
    private static final ResourceLocation GUN_ID =
            ResourceLocation.parse("test:soul_template_" + UUID.randomUUID());
    private static final ResourceLocation PLUGIN_A =
            ResourceLocation.parse("test:plugin_a_" + UUID.randomUUID());
    private static final ResourceLocation PLUGIN_B =
            ResourceLocation.parse("test:plugin_b_" + UUID.randomUUID());
    private static final ResourceLocation TYPE =
            ResourceLocation.parse("modularshoot:sight");

    @Test
    void calculateValueAddsBaseAndAddPluginModifiers() {
        SoulData data = soulData(
                List.of(
                        new PluginInstance(PLUGIN_A, UUID.randomUUID(), TYPE, false),
                        new PluginInstance(PLUGIN_B, UUID.randomUUID(), TYPE, false)));

        double value = PlayerGunAttributeModifierService.calculateValue(
                data, LOGICAL_DAMAGE, registryAccess());

        assertEquals(14.0, value, 1.0e-9,
                "base 10.0 + plugin ADD 2.5 + plugin ADD 1.5");
    }

    @Test
    void calculateValueReturnsZeroForMissingRegistryOrMissingMeta() {
        SoulData data = SoulData.create(
                UUID.randomUUID(),
                ResourceLocation.parse("test:missing_" + UUID.randomUUID()));

        double value = PlayerGunAttributeModifierService.calculateValue(
                data, LOGICAL_DAMAGE, RegistryAccess.EMPTY);

        assertEquals(0.0, value);
    }

    @Test
    void pluginModifierIdsAreStableAndOccurrenceDistinct() {
        ResourceLocation pluginId = ResourceLocation.parse("modularshoot:rapid_barrel");

        assertEquals(
                OneGunAttributeIds.pluginModifierId(pluginId, 0),
                OneGunAttributeIds.pluginModifierId(pluginId, 0));
        assertNotEquals(
                OneGunAttributeIds.pluginModifierId(pluginId, 0),
                OneGunAttributeIds.pluginModifierId(pluginId, 1));
    }

    @Test
    void pluginModifierIdsAreUnambiguousAcrossLengthPrefixes() {
        ResourceLocation ambiguousA = ResourceLocation.parse("a:b_c");
        ResourceLocation ambiguousB = ResourceLocation.parse("a_b:c");

        ResourceLocation idA = OneGunAttributeIds.pluginModifierId(ambiguousA, 0);
        ResourceLocation idB = OneGunAttributeIds.pluginModifierId(ambiguousB, 0);

        assertEquals("onegunlifetime", idA.getNamespace());
        assertEquals("plugin_1_a_3_b_c_0", idA.getPath());
        assertEquals("plugin_3_a_b_1_c_0", idB.getPath());
        assertNotEquals(idA, idB);
    }

    @Test
    void removeOwnedModifiersClearsOnlyOneGunLifetimeIds() {
        AttributeInstance instance =
                new AttributeInstance(Attributes.ATTACK_DAMAGE, ignored -> {
                });
        ResourceLocation other = ResourceLocation.parse("modularshoot:other");
        ResourceLocation pluginModifier =
                OneGunAttributeIds.pluginModifierId(ResourceLocation.parse("modularshoot:rapid"), 0);

        instance.addTransientModifier(
                new AttributeModifier(OneGunAttributeIds.BASE_MODIFIER_ID, 7.0,
                        AttributeModifier.Operation.ADD_VALUE));
        instance.addTransientModifier(
                new AttributeModifier(pluginModifier, 3.0,
                        AttributeModifier.Operation.ADD_VALUE));
        instance.addTransientModifier(
                new AttributeModifier(other, 1.0,
                        AttributeModifier.Operation.ADD_VALUE));

        PlayerGunAttributeModifierService.removeOwnedModifiers(instance);

        assertNull(instance.getModifier(OneGunAttributeIds.BASE_MODIFIER_ID));
        assertNull(instance.getModifier(pluginModifier));
        assertEquals(1.0, instance.getModifier(other).amount());
    }

    @Test
    void removeDoesNotThrowForNullPlayerOrEmptyInstance() {
        assertDoesNotThrow(() -> PlayerGunAttributeModifierService.remove(null));
        assertDoesNotThrow(
                () -> PlayerGunAttributeModifierService.removeOwnedModifiers(null));
        assertDoesNotThrow(
                () -> PlayerGunAttributeModifierService.removeOwnedModifiers(
                        new AttributeInstance(Attributes.ATTACK_DAMAGE, ignored -> {
                        })));
    }

    private static SoulData soulData(List<PluginInstance> plugins) {
        return new SoulData(
                UUID.randomUUID(),
                GUN_ID,
                Map.of(),
                Set.of(),
                plugins,
                new CompoundTag(),
                1,
                UUID.randomUUID());
    }

    private static RegistryAccess registryAccess() {
        MappedRegistry<GunDefinition> guns =
                new MappedRegistry<>(ModularShootRegistries.GUNS_KEY, Lifecycle.stable());
        Registry.register(guns, GUN_ID, gunDefinition());

        MappedRegistry<PluginDefinition> plugins =
                new MappedRegistry<>(ModularShootRegistries.PLUGINS_KEY, Lifecycle.stable());
        Registry.register(plugins, PLUGIN_A, pluginDefinition(2.5));
        Registry.register(plugins, PLUGIN_B, pluginDefinition(1.5));

        MappedRegistry<AttributeMeta> metas =
                new MappedRegistry<>(ModularShootRegistries.ATTRIBUTE_META_KEY, Lifecycle.stable());
        Registry.register(metas, LOGICAL_DAMAGE, AttributeMeta.of(BOUND_ATTACK, 5.0));

        return new RegistryAccess.ImmutableRegistryAccess(List.of(guns, plugins, metas));
    }

    private static GunDefinition gunDefinition() {
        return new GunDefinition(
                Optional.empty(),
                ResourceLocation.parse("modularshoot:textures/gun/base.png"),
                Optional.empty(),
                ShootTextureMode.PER_SHOT,
                TextureScaleMode.AUTO,
                Map.of(LOGICAL_DAMAGE, 10.0),
                Map.of(),
                Map.of(),
                Map.of(),
                Optional.empty(),
                Map.of(),
                Map.of(),
                Optional.empty());
    }

    private static PluginDefinition pluginDefinition(double value) {
        return new PluginDefinition(
                List.of(),
                0,
                ResourceLocation.parse("modularshoot:textures/plugin/icon.png"),
                TextureScaleMode.AUTO,
                List.of(new PluginModifier(BOUND_ATTACK.toString(),
                        PluginModifier.Operation.ADD, value)),
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                Map.of(),
                Optional.empty());
    }
}