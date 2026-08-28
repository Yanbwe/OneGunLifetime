package org.yanbwe.onegunlifetime;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.modularshoot.plugin.event.PostPluginInstallEvent;
import org.yanbwe.modularshoot.plugin.event.PostPluginUninstallEvent;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeModifierService;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeSourceProvider;
import org.yanbwe.onegunlifetime.def.DynamicGunDefinitionProvider;
import org.yanbwe.onegunlifetime.def.ServerSoulDataResolver;
import org.yanbwe.onegunlifetime.def.SoulDataResolvers;
import org.yanbwe.onegunlifetime.lifecycle.CommandClearSuppression;
import org.yanbwe.onegunlifetime.lifecycle.ContainerGuard;
import org.yanbwe.onegunlifetime.lifecycle.ContainerRecoveryScanner;
import org.yanbwe.onegunlifetime.lifecycle.DropHandler;
import org.yanbwe.onegunlifetime.lifecycle.MemorialConverter;
import org.yanbwe.onegunlifetime.network.OneGunPayloads;
import org.yanbwe.onegunlifetime.network.SoulDataSyncPayload;
import org.yanbwe.onegunlifetime.plugin.PluginSyncHandler;
import org.yanbwe.onegunlifetime.scan.InventoryScanEvents;
import org.yanbwe.onegunlifetime.scan.LowFrequencyScanHandler;
import org.yanbwe.onegunlifetime.shoot.AssimilationShootPredicate;
import org.yanbwe.onegunlifetime.shoot.NonOwnerShootPredicate;
import org.yanbwe.onegunlifetime.soul.OneGunAttachmentTypes;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataChangedEvent;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(OneGunLifetime.MODID)
public class OneGunLifetime {
    // Define mod id in a place everything can reference
    public static final String MODID = "onegunlifetime";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public OneGunLifetime(IEventBus modEventBus) {
        // All registration code and event listeners go here.
        OneGunAttachmentTypes.ATTACHMENT_TYPES.register(modEventBus);

        // Dynamic soul gun definition source: onegunlifetime:soul_<uuid>.
        ModularShootAPI.registerGunDefinitionProvider(new DynamicGunDefinitionProvider());

        // Player attribute tooltip source: reads from side-appropriate SoulData.
        ModularShootAPI.registerPlayerAttributeSourceProvider(new PlayerGunAttributeSourceProvider());

        // Stage 9: bound players can never fire foreign guns — the trigger
        // pull denies the shot and fast-tracks assimilation to the next tick.
        // Registered before NonOwnerShootPredicate so every foreign gun held
        // by a bound player goes through the assimilation path.
        ModularShootAPI.registerShootPredicate(new AssimilationShootPredicate());

        // Stage 8: only the soul owner may fire their projection gun.
        ModularShootAPI.registerShootPredicate(new NonOwnerShootPredicate());

        // The server resolver is side-safe. It is the active resolver on
        // dedicated servers; on the physical client, OneGunLifetimeClient
        // installs the store-backed client resolver separately.
        SoulDataResolvers.setServer(new ServerSoulDataResolver());

        // Network payload registration.
        modEventBus.addListener(OneGunPayloads::register);

        // Server-side soul-data broadcast and log-in full-sync.
        NeoForge.EVENT_BUS.addListener(OneGunLifetime::onSoulDataChanged);
        NeoForge.EVENT_BUS.addListener(OneGunLifetime::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(OneGunLifetime::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(OneGunLifetime::onPlayerChangedDimension);

        // Stage 5: personal-inventory scanner and assimilation events.
        NeoForge.EVENT_BUS.addListener(InventoryScanEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(InventoryScanEvents::onItemEntityPickupPost);
        NeoForge.EVENT_BUS.addListener(LowFrequencyScanHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LowFrequencyScanHandler::onPlayerLoggedOut);

        // Stage 6: drop recovery, command-clear suppression, container guards,
        // container recovery, and memorial conversion.
        NeoForge.EVENT_BUS.addListener(DropHandler::onItemToss);
        NeoForge.EVENT_BUS.addListener(CommandClearSuppression::onCommand);
        NeoForge.EVENT_BUS.addListener(CommandClearSuppression::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ContainerGuard::onContainerOpen);
        NeoForge.EVENT_BUS.addListener(ContainerGuard::onContainerClose);
        NeoForge.EVENT_BUS.addListener(ContainerGuard::onItemStackedOnOther);
        NeoForge.EVENT_BUS.addListener(ContainerRecoveryScanner::onServerTick);
        NeoForge.EVENT_BUS.addListener(MemorialConverter::onItemEntityPickupPost);

        // Stage 7: plugin install/uninstall changes are written back into soul
        // data and player-mounted attributes are refreshed on the server side.
        NeoForge.EVENT_BUS.addListener(PluginSyncHandler::onPluginInstall);
        NeoForge.EVENT_BUS.addListener(PluginSyncHandler::onPluginUninstall);
    }

    private static void onSoulDataChanged(SoulDataChangedEvent event) {
        SoulData data = event.getData();
        SoulDataSyncPayload payload = data != null
                ? SoulDataSyncPayload.upsert(data)
                : SoulDataSyncPayload.removal(event.getPlayer().getUUID());
        sendToAll(payload);

        // Keep the player entity's mounted modifiers in sync with the new data.
        PlayerGunAttributeModifierService.refresh(event.getPlayer());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        List<SoulData> souls = new ArrayList<>();
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            SoulData soul = SoulDataManager.get(online);
            if (soul != null) {
                souls.add(soul);
            }
        }
        if (!souls.isEmpty()) {
            PacketDistributor.sendToPlayer(player, SoulDataSyncPayload.upsertAll(souls));
        }

        // Mount/refresh the player's soul-gun attributes after login.
        PlayerGunAttributeModifierService.refresh(player);
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        refreshServerPlayer(event);
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refreshServerPlayer(event);
    }

    private static void refreshServerPlayer(PlayerEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerGunAttributeModifierService.refresh(player);
    }

    private static void sendToAll(SoulDataSyncPayload payload) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        // Snapshot the list to avoid concurrent modification if a player logs
        // out while the change is being broadcast.
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}