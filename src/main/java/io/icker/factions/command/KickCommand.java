package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class KickCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (target.getUUID().equals(player.getUUID())) {
            new Message("Cannot kick yourself").format(ChatFormatting.RED).send(player, false);
            return 0;
        }

        User selfUser = Command.getUser(player);
        User targetUser = User.get(target.getUUID());
        Faction faction = selfUser.getFaction();

        if (targetUser.getFaction().getID() != faction.getID()) {
            new Message("Cannot kick someone that is not in your faction");
            return 0;
        }

        if (selfUser.rank == User.Rank.LEADER && (targetUser.rank == User.Rank.LEADER || targetUser.rank == User.Rank.OWNER)) {
            new Message("Cannot kick members with a higher of equivalent rank").format(ChatFormatting.RED).send(player, false);
            return 0;
        }

        targetUser.leaveFaction();
        context.getSource().getServer().getPlayerList().sendPlayerPermissionLevel(target);

        new Message("Kicked " + player.getName().getString()).send(player, false);
        new Message("You have been kicked from the faction by " + player.getName().getString()).send(target, false);

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("kick")
            .requires(Requires.multiple(Requires.isLeader(), Requires.hasPerms("factions.kick", 0)))
            .then(
                Commands.argument("player", EntityArgument.player()).executes(this::run)
            )
            .build();
    }
}
