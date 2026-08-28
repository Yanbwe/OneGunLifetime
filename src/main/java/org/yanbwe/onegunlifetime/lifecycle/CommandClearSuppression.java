package org.yanbwe.onegunlifetime.lifecycle;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * One-time suppression for inventory-clearing commands.
 *
 * <p>When a player runs {@code /clear}, the command intentionally removes the
 * projection gun from the inventory. Without a guard, the next low-frequency
 * inventory scan would see the missing gun and immediately re-create it,
 * defeating the command. This class marks the affected players before the
 * command executes; the next inventory scan consumes the marker and skips the
 * missing-gun recovery while still allowing later scans to recover a gun that
 * was destroyed by lava, cactus, or other world mechanics.</p>
 */
public final class CommandClearSuppression {

    private static final Set<UUID> SUPPRESSED_PLAYERS = ConcurrentHashMap.newKeySet();

    private CommandClearSuppression() {
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SUPPRESSED_PLAYERS.remove(player.getUUID());
        }
    }

    public static void onCommand(CommandEvent event) {
        var contextBuilder = event.getParseResults().getContext();
        boolean clearCommand = contextBuilder.getNodes().stream()
                .anyMatch(node -> "clear".equals(node.getNode().getName()));
        if (!clearCommand) {
            return;
        }

        try {
            if (contextBuilder.getArguments().containsKey("targets")) {
                var context = contextBuilder.build(event.getParseResults().getReader().getString());
                Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                for (ServerPlayer target : targets) {
                    suppress(target);
                }
            } else {
                ServerPlayer source = contextBuilder.getSource().getPlayerOrException();
                suppress(source);
            }
        } catch (CommandSyntaxException e) {
            // The parse may be incomplete or the source may not be a player.
            // Fall back to suppressing the command sender when it is a player.
            if (contextBuilder.getSource().getEntity() instanceof ServerPlayer player) {
                suppress(player);
            }
        }
    }

    public static void suppress(ServerPlayer player) {
        if (player != null) {
            SUPPRESSED_PLAYERS.add(player.getUUID());
        }
    }

    public static boolean isSuppressed(ServerPlayer player) {
        return player != null && SUPPRESSED_PLAYERS.contains(player.getUUID());
    }

    public static void consume(ServerPlayer player) {
        if (player != null) {
            SUPPRESSED_PLAYERS.remove(player.getUUID());
        }
    }
}