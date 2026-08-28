package org.yanbwe.onegunlifetime.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers OneGunLifetime's custom payloads.
 *
 * <p>The registration is wired from {@code OneGunLifetime}'s mod constructor
 * to the mod event bus. The payload is {@code playToClient}: only the server
 * sends it, and the handler is safe to register on both sides because
 * {@link SoulDataSyncHandler} only touches the common
 * {@link org.yanbwe.onegunlifetime.client.ClientSoulDataStore}.</p>
 */
public final class OneGunPayloads {
    /**
     * Bump when a payload's wire format changes incompatibly.
     */
    public static final String PROTOCOL_VERSION = "1";

    private OneGunPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                SoulDataSyncPayload.TYPE,
                SoulDataSyncPayload.STREAM_CODEC,
                SoulDataSyncHandler::handle
        );
    }
}