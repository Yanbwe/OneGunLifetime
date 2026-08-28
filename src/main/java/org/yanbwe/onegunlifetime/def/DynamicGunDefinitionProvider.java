package org.yanbwe.onegunlifetime.def;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.yanbwe.modularshoot.registry.gun.GunDefinition;
import org.yanbwe.modularshoot.registry.gun.GunDefinitionProvider;

/**
 * ModularShoot dynamic provider for {@code onegunlifetime:soul_*} gun ids.
 *
 * <p>The provider is registered during mod construction. Every query:
 * <ol>
 *   <li>parses the owner UUID from the gun id;</li>
 *   <li>resolves soul data through the current side's
 *       {@link SoulDataResolver};</li>
 *   <li>requires a live {@link net.minecraft.core.RegistryAccess registry
 *       view};</li>
 *   <li>synthesises the player's effective definition.</li>
 * </ol>
 * Any missing piece returns {@link Optional#empty()}, letting ModularShoot
 * fall through to the next gun source.</p>
 */
public final class DynamicGunDefinitionProvider implements GunDefinitionProvider {

    @Override
    @NotNull
    public Optional<GunDefinition> get(@NotNull ResourceLocation gunId) {
        Optional<UUID> ownerIdOpt = SoulGunId.parse(gunId);
        if (ownerIdOpt.isEmpty()) {
            return Optional.empty();
        }

        SoulDataResolver resolver = SoulDataResolvers.current();
        if (resolver == null) {
            return Optional.empty();
        }

        var data = resolver.resolve(ownerIdOpt.get());
        if (data == null) {
            return Optional.empty();
        }

        var registryAccess = resolver.registryAccess();
        if (registryAccess == null) {
            return Optional.empty();
        }

        return Optional.of(GunDefinitionSynthesizer.synthesize(data, registryAccess));
    }
}