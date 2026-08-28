package org.yanbwe.onegunlifetime.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.yanbwe.modularshoot.registry.gun.GunRegistry;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeModifierService;
import org.yanbwe.onegunlifetime.item.ProjectionGunFactory;
import org.yanbwe.onegunlifetime.soul.SoulData;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Implements {@code /onegun bind <gunId>}.
 *
 * <p>Binds the executor's soul to a registered template gun, gives them a new
 * projection gun, and refreshes their mounted soul-gun attributes.</p>
 */
public final class BindCommand {

    private BindCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("bind")
                .then(Commands.argument("gunId", ResourceLocationArgument.id())
                        .executes(BindCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation gunId = ResourceLocationArgument.getId(context, "gunId");
        RegistryAccess registryAccess = player.registryAccess();

        if (!GunRegistry.getAllGunIds(registryAccess).contains(gunId)) {
            source.sendFailure(Component.translatable("onegunlifetime.command.bind.not_registered"));
            return 0;
        }
        if (SoulDataManager.isBound(player)) {
            source.sendFailure(Component.translatable("onegunlifetime.command.bind.already_bound"));
            return 0;
        }

        SoulData data;
        try {
            data = SoulDataManager.bind(player, gunId);
        } catch (IllegalStateException e) {
            source.sendFailure(Component.translatable("onegunlifetime.command.bind.already_bound"));
            return 0;
        }

        ItemStack projection = ProjectionGunFactory.create(data, registryAccess);
        if (!player.getInventory().add(projection)) {
            player.drop(projection, false);
        }

        // The SoulDataChangedEvent also refreshes, but an explicit refresh
        // here avoids any ordering surprise between bind and inventory add.
        PlayerGunAttributeModifierService.refresh(player);

        source.sendSuccess(() -> Component.translatable("onegunlifetime.command.bind.success", gunId), false);
        return 1;
    }
}