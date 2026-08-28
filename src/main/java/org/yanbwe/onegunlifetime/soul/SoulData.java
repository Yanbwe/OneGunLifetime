package org.yanbwe.onegunlifetime.soul;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.yanbwe.modularshoot.component.PluginInstance;

/**
 * Immutable soul-bound gun data stored on a player.
 *
 * <p>This record is the single source of truth for what a player's bound gun
 * is: the original template definition, player-owned stat overrides, active
 * traits, installed plugins, runtime gun state and the stable gun instance id.
 * Every mutating helper returns a new instance; the original is never
 * modified.</p>
 *
 * <p>The {@link CompoundTag} held by {@link #gunState()} is defensively copied
 * at construction time to keep the record immutable even if a caller later
 * mutates the tag they supplied.</p>
 *
 * @param ownerId       the owning player's UUID
 * @param templateGunId the original ModularShoot gun definition id used as the
 *                      binding template
 * @param statOverrides player-custom stat value overrides keyed by stat id
 * @param traits        player-active trait ids
 * @param plugins       ordered installed plugin list
 * @param gunState      runtime per-gun state payload (NBT)
 * @param version       soul data schema version; see {@link SoulDataVersion}
 * @param stableGunId   stable UUID for this player's soul gun
 */
public record SoulData(
        UUID ownerId,
        ResourceLocation templateGunId,
        Map<ResourceLocation, Double> statOverrides,
        Set<ResourceLocation> traits,
        List<PluginInstance> plugins,
        CompoundTag gunState,
        int version,
        UUID stableGunId
) {
    public SoulData {
        statOverrides = Map.copyOf(statOverrides);
        traits = Set.copyOf(traits);
        plugins = List.copyOf(plugins);
        gunState = gunState.copy();
    }

    /**
     * Creates a fresh soul binding for a player: empty overrides/traits/plugins,
     * empty gun state, current schema version and a newly generated stable gun id.
     *
     * @param ownerId       the owning player's UUID
     * @param templateGunId the template gun definition id
     * @return a new, empty {@link SoulData}
     */
    public static SoulData create(UUID ownerId, ResourceLocation templateGunId) {
        return new SoulData(
                ownerId,
                templateGunId,
                Map.of(),
                Set.of(),
                List.of(),
                new CompoundTag(),
                SoulDataVersion.CURRENT,
                UUID.randomUUID()
        );
    }

    public SoulData withStatOverride(ResourceLocation key, double value) {
        Map<ResourceLocation, Double> newOverrides = new LinkedHashMap<>(statOverrides);
        newOverrides.put(key, value);
        return new SoulData(ownerId, templateGunId, newOverrides, traits, plugins, gunState, version, stableGunId);
    }

    public SoulData withoutStatOverride(ResourceLocation key) {
        if (!statOverrides.containsKey(key)) {
            return this;
        }
        Map<ResourceLocation, Double> newOverrides = new LinkedHashMap<>(statOverrides);
        newOverrides.remove(key);
        return new SoulData(ownerId, templateGunId, newOverrides, traits, plugins, gunState, version, stableGunId);
    }

    public SoulData clearStatOverrides() {
        if (statOverrides.isEmpty()) {
            return this;
        }
        return new SoulData(ownerId, templateGunId, Map.of(), traits, plugins, gunState, version, stableGunId);
    }

    public SoulData withTrait(ResourceLocation traitId) {
        Set<ResourceLocation> newTraits = new LinkedHashSet<>(traits);
        newTraits.add(traitId);
        return new SoulData(ownerId, templateGunId, statOverrides, newTraits, plugins, gunState, version, stableGunId);
    }

    public SoulData withoutTrait(ResourceLocation traitId) {
        if (!traits.contains(traitId)) {
            return this;
        }
        Set<ResourceLocation> newTraits = new LinkedHashSet<>(traits);
        newTraits.remove(traitId);
        return new SoulData(ownerId, templateGunId, statOverrides, newTraits, plugins, gunState, version, stableGunId);
    }

    public SoulData withTraits(Collection<ResourceLocation> newTraits) {
        return new SoulData(ownerId, templateGunId, statOverrides, new LinkedHashSet<>(newTraits), plugins, gunState, version, stableGunId);
    }

    public SoulData withPlugins(List<PluginInstance> newPlugins) {
        return new SoulData(ownerId, templateGunId, statOverrides, traits, newPlugins, gunState, version, stableGunId);
    }

    public SoulData withGunState(CompoundTag newGunState) {
        return new SoulData(ownerId, templateGunId, statOverrides, traits, plugins, newGunState, version, stableGunId);
    }

    public SoulData withVersion(int newVersion) {
        return new SoulData(ownerId, templateGunId, statOverrides, traits, plugins, gunState, newVersion, stableGunId);
    }
}