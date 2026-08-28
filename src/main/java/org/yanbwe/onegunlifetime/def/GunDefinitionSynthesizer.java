package org.yanbwe.onegunlifetime.def;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.yanbwe.onegunlifetime.OneGunLifetime;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.modularshoot.registry.gun.AttributeMount;
import org.yanbwe.modularshoot.registry.gun.GunDefinition;
import org.yanbwe.modularshoot.registry.gun.GunRegistry;
import org.yanbwe.modularshoot.registry.gun.ShootTextureMode;
import org.yanbwe.modularshoot.registry.gun.TextureScaleMode;

/**
 * Builds a {@link GunDefinition} for a soul-bound gun.
 *
 * <p>When the template id still resolves in the runtime registry the result is
 * the template with the player's stat overrides applied, the player's final
 * trait set substituted, and {@link AttributeMount#PLAYER} forced. When the
 * template is missing (e.g. a datapack was removed), a minimal placeholder
 * definition is returned so client-side queries degrade gracefully instead of
 * failing the whole gun lookup.</p>
 */
public final class GunDefinitionSynthesizer {
    private GunDefinitionSynthesizer() {
    }

    /**
     * Synthesises the player's effective gun definition.
     *
     * <p>Trait policy: {@link SoulData#traits()} is always the player's final
     * trait set. An empty set therefore means the soul gun has no final traits;
     * the template's traits are copied into a fresh binding at bind time, after
     * which the soul data is authoritative.</p>
     *
     * @param data           the soul data for the owner
     * @param registryAccess the runtime registry view used to resolve the
     *                       template; must not be {@code null}
     * @return the synthesised definition
     */
    public static GunDefinition synthesize(SoulData data, RegistryAccess registryAccess) {
        Optional<GunDefinition> templateOpt = GunRegistry.getGun(registryAccess, data.templateGunId());
        if (templateOpt.isEmpty()) {
            return missingTemplateFallback(data);
        }

        GunDefinition template = templateOpt.get();

        Map<ResourceLocation, Double> stats = new LinkedHashMap<>(template.stats());
        stats.putAll(data.statOverrides());

        Map<ResourceLocation, Boolean> traits = resolveTraits(data);

        return new GunDefinition(
                template.name(),
                template.texture(),
                template.shootTexture(),
                template.shootTextureMode(),
                template.textureScale(),
                stats,
                traits,
                template.slots(),
                template.sounds(),
                template.bulletStyle(),
                template.variants(),
                template.extraValues(),
                template.soundRange(),
                AttributeMount.PLAYER
        );
    }

    private static Map<ResourceLocation, Boolean> resolveTraits(SoulData data) {
        Map<ResourceLocation, Boolean> traits = new LinkedHashMap<>();
        for (ResourceLocation traitId : data.traits()) {
            traits.put(traitId, Boolean.TRUE);
        }
        return traits;
    }

    private static GunDefinition missingTemplateFallback(SoulData data) {
        Map<ResourceLocation, Boolean> traits = new LinkedHashMap<>();
        for (ResourceLocation traitId : data.traits()) {
            traits.put(traitId, Boolean.TRUE);
        }
        return new GunDefinition(
                Optional.of("Missing Template"),
                ResourceLocation.fromNamespaceAndPath(OneGunLifetime.MODID, "missing"),
                Optional.empty(),
                ShootTextureMode.PER_SHOT,
                TextureScaleMode.AUTO,
                new LinkedHashMap<>(data.statOverrides()),
                traits,
                Map.of(),
                Map.of(),
                Optional.empty(),
                Map.of(),
                Map.of(),
                Optional.empty(),
                AttributeMount.PLAYER
        );
    }
}