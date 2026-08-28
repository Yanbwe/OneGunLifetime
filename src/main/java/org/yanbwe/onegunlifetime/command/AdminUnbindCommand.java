package org.yanbwe.onegunlifetime.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.yanbwe.modularshoot.ModularShootAPI;
import org.yanbwe.onegunlifetime.attribute.PlayerGunAttributeModifierService;
import org.yanbwe.onegunlifetime.def.SoulGunId;
import org.yanbwe.onegunlifetime.soul.SoulDataManager;

/**
 * Implements the admin-only {@code /onegun unbind <player>}.
 *
 * <p>Removes the target's soul binding and clears that player's projection
 * guns from their main inventory, armor slots and offhand slot.</p>
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

        if (!SoulDataManager.isBound(target)) {
            source.sendFailure(Component.translatable("onegunlifetime.command.unbind.not_bound"));
            return 0;
        }

        SoulDataManager.unbind(target);
        PlayerGunAttributeModifierService.remove(target);
        removeProjectionGuns(target);

        source.sendSuccess(() -> Component.translatable(
                "onegunlifetime.command.unbind.success", target.getGameProfile().getName()), false);
        return 1;
    }

    private static void removeProjectionGuns(ServerPlayer player) {
        var inventory = player.getInventory();
        removeMatching(inventory.items, player);
        removeMatching(inventory.armor, player);
        removeMatching(inventory.offhand, player);
    }

    private static void removeMatching(net.minecraft.core.NonNullList<ItemStack> slots, ServerPlayer player) {
        for (int i = 0; i < slots.size(); i++) {
            if (isOwnedProjection(slots.get(i), player)) {
                slots.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isOwnedProjection(ItemStack stack, ServerPlayer player) {
        return ModularShootAPI.getGunId(stack)
                .filter(SoulGunId::isSoulGunId)
                .flatMap(SoulGunId::parse)
                .filter(ownerId -> ownerId.equals(player.getUUID()))
                .isPresent();
    }
}