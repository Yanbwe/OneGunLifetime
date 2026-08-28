package org.yanbwe.onegunlifetime.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Implements {@code /onegun info}.
 *
 * <p>Shows the executor's soul binding details in a compact multi-line
 * summary.</p>
 */
public final class InfoCommand {

    private InfoCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("info").executes(InfoCommand::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        SoulData data = SoulDataManager.get(player);
        if (data == null) {
            source.sendFailure(Component.translatable("onegunlifetime.command.not_bound"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable(
                "onegunlifetime.command.info.header", player.getGameProfile().getName()), false);
        source.sendSuccess(() -> Component.literal("  template: " + data.templateGunId()), false);
        source.sendSuccess(() -> Component.literal("  stable_gun_id: " + data.stableGunId()), false);
        source.sendSuccess(() -> Component.literal("  version: " + data.version()), false);

        if (data.statOverrides().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  overrides: <none>"), false);
        } else {
            source.sendSuccess(() -> Component.literal("  overrides:"), false);
            data.statOverrides().forEach((key, value) -> source.sendSuccess(
                    () -> Component.literal("    " + key + " = " + value), false));
        }

        if (data.traits().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  traits: <none>"), false);
        } else {
            source.sendSuccess(() -> Component.literal("  traits: " + String.join(", ",
                    data.traits().stream().map(Object::toString).toList())), false);
        }

        if (data.plugins().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  plugins: <none>"), false);
        } else {
            source.sendSuccess(() -> Component.literal("  plugins:"), false);
            data.plugins().forEach(plugin -> source.sendSuccess(() -> Component.literal(
                    "    " + plugin.pluginId()
                            + " | uuid " + plugin.instanceUuid()
                            + " | type " + plugin.installedTypeId()
                            + (plugin.locked() ? " | locked" : "")), false));
        }

        return 1;
    }
}