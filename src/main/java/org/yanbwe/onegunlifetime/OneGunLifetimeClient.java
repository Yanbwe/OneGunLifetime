package org.yanbwe.onegunlifetime;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.yanbwe.onegunlifetime.client.ClientSoulDataStore;
import org.yanbwe.onegunlifetime.def.ClientSoulDataResolver;
import org.yanbwe.onegunlifetime.def.SoulDataResolvers;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = OneGunLifetime.MODID, dist = Dist.CLIENT)
public class OneGunLifetimeClient {
    public OneGunLifetimeClient() {
        // Client-side registration code goes here.
        SoulDataResolvers.setClient(new ClientSoulDataResolver());
        NeoForge.EVENT_BUS.addListener(OneGunLifetimeClient::onClientLoggingOut);
        NeoForge.EVENT_BUS.addListener(OneGunLifetimeClient::onClientLoggingIn);
    }

    private static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Defensive: guarantee no stale server data survives into a new login.
        ClientSoulDataStore.clear();
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // A new integrated server or a disconnect to the main menu must not
        // carry the previous world's soul data into the next session.
        ClientSoulDataStore.clear();
    }
}