package org.yanbwe.onegunlifetime.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.modularshoot.registry.gun.AttributeMount;
import org.yanbwe.modularshoot.registry.gun.BulletStyle;
import org.yanbwe.modularshoot.registry.gun.GunDefinition;
import org.yanbwe.modularshoot.registry.gun.GunRegistry;
import org.yanbwe.modularshoot.registry.gun.ShootTextureMode;
import org.yanbwe.modularshoot.registry.gun.TextureScaleMode;

class GunDefinitionSynthesizerTest {

    private static final ResourceLocation DAMAGE = ResourceLocation.fromNamespaceAndPath("modularshoot", "damage");
    private static final ResourceLocation FIRE_RATE = ResourceLocation.fromNamespaceAndPath("modularshoot", "fire_rate");
    private static final ResourceLocation BASE_TRAIT = ResourceLocation.fromNamespaceAndPath("modularshoot", "base_trait");
    private static final ResourceLocation PLAYER_TRAIT = ResourceLocation.fromNamespaceAndPath("modularshoot", "player_trait");
    private static final ResourceLocation SLOT = ResourceLocation.fromNamespaceAndPath("modularshoot", "sight");
    private static final ResourceLocation VARIANT = ResourceLocation.fromNamespaceAndPath("modularshoot", "variant");
    private static final ResourceLocation EXTRA = ResourceLocation.fromNamespaceAndPath("my_addon", "extra");

    @Test
    void synthesizesFromTemplateWithOverridesAndForcedPlayerMount() {
        ResourceLocation templateId = uniqueTemplateId();
        GunDefinition template = template(templateId);
        GunRegistry.registerGun(templateId, template);

        SoulData soul = new SoulData(
                UUID.randomUUID(),
                templateId,
                Map.of(DAMAGE, 99.0d, FIRE_RATE, 3.5d),
                Set.of(PLAYER_TRAIT),
                List.of(),
                new net.minecraft.nbt.CompoundTag(),
                1,
                UUID.randomUUID()
        );

        GunDefinition result = GunDefinitionSynthesizer.synthesize(soul, RegistryAccess.EMPTY);

        assertEquals(AttributeMount.PLAYER, result.attributeMount());
        assertEquals(Optional.of("Template Gun"), result.name());
        assertEquals(template.texture(), result.texture());
        assertEquals(template.shootTexture(), result.shootTexture());
        assertEquals(template.slots(), result.slots());
        assertEquals(template.sounds(), result.sounds());
        assertEquals(template.variants(), result.variants());
        assertEquals(template.extraValues(), result.extraValues());
        assertEquals(template.bulletStyle(), result.bulletStyle());

        // Player overrides replace template values; new override keys are merged.
        assertEquals(99.0d, result.stats().get(DAMAGE));
        assertEquals(3.5d, result.stats().get(FIRE_RATE));
        assertEquals(2, result.stats().size());
    }

    @Test
    void nonEmptyPlayerTraitsFormTheFinalTraitMap() {
        ResourceLocation templateId = uniqueTemplateId();
        GunRegistry.registerGun(templateId, template(templateId));

        SoulData soul = new SoulData(
                UUID.randomUUID(),
                templateId,
                Map.of(),
                Set.of(PLAYER_TRAIT),
                List.of(),
                new net.minecraft.nbt.CompoundTag(),
                1,
                UUID.randomUUID()
        );

        GunDefinition result = GunDefinitionSynthesizer.synthesize(soul, RegistryAccess.EMPTY);

        assertTrue(result.traits().get(PLAYER_TRAIT));
        assertNull(result.traits().get(BASE_TRAIT), "player's final trait set should replace the template traits");
        assertEquals(1, result.traits().size());
    }

    @Test
    void emptyPlayerTraitsYieldEmptyFinalTraitMap() {
        ResourceLocation templateId = uniqueTemplateId();
        GunRegistry.registerGun(templateId, template(templateId));

        SoulData soul = new SoulData(
                UUID.randomUUID(),
                templateId,
                Map.of(),
                Set.of(),
                List.of(),
                new net.minecraft.nbt.CompoundTag(),
                1,
                UUID.randomUUID()
        );

        GunDefinition result = GunDefinitionSynthesizer.synthesize(soul, RegistryAccess.EMPTY);

        assertTrue(result.traits().isEmpty(), "empty soul trait set must mean no final traits");
    }

    @Test
    void missingTemplateReturnsSafeFallbackWithPlayerMount() {
        ResourceLocation missingId = ResourceLocation.fromNamespaceAndPath("test", "missing_" + UUID.randomUUID());
        SoulData soul = SoulData.create(UUID.randomUUID(), missingId);

        GunDefinition result = GunDefinitionSynthesizer.synthesize(soul, RegistryAccess.EMPTY);

        assertEquals(AttributeMount.PLAYER, result.attributeMount());
        assertEquals(ResourceLocation.fromNamespaceAndPath("onegunlifetime", "missing"), result.texture());
        assertEquals(Optional.of("Missing Template"), result.name());
        assertTrue(result.stats().isEmpty());
        assertTrue(result.traits().isEmpty());
        assertTrue(result.slots().isEmpty());
        assertTrue(result.sounds().isEmpty());
        assertTrue(result.shootTexture().isEmpty());
        assertTrue(result.bulletStyle().isEmpty());
        assertFalse(result.soundRange().isPresent());
    }

    @Test
    void missingTemplateKeepsPlayerOverridesAndTraits() {
        ResourceLocation missingId = ResourceLocation.fromNamespaceAndPath("test", "missing_" + UUID.randomUUID());
        SoulData soul = new SoulData(
                UUID.randomUUID(),
                missingId,
                Map.of(DAMAGE, 123.0d),
                Set.of(PLAYER_TRAIT),
                List.of(),
                new net.minecraft.nbt.CompoundTag(),
                1,
                UUID.randomUUID()
        );

        GunDefinition result = GunDefinitionSynthesizer.synthesize(soul, RegistryAccess.EMPTY);

        assertEquals(AttributeMount.PLAYER, result.attributeMount());
        assertEquals(123.0d, result.stats().get(DAMAGE));
        assertTrue(result.traits().get(PLAYER_TRAIT));
        assertNull(result.traits().get(BASE_TRAIT));
    }

    private static GunDefinition template(ResourceLocation id) {
        return new GunDefinition(
                Optional.of("Template Gun"),
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/gun/template.png"),
                Optional.of(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/gun/template_shoot.png")),
                ShootTextureMode.WHILE_FIRING,
                TextureScaleMode.FIXED,
                Map.of(DAMAGE, 10.0d),
                Map.of(BASE_TRAIT, true),
                Map.of(SLOT, 2),
                Map.of("shoot", ResourceLocation.fromNamespaceAndPath("minecraft", "entity.shoot")),
                Optional.of(new BulletStyle(Optional.empty(), List.of())),
                Map.of(VARIANT, 1.0d),
                Map.of(EXTRA, 7.0d),
                Optional.of(32.0f),
                AttributeMount.ITEM
        );
    }

    private static ResourceLocation uniqueTemplateId() {
        return ResourceLocation.fromNamespaceAndPath("test", "template_" + UUID.randomUUID());
    }
}