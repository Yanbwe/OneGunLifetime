package org.yanbwe.onegunlifetime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.component.PluginInstance;
import org.yanbwe.modularshoot.plugin.PluginInstallService.InstallResult;
import org.yanbwe.modularshoot.plugin.PluginRegistry;
import org.yanbwe.modularshoot.plugin.UninstallResult;
import org.yanbwe.modularshoot.registry.ModularShootRegistries;
import org.yanbwe.modularshoot.registry.Trait;
import org.yanbwe.modularshoot.registry.attribute.AttributeMeta;
import org.yanbwe.modularshoot.registry.gun.GunRegistry;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeModifierService;
import org.yanbwe.onegunlifetime.item.ProjectionGunFactory;
import org.yanbwe.onegunlifetime.item.ProjectionGuns;
import org.yanbwe.onegunlifetime.scan.PlayerInventoryScanner;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Public Java API facade for OneGunLifetime, intended for other mods.
 *
 * <p>All mutating methods require a {@link ServerPlayer} (server-authoritative)
 * and return result objects instead of throwing business exceptions. Query
 * methods accept a plain {@link Player}. Plugin changes go through the
 * ModularShoot install/uninstall pipeline on the player's projection gun, so
 * the existing event wiring (soul data write-back, sync broadcast, attribute
 * refresh) applies automatically.</p>
 *
 * <p>Usage mirrors the {@code ModularShootAPI} conventions: static methods,
 * {@code Objects.requireNonNull} boundary checks, Optional/record results.</p>
 */
public final class OneGunLifetimeAPI {

    private OneGunLifetimeAPI() {
    }

    // ===== Result types ==================================================

    /** Outcome of a bind attempt. */
    public enum BindResult {
        /** The player is now bound and has received a projection gun. */
        SUCCESS,
        /** The player already has a soul binding (checked before and inside bind). */
        ALREADY_BOUND,
        /** The template gun id is not registered in ModularShoot. */
        NOT_REGISTERED
    }

    /** Outcome of a soul-data mutation (stats, traits, gun state). */
    public enum MutationResult {
        SUCCESS,
        NOT_BOUND,
        /** The attribute id is not in the {@code modularshoot:attribute_meta} registry. */
        INVALID_ATTRIBUTE,
        /** The value is not a finite double. */
        INVALID_VALUE,
        /** The trait id is not in the {@code modularshoot:traits} registry. */
        INVALID_TRAIT
    }

    /**
     * Outcome of a plugin add/remove attempt.
     *
     * @param status          classification of the outcome
     * @param uninstallReason structured reason from the ModularShoot uninstall
     *                        pipeline; non-null only when a {@link #removePlugin}
     *                        change was rejected
     * @param rejectionReason localizable reason from the ModularShoot install
     *                        pipeline; non-null only when an {@link #addPlugin}
     *                        change was rejected
     */
    public record PluginChangeResult(
            Status status,
            @Nullable UninstallResult.Reason uninstallReason,
            @Nullable Component rejectionReason) {

        /** Classification of a plugin change outcome. */
        public enum Status {
            SUCCESS,
            NOT_BOUND,
            /** The player has no projection gun in their inventory. */
            NO_PROJECTION,
            /** The plugin id is not registered in ModularShoot (add only). */
            UNKNOWN_PLUGIN,
            /** The ModularShoot pipeline rejected the change. */
            NOT_INSTALLED
        }

        public boolean success() {
            return status == Status.SUCCESS;
        }

        public static PluginChangeResult ok() {
            return new PluginChangeResult(Status.SUCCESS, null, null);
        }

        public static PluginChangeResult fail(Status status) {
            return new PluginChangeResult(status, null, null);
        }

        /** Wraps a rejection from the ModularShoot install pipeline. */
        public static PluginChangeResult rejected(@Nullable Component reason) {
            return new PluginChangeResult(Status.NOT_INSTALLED, null, reason);
        }

        /** Wraps a rejection from the ModularShoot uninstall pipeline. */
        public static PluginChangeResult rejected(UninstallResult.Reason reason) {
            return new PluginChangeResult(Status.NOT_INSTALLED, reason, null);
        }
    }

    // ===== Queries =======================================================

    /**
     * Returns the player's soul data, or empty when unbound.
     */
    public static Optional<SoulData> getSoulData(Player player) {
        Objects.requireNonNull(player, "player");
        return Optional.ofNullable(SoulDataManager.get(player));
    }

    /**
     * Returns whether the player has a soul binding.
     */
    public static boolean isBound(Player player) {
        Objects.requireNonNull(player, "player");
        return SoulDataManager.isBound(player);
    }

    /**
     * Returns the soul owner UUID carried by a projection gun stack, if any.
     * This is the single recognition implementation shared by the whole mod.
     */
    public static Optional<UUID> getOwnerOf(ItemStack gunStack) {
        return ProjectionGuns.ownerOf(gunStack);
    }

    /**
     * Computes the player's final value for every player-applicable logical
     * attribute (base stats + overrides + traits + plugin modifiers).
     *
     * <p>Pure computation with no entity access. Each entry runs one
     * synthesis, so cache the result instead of calling per tick.</p>
     *
     * @return an unmodifiable map of logical attribute id to final value,
     *         iterating in {@code modularshoot:attribute_meta} registry
     *         order; empty when the player is unbound
     */
    public static Map<ResourceLocation, Double> getEffectiveValues(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        SoulData data = SoulDataManager.get(player);
        if (data == null) {
            return Map.of();
        }
        RegistryAccess registryAccess = player.registryAccess();
        Registry<AttributeMeta> metaRegistry =
                registryAccess.registry(ModularShootRegistries.ATTRIBUTE_META_KEY).orElse(null);
        if (metaRegistry == null) {
            return Map.of();
        }

        Map<ResourceLocation, Double> values = new LinkedHashMap<>();
        for (Map.Entry<ResourceKey<AttributeMeta>, AttributeMeta> entry : metaRegistry.entrySet()) {
            ResourceLocation logicalId = entry.getKey().location();
            if (!entry.getValue().allowsEntity(EntityType.PLAYER)) {
                continue;
            }
            values.put(logicalId, PlayerGunAttributeModifierService.calculateValue(
                    data, logicalId, registryAccess));
        }
        return Collections.unmodifiableMap(values);
    }

    // ===== Lifecycle =====================================================

    /**
     * One-step bind: validates the template gun, binds the player's soul,
     * gives them a fresh projection gun (dropped at their feet when the
     * inventory is full) and refreshes their mounted attributes.
     *
     * @return {@link BindResult#SUCCESS} on success; never throws for
     *         business failures
     */
    public static BindResult bindAndGive(ServerPlayer player, ResourceLocation templateGunId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(templateGunId, "templateGunId");
        RegistryAccess registryAccess = player.registryAccess();

        if (!GunRegistry.getAllGunIds(registryAccess).contains(templateGunId)) {
            return BindResult.NOT_REGISTERED;
        }
        if (SoulDataManager.isBound(player)) {
            return BindResult.ALREADY_BOUND;
        }
        SoulData data;
        try {
            data = SoulDataManager.bind(player, templateGunId);
        } catch (IllegalStateException e) {
            return BindResult.ALREADY_BOUND;
        }

        ItemStack projection = ProjectionGunFactory.create(data, registryAccess);
        if (!player.getInventory().add(projection)) {
            player.drop(projection, false);
        }

        // The SoulDataChangedEvent also refreshes, but an explicit refresh
        // here avoids any ordering surprise between bind and the inventory add.
        PlayerGunAttributeModifierService.refresh(player);
        return BindResult.SUCCESS;
    }

    /**
     * Full unbind: removes the soul binding, clears every mounted attribute
     * modifier owned by this mod and removes all projection guns from the
     * player's main inventory, armor and offhand slots.
     *
     * @return {@code true} when the player was bound and is now fully
     *         unbound; {@code false} when the player was not bound (idempotent)
     */
    public static boolean unbindAll(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!SoulDataManager.isBound(player)) {
            return false;
        }
        SoulDataManager.unbind(player);
        PlayerGunAttributeModifierService.remove(player);
        clearProjectionSlots(player);
        return true;
    }

    /**
     * Forces one full inventory scan now (dedup, foreign-gun assimilation,
     * projection recovery, plugin backfill). No-op for unbound players.
     */
    public static void rescan(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        PlayerInventoryScanner.scan(player);
    }

    // ===== Stat / trait (validation moved out of the command layer) ======

    /**
     * Sets one stat override after validating the value and the attribute id.
     */
    public static MutationResult setStatOverride(ServerPlayer player, ResourceLocation key, double value) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(key, "key");
        if (!SoulDataManager.isBound(player)) {
            return MutationResult.NOT_BOUND;
        }
        if (!Double.isFinite(value)) {
            return MutationResult.INVALID_VALUE;
        }
        if (!isValidAttribute(player.registryAccess(), key)) {
            return MutationResult.INVALID_ATTRIBUTE;
        }
        SoulDataManager.setStatOverride(player, key, value);
        PlayerGunAttributeModifierService.refresh(player);
        return MutationResult.SUCCESS;
    }

    /**
     * Removes one stat override after validating the attribute id.
     */
    public static MutationResult removeStatOverride(ServerPlayer player, ResourceLocation key) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(key, "key");
        if (!SoulDataManager.isBound(player)) {
            return MutationResult.NOT_BOUND;
        }
        if (!isValidAttribute(player.registryAccess(), key)) {
            return MutationResult.INVALID_ATTRIBUTE;
        }
        SoulDataManager.removeStatOverride(player, key);
        PlayerGunAttributeModifierService.refresh(player);
        return MutationResult.SUCCESS;
    }

    /**
     * Clears all stat overrides.
     */
    public static MutationResult clearStatOverrides(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!SoulDataManager.isBound(player)) {
            return MutationResult.NOT_BOUND;
        }
        SoulDataManager.clearStatOverrides(player);
        PlayerGunAttributeModifierService.refresh(player);
        return MutationResult.SUCCESS;
    }

    /**
     * Adds a trait after validating the trait id.
     */
    public static MutationResult addTrait(ServerPlayer player, ResourceLocation traitId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(traitId, "traitId");
        if (!SoulDataManager.isBound(player)) {
            return MutationResult.NOT_BOUND;
        }
        if (!isValidTrait(player.registryAccess(), traitId)) {
            return MutationResult.INVALID_TRAIT;
        }
        SoulDataManager.addTrait(player, traitId);
        PlayerGunAttributeModifierService.refresh(player);
        return MutationResult.SUCCESS;
    }

    /**
     * Removes a trait after validating the trait id.
     */
    public static MutationResult removeTrait(ServerPlayer player, ResourceLocation traitId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(traitId, "traitId");
        if (!SoulDataManager.isBound(player)) {
            return MutationResult.NOT_BOUND;
        }
        if (!isValidTrait(player.registryAccess(), traitId)) {
            return MutationResult.INVALID_TRAIT;
        }
        SoulDataManager.removeTrait(player, traitId);
        PlayerGunAttributeModifierService.refresh(player);
        return MutationResult.SUCCESS;
    }

    // ===== Plugins / gun state ===========================================

    /**
     * Installs a plugin by registry id onto the player's projection gun,
     * reusing the full ModularShoot install pipeline (slot type, capacity and
     * lock validation included). The installed gun copy is written back to
     * its inventory slot; the framework's post-install event then writes the
     * final plugin list to the soul data and refreshes attributes.
     *
     * @return {@link PluginChangeResult.Status#NO_PROJECTION} (instead of
     *         silently creating one) when the player has no projection gun
     */
    public static PluginChangeResult addPlugin(ServerPlayer player, ResourceLocation pluginId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(pluginId, "pluginId");
        if (!SoulDataManager.isBound(player)) {
            return PluginChangeResult.fail(PluginChangeResult.Status.NOT_BOUND);
        }
        RegistryAccess registryAccess = player.registryAccess();
        if (ModularShootAPI.getPluginDefinition(registryAccess, pluginId).isEmpty()) {
            return PluginChangeResult.fail(PluginChangeResult.Status.UNKNOWN_PLUGIN);
        }
        ItemStack projection = ProjectionGuns.find(player);
        if (projection.isEmpty()) {
            return PluginChangeResult.fail(PluginChangeResult.Status.NO_PROJECTION);
        }

        ItemStack pluginStack = PluginRegistry.createPluginStack(pluginId);
        InstallResult result = ModularShootAPI.installPlugin(projection, pluginStack, player);
        if (!result.success()) {
            return PluginChangeResult.rejected(result.errorMessage().orElse(null));
        }
        ProjectionGuns.writeBack(player, result.installedGun());
        return PluginChangeResult.ok();
    }

    /**
     * Removes one installed plugin identified by its instance uuid from the
     * player's projection gun. The stack is mutated in place by the
     * ModularShoot pipeline; the framework's post-uninstall event then writes
     * the final plugin list to the soul data and refreshes attributes. The
     * removed plugin item is returned to the player's inventory (dropped at
     * their feet when full).
     *
     * @return {@link PluginChangeResult.Status#NOT_INSTALLED} with the
     *         structured {@link UninstallResult.Reason} when the pipeline
     *         rejects (locked, uuid not found, ...)
     */
    public static PluginChangeResult removePlugin(ServerPlayer player, UUID pluginInstanceUuid) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(pluginInstanceUuid, "pluginInstanceUuid");
        if (!SoulDataManager.isBound(player)) {
            return PluginChangeResult.fail(PluginChangeResult.Status.NOT_BOUND);
        }
        ItemStack projection = ProjectionGuns.find(player);
        if (projection.isEmpty()) {
            return PluginChangeResult.fail(PluginChangeResult.Status.NO_PROJECTION);
        }

        UninstallResult result = ModularShootAPI.uninstallPlugin(
                projection, pluginInstanceUuid, player, false, true);
        if (!result.success()) {
            return PluginChangeResult.rejected(result.reason());
        }
        return PluginChangeResult.ok();
    }

    /**
     * Returns the soul data's ordered plugin list; empty when unbound.
     */
    public static List<PluginInstance> getPlugins(Player player) {
        Objects.requireNonNull(player, "player");
        SoulData data = SoulDataManager.get(player);
        return data == null ? List.of() : data.plugins();
    }

    /**
     * Returns the soul data's runtime gun state, or empty when unbound.
     */
    public static Optional<CompoundTag> getGunState(Player player) {
        Objects.requireNonNull(player, "player");
        SoulData data = SoulDataManager.get(player);
        return data == null ? Optional.empty() : Optional.of(data.gunState());
    }

    /**
     * Replaces the soul data's runtime gun state.
     *
     * @return {@link MutationResult#NOT_BOUND} when the player is not bound;
     *         {@link MutationResult#SUCCESS} otherwise
     */
    public static MutationResult setGunState(ServerPlayer player, CompoundTag state) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(state, "state");
        if (!SoulDataManager.isBound(player)) {
            return MutationResult.NOT_BOUND;
        }
        SoulDataManager.setGunState(player, state);
        return MutationResult.SUCCESS;
    }

    // ===== Internals =====================================================

    private static boolean isValidAttribute(RegistryAccess registryAccess, ResourceLocation attributeId) {
        Registry<AttributeMeta> metaRegistry =
                registryAccess.registry(ModularShootRegistries.ATTRIBUTE_META_KEY).orElse(null);
        return metaRegistry != null && metaRegistry.get(attributeId) != null;
    }

    private static boolean isValidTrait(RegistryAccess registryAccess, ResourceLocation traitId) {
        Registry<Trait> traitRegistry =
                registryAccess.registry(ModularShootRegistries.TRAITS_KEY).orElse(null);
        return traitRegistry != null && traitRegistry.get(traitId) != null;
    }

    private static void clearProjectionSlots(ServerPlayer player) {
        var inventory = player.getInventory();
        UUID ownerId = player.getUUID();
        clearOwnedProjections(inventory.items, ownerId);
        clearOwnedProjections(inventory.armor, ownerId);
        clearOwnedProjections(inventory.offhand, ownerId);
    }

    private static void clearOwnedProjections(List<ItemStack> slots, UUID ownerId) {
        for (int i = 0; i < slots.size(); i++) {
            if (ProjectionGuns.isOwnedBy(slots.get(i), ownerId)) {
                slots.set(i, ItemStack.EMPTY);
            }
        }
    }
}
