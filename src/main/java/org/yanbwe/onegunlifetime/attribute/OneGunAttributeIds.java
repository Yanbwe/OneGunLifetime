package org.yanbwe.onegunlifetime.attribute;

import net.minecraft.resources.ResourceLocation;
import org.yanbwe.onegunlifetime.OneGunLifetime;

/**
 * Stable modifier ids used by OneGunLifetime's player-side attribute mounting.
 *
 * <p>Vanilla keys attribute modifiers by the {@code (attribute, id)} pair, so
 * the ids here only need to be unique per attribute. Keeping them stable across
 * refreshes prevents duplicate-modifier exceptions and lets
 * {@code AttributeInstance.removeModifier(id)} clean up old values.</p>
 */
public final class OneGunAttributeIds {

    /** Stable modifier id for the synthesized gun's base stat values. */
    public static final ResourceLocation BASE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(OneGunLifetime.MODID, "gun_base");

    /** Prefix of every OneGunLifetime plugin modifier id. */
    public static final String PLUGIN_ID_PREFIX = "plugin_";

    private OneGunAttributeIds() {
    }

    /**
     * Builds the stable modifier id for one plugin instance.
     *
     * <p>The encoding mirrors ModularShoot's
     * {@code AttributeModifierService.pluginModifierId}, but under the
     * {@code onegunlifetime} namespace so player-mounted modifiers cannot
     * collide with item-side modifiers left by older framework behavior. The
     * namespace and path are length-prefixed, which makes the encoding
     * collision-free for legal {@link ResourceLocation} values.</p>
     *
     * @param pluginId        the plugin definition id
     * @param occurrenceIndex the index of this plugin instance in the full
     *                        installed-plugin list
     * @return a stable {@code onegunlifetime:plugin_<...>_<index>} id
     */
    public static ResourceLocation pluginModifierId(ResourceLocation pluginId, int occurrenceIndex) {
        String namespace = pluginId.getNamespace();
        String path = pluginId.getPath();
        String encoded = PLUGIN_ID_PREFIX
                + namespace.length() + "_" + namespace + "_"
                + path.length() + "_" + path + "_"
                + occurrenceIndex;
        return ResourceLocation.fromNamespaceAndPath(OneGunLifetime.MODID, encoded);
    }

    /**
     * Returns whether a modifier id is one of this mod's plugin modifiers.
     *
     * <p>Used for robust cleanup when the current {@code SoulData} plugin list
     * no longer contains a plugin that was mounted previously.</p>
     *
     * @param id the modifier id to test
     * @return {@code true} if the id belongs to this mod's player-side plugin
     *         modifier family
     */
    public static boolean isPluginModifierId(ResourceLocation id) {
        return OneGunLifetime.MODID.equals(id.getNamespace())
                && id.getPath().startsWith(PLUGIN_ID_PREFIX);
    }
}