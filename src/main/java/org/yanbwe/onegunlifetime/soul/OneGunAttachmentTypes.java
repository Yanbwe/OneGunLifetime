package org.yanbwe.onegunlifetime.soul;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.yanbwe.onegunlifetime.OneGunLifetime;

/**
 * Deferred register for OneGunLifetime's data attachments.
 *
 * <p><strong>Default value design:</strong> {@link #PLAYER_SOUL} uses
 * {@code null} as its default placeholder. NeoForge's
 * {@code IAttachmentHolder#setData} does not accept {@code null}, so
 * "unbound" is represented by <em>no attachment entry</em>, not by a null
 * stored value. The manager therefore reads through
 * {@code getExistingDataOrNull} (which never materialises the default) and
 * unbinds with {@code removeData}. This preserves the intended
 * "unbound == null" semantic without storing null in the attachment map. As a
 * safety net, the serializer also skips null values if some future code
 * accidentally calls {@code getData} before binding.</p>
 *
 * <p>The attachment is serialized with {@link SoulDataCodec#CODEC} and
 * {@code copyOnDeath()} is enabled so a soul survives player respawn.</p>
 */
public final class OneGunAttachmentTypes {
    private OneGunAttachmentTypes() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, OneGunLifetime.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SoulData>> PLAYER_SOUL =
            ATTACHMENT_TYPES.register("player_soul", () ->
                    AttachmentType.builder(() -> (SoulData) null)
                            .serialize(SoulDataCodec.CODEC, data -> data != null)
                            .copyOnDeath()
                            .build());
}