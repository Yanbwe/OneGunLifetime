package org.yanbwe.onegunlifetime.item;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.yanbwe.modularshoot.attribute.AttributeModifierService;
import org.yanbwe.modularshoot.component.GunData;
import org.yanbwe.modularshoot.component.ModularShootDataComponents;
import org.yanbwe.modularshoot.registry.gun.GunRegistry;
import org.yanbwe.onegunlifetime.def.SoulGunId;
import org.yanbwe.onegunlifetime.soul.SoulData;

/**
 * Creates the physical "projection gun" {@link ItemStack} for a bound soul.
 *
 * <p>The stack uses the owner-specific dynamic gun id
 * ({@code onegunlifetime:soul_<owner-uuid>}) and carries the soul's stable
 * instance UUID, installed plugins, plugin-derived modifier version and runtime
 * gun state instead of the random UUID assigned by the base gun factory.</p>
 */
public final class ProjectionGunFactory {

    private ProjectionGunFactory() {
    }

    /**
     * Builds a new projection gun stack for the given soul data.
     *
     * @param data           the player's current soul binding
     * @param registryAccess the runtime registry view
     * @return a fully populated gun {@link ItemStack}
     */
    public static ItemStack create(SoulData data, RegistryAccess registryAccess) {
        ResourceLocation gunId = SoulGunId.fromPlayer(data.ownerId());

        // createGunStack also resolves the dynamic soul gun definition and
        // writes EMPTY item modifiers for player-mounted guns.
        ItemStack stack = GunRegistry.createGunStack(gunId, registryAccess);

        // Modifier version is an anti-cheat counter owned by the physical GunData;
// SoulData does not persist it, so a freshly created projection starts from
// the new stack's version (normally 0). Keeping 0 here is intentional.
        GunData existing = stack.get(ModularShootDataComponents.GUN_DATA.get());
        int modifierVersion = existing != null ? existing.modifierVersion() : 0;

        GunData newGunData = new GunData(
                gunId,
                data.stableGunId(),
                data.plugins(),
                modifierVersion,
                data.gunState().copy());
        stack.set(ModularShootDataComponents.GUN_DATA.get(), newGunData);

        // Refresh in case the definition/registry changed since creation; for
        // player-mounted soul guns this clears any stale item-side modifiers.
        AttributeModifierService.refreshModifiers(stack, registryAccess);

        return stack;
    }
}