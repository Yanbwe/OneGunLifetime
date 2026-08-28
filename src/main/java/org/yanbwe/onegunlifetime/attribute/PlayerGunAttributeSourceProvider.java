package org.yanbwe.onegunlifetime.attribute;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.attribute.PlayerAttributeSourceProvider;
import org.yanbwe.modularshoot.attribute.PlayerAttributeValueReader;
import org.yanbwe.onegunlifetime.def.SoulDataResolvers;
import org.yanbwe.onegunlifetime.def.SoulGunId;
import org.yanbwe.onegunlifetime.soul.SoulData;

/**
 * Resolves a {@link PlayerAttributeValueReader} for OneGunLifetime's soul guns.
 *
 * <p>The stack's dynamic gun id encodes the owning player UUID. The reader
 * always computes from the side-appropriate {@link SoulData} (server attachment
 * or client store) via {@link PlayerGunAttributeModifierService#calculateValue}.
 * This keeps tooltip values identical whether or not a real viewer entity is
 * available, and avoids relying on the viewer's possibly stale mounted
 * attribute instance.</p>
 */
public final class PlayerGunAttributeSourceProvider implements PlayerAttributeSourceProvider {

    @Override
    public Optional<PlayerAttributeValueReader> resolve(
            ItemStack stack, @Nullable Player viewer) {
        Optional<ResourceLocation> gunIdOpt = ModularShootAPI.getGunId(stack);
        if (gunIdOpt.isEmpty() || !SoulGunId.isSoulGunId(gunIdOpt.get())) {
            return Optional.empty();
        }
        Optional<UUID> ownerIdOpt = SoulGunId.parse(gunIdOpt.get());
        if (ownerIdOpt.isEmpty()) {
            return Optional.empty();
        }
        UUID ownerId = ownerIdOpt.get();
        return Optional.of((logicalId, access) -> {
            if (SoulDataResolvers.current() == null) {
                return 0.0;
            }
            SoulData data = SoulDataResolvers.current().resolve(ownerId);
            if (data == null) {
                return 0.0;
            }
            RegistryAccess registryAccess = access != null
                    ? access
                    : SoulDataResolvers.current().registryAccess();
            if (registryAccess == null) {
                return 0.0;
            }
            return PlayerGunAttributeModifierService.calculateValue(
                    data, logicalId, registryAccess);
        });
    }
}