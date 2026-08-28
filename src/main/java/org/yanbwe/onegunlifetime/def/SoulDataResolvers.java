package org.yanbwe.onegunlifetime.def;

import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the active {@link SoulDataResolver} for each side.
 *
 * <p>Both resolvers may be set in a single integrated-server process. When a
 * server is present, {@link #current()} prefers the authoritative server
 * resolver; a pure multiplayer client with no local server uses the store-backed
 * client resolver.</p>
 */
public final class SoulDataResolvers {
    @Nullable
    private static volatile SoulDataResolver server;

    @Nullable
    private static volatile SoulDataResolver client;

    private SoulDataResolvers() {
    }

    public static void setServer(SoulDataResolver resolver) {
        server = resolver;
    }

    public static void setClient(SoulDataResolver resolver) {
        client = resolver;
    }

    /**
     * Returns the resolver for the current physical side.
     *
     * @return the active resolver, or {@code null} if the current side has not
     *         been initialised with one
     */
    @Nullable
    public static SoulDataResolver current() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            // Integrated servers and dedicated servers: the server resolver is
            // authoritative for both server-side and client-side queries.
            return server;
        }
        // Pure multiplayer client: no local server exists, use the client store.
        return client;
    }
}