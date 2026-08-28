package org.yanbwe.onegunlifetime.attribute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.yanbwe.modularshoot.ModularShoot;
import org.yanbwe.modularshoot.component.PluginInstance;
import org.yanbwe.modularshoot.plugin.PluginDefinition;
import org.yanbwe.modularshoot.plugin.PluginModifier;
import org.yanbwe.modularshoot.plugin.PluginRegistry;
import org.yanbwe.modularshoot.registry.ModularShootRegistries;
import org.yanbwe.modularshoot.registry.attribute.AttributeMeta;
import org.yanbwe.modularshoot.registry.gun.GunDefinition;
import org.yanbwe.onegunlifetime.def.GunDefinitionSynthesizer;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Mounts OneGunLifetime's synthesized gun values and plugin modifiers directly
 * on a {@link ServerPlayer}'s attributes.
 *
 * <p>The framework writes {@code ATTRIBUTE_MODIFIERS} to the item stack only
 * when {@code attribute_mount = item}. For soul guns the synthesizer forces
 * {@code attribute_mount = player}, so this service is responsible for keeping
 * the player entity's vanilla attribute instances in sync.</p>
 *
 * <p>Modifier ids are stable ({@link OneGunAttributeIds}), so refresh is
 * always {@code remove-then-apply}; {@code remove} is intentionally aggressive
 * and cleans every modifier owned by OneGunLifetime on every attribute the
 * player has, including stale plugin ids from an older plugin list.</p>
 */
public final class PlayerGunAttributeModifierService {

    private PlayerGunAttributeModifierService() {
    }

