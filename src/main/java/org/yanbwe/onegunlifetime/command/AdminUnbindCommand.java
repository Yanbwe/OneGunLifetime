package org.yanbwe.onegunlifetime.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.yanbwe.onegunlifetime.OneGunLifetimeAPI;

/**
 * Implements the admin-only {@code /onegun unbind <player>}.
 *
 * <p>Removes the target's soul binding and clears that player's projection
 * guns from their main inventory, armor slots and offhand slot.
 * Implementation lives in
 * {@link org.yanbwe.onegunlifetime.OneGunLifetimeAPI#unbindAll}.</p>
 */
public final class AdminUnbindCommand {

    /** Op permission level required to unbind another player. */
    private static final int PERMISSION_LEVEL = 2;

    private AdminUnbindCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("unbind")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(AdminUnbindCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");

        if (!OneGunLifetimeAPI.unbindAll(target)) {
            source.sendFailure(Component.translatable(
                    "onegunlifetime.command.unbind.not_bound"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "onegunlifetime.command.unbind.success", target.getGameProfile().getName()), false);
        return 1;
    }
}