    /**
     * Refreshes a server player's mounted modifiers from their current
     * {@link SoulData}. Unbound players are simply cleaned.
     */
    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        SoulData data = SoulDataManager.get(player);
        if (data == null) {
            remove(player);
            return;
        }
        remove(player);
        apply(player, data, player.level().registryAccess());
    }

    /**
     * Removes every OneGunLifetime modifier currently mounted on the player.
     *
     * <p>Instead of relying only on the current plugin list, this scans all
     * registered attributes and removes any modifier whose id belongs to this
     * mod. That also removes stale plugin modifiers after a plugin list
     * change or an unbound transition.</p>
     */
    public static void remove(ServerPlayer player) {
        if (player == null) {
            return;
        }
        for (Holder.Reference<Attribute> holder : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance instance = player.getAttribute(holder);
            if (instance != null) {
                removeOwnedModifiers(instance);
            }
        }
    }

    /**
     * Replaces the player's mounted modifiers with values derived from the
     * given {@link SoulData} and its synthesized {@link GunDefinition}.
     */
    public static void apply(ServerPlayer player, SoulData data, RegistryAccess registryAccess) {
        if (player == null) {
            return;
        }
        remove(player);
        if (data == null || registryAccess == null) {
            return;
        }

        GunDefinition synthesized = GunDefinitionSynthesizer.synthesize(data, registryAccess);
        applyBaseModifiers(player, synthesized, registryAccess);
        applyPluginModifiers(player, data, registryAccess);
    }

    /**
     * Pure calculation used by the tooltip/value-reader path when no entity or
     * no mounted instance is available.
     *
     * <p>The returned value follows the vanilla attribute calculation order:
     * all ADD_VALUE modifiers are added to the base, ADD_MULTIPLIED_BASE is
     * applied to that intermediate value, then ADD_MULTIPLIED_TOTAL is
     * applied. Plugin modifiers are matched to the logical attribute either by
     * the logical id itself or by the bound vanilla attribute id.</p>
     */
    public static double calculateValue(
            SoulData data, ResourceLocation logicalId, RegistryAccess registryAccess) {
        if (data == null || logicalId == null || registryAccess == null) {
            return 0.0;
        }
        Registry<AttributeMeta> metaRegistry =
                registryAccess.registry(ModularShootRegistries.ATTRIBUTE_META_KEY).orElse(null);
        if (metaRegistry == null) {
            return 0.0;
        }
        AttributeMeta meta = metaRegistry.get(logicalId);
        if (meta == null || !meta.allowsEntity(EntityType.PLAYER)) {
            return 0.0;
        }

        GunDefinition synthesized = GunDefinitionSynthesizer.synthesize(data, registryAccess);
        double base = synthesized.stats().getOrDefault(logicalId, meta.defaultValue());
        List<PluginModifier> matching =
                collectMatchingPluginModifiers(data, logicalId, meta, registryAccess);

        double afterAdd = base;
        for (PluginModifier mod : matching) {
            if (mod.operation() == PluginModifier.Operation.ADD) {
                afterAdd += mod.value();
            }
        }

        double result = afterAdd;
        for (PluginModifier mod : matching) {
            if (mod.operation() == PluginModifier.Operation.MULTIPLY) {
                result += afterAdd * mod.value();
            }
        }
        for (PluginModifier mod : matching) {
            if (mod.operation() == PluginModifier.Operation.MULTIPLY_TOTAL) {
                result *= 1.0 + mod.value();
            }
        }
        return result;
    }

    /**
     * Removes OneGunLifetime modifiers from a single attribute instance.
     *
     * <p>Package-visible for focused unit testing of the remove path without
     * constructing a full {@link ServerPlayer}.</p>
     */
    static void removeOwnedModifiers(AttributeInstance instance) {
        if (instance == null) {
            return;
        }
        for (AttributeModifier modifier : instance.getModifiers()) {
            ResourceLocation id = modifier.id();
            if (OneGunAttributeIds.BASE_MODIFIER_ID.equals(id)
                    || OneGunAttributeIds.isPluginModifierId(id)) {
                instance.removeModifier(id);
            }
        }
    }

    private static void applyBaseModifiers(
            ServerPlayer player, GunDefinition synthesized, RegistryAccess registryAccess) {
        Registry<AttributeMeta> metaRegistry =
                registryAccess.registry(ModularShootRegistries.ATTRIBUTE_META_KEY).orElse(null);
        if (metaRegistry == null) {
            return;
        }
        for (Map.Entry<ResourceKey<AttributeMeta>, AttributeMeta> entry : metaRegistry.entrySet()) {
            ResourceLocation logicalId = entry.getKey().location();
            AttributeMeta meta = entry.getValue();
            if (!meta.allowsEntity(EntityType.PLAYER)) {
                continue;
            }
            Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(meta.binds()).orElse(null);
            if (holder == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) {
                continue;
            }
            double value = synthesized.stats().getOrDefault(logicalId, meta.defaultValue());
            instance.addTransientModifier(new AttributeModifier(
                    OneGunAttributeIds.BASE_MODIFIER_ID,
                    value,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyPluginModifiers(
            ServerPlayer player, SoulData data, RegistryAccess registryAccess) {
        List<PluginInstance> installed = data.plugins();
        for (int occurrenceIndex = 0; occurrenceIndex < installed.size(); occurrenceIndex++) {
            PluginInstance pluginInstance = installed.get(occurrenceIndex);
            Optional<PluginDefinition> pluginDef =
                    PluginRegistry.getPlugin(registryAccess, pluginInstance.pluginId());
            if (pluginDef.isEmpty()) {
                continue;
            }
            applySinglePluginModifiers(player, pluginInstance, pluginDef.get(), occurrenceIndex);
        }
    }

    private static void applySinglePluginModifiers(
            ServerPlayer player,
            PluginInstance pluginInstance,
            PluginDefinition pluginDef,
            int occurrenceIndex) {
        ResourceLocation modifierId =
                OneGunAttributeIds.pluginModifierId(pluginInstance.pluginId(), occurrenceIndex);
        Set<String> seenAttributes = new HashSet<>();
        for (PluginModifier mod : pluginDef.modifiers()) {
            if (!seenAttributes.add(mod.attribute())) {
                continue;
            }
            Holder<Attribute> holder = resolveAttributeHolder(mod.attribute());
            if (holder == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) {
                continue;
            }
            instance.addTransientModifier(new AttributeModifier(
                    modifierId, mod.value(), mapOperation(mod.operation())));
        }
    }

    private static List<PluginModifier> collectMatchingPluginModifiers(
            SoulData data,
            ResourceLocation logicalId,
            AttributeMeta meta,
            RegistryAccess registryAccess) {
        List<PluginModifier> matching = new ArrayList<>();
        for (int occurrenceIndex = 0; occurrenceIndex < data.plugins().size(); occurrenceIndex++) {
            PluginInstance pluginInstance = data.plugins().get(occurrenceIndex);
            Optional<PluginDefinition> pluginDef =
                    PluginRegistry.getPlugin(registryAccess, pluginInstance.pluginId());
            if (pluginDef.isEmpty()) {
                continue;
            }
            Set<String> seenAttributes = new HashSet<>();
            for (PluginModifier mod : pluginDef.get().modifiers()) {
                if (!seenAttributes.add(mod.attribute())) {
                    continue;
                }
                ResourceLocation target = parsePluginAttribute(mod.attribute());
                if (target.equals(logicalId) || target.equals(meta.binds())) {
                    matching.add(mod);
                }
            }
        }
        return matching;
    }

    private static Holder<Attribute> resolveAttributeHolder(String attribute) {
        ResourceLocation attrId = parsePluginAttribute(attribute);
        return BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
    }

    private static ResourceLocation parsePluginAttribute(String attribute) {
        return attribute.contains(":")
                ? ResourceLocation.parse(attribute)
                : ResourceLocation.fromNamespaceAndPath(ModularShoot.MODID, attribute);
    }

    private static AttributeModifier.Operation mapOperation(PluginModifier.Operation operation) {
        return switch (operation) {
            case ADD -> AttributeModifier.Operation.ADD_VALUE;
            case MULTIPLY -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case MULTIPLY_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
}