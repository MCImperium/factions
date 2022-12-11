package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class RankCommand implements Command {
    private int promote(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (target.getUUID().equals(player.getUUID())) {
            new Message("You cannot promote yourself").format(ChatFormatting.RED).send(player, false);

            return 0;
        }

        Faction faction = Command.getUser(player).getFaction();

        for (User users : faction.getUsers())
            if (users.getID().equals(target.getUUID())) {

                switch (users.rank) {
                    case GUEST -> users.rank = User.Rank.MEMBER;
                    case MEMBER -> users.rank = User.Rank.COMMANDER;
                    case COMMANDER -> users.rank = User.Rank.LEADER;
                    case LEADER -> {
                        new Message("You cannot promote a Leader to Owner").format(ChatFormatting.RED).send(player, false);
                        return 0;
                    }
                    case OWNER -> {
                        new Message("You cannot promote the Owner").format(ChatFormatting.RED).send(player, false);
                        return 0;
                    }
                }

                context.getSource().getServer().getPlayerList().sendPlayerPermissionLevel(target);

                new Message("Promoted " + target.getName().getString() + " to " + User.get(target.getUUID()).getRankName())
                    .prependFaction(faction)
                    .send(player, false);
                
                return 1;
            }

        new Message(target.getName().getString() + " is not in your faction").format(ChatFormatting.RED).send(player, false);
        return 0;
    }

    private int demote(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (target.getUUID().equals(player.getUUID())) {
            new Message("You cannot demote yourself").format(ChatFormatting.RED).send(player, false);
            return 0;
        }

        Faction faction = Command.getUser(player).getFaction();

        for (User user : faction.getUsers())
            if (user.getID().equals(target.getUUID())) {

                switch (user.rank) {
                    case GUEST -> {
                        new Message("You cannot demote a Guest").format(ChatFormatting.RED).send(player, false);
                        return 0;
                    }
                    case MEMBER -> user.rank = User.Rank.GUEST;
                    case COMMANDER -> user.rank = User.Rank.MEMBER;
                    case LEADER -> {
                        if (Command.getUser(player).rank == User.Rank.LEADER) {
                            new Message("You cannot demote a fellow Co-Owner").format(ChatFormatting.RED).send(player, false);
                            return 0;
                        }

                        user.rank = User.Rank.COMMANDER;
                    }
                    case OWNER -> {
                        new Message("You cannot demote the Owner").format(ChatFormatting.RED).send(player, false);
                        return 0;
                    }
                }

                context.getSource().getServer().getPlayerList().sendPlayerPermissionLevel(target);

                new Message("Demoted " + target.getName().getString() + " to " + User.get(target.getUUID()).getRankName())
                    .prependFaction(faction)
                    .send(player, false);
                
                return 1;
            }

        new Message(target.getName().getString() + " is not in your faction").format(ChatFormatting.RED).send(player, false);
        return 0;
    }

    private int transfer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (target.getUUID().equals(player.getUUID())) {
            new Message("You cannot transfer ownership to yourself").format(ChatFormatting.RED).send(player, false);

            return 0;
        }

        User targetUser = User.get(target.getUUID());
        UUID targetFaction = targetUser.isInFaction() ? targetUser.getFaction().getID() : null;
        if (Command.getUser(player).getFaction().getID().equals(targetFaction)) {
            targetUser.rank = User.Rank.OWNER;
            Command.getUser(player).rank = User.Rank.LEADER;

            context.getSource().getServer().getPlayerList().sendPlayerPermissionLevel(player);
            context.getSource().getServer().getPlayerList().sendPlayerPermissionLevel(target);

            new Message("Transferred ownership to " + target.getName().getString())
                .prependFaction(Faction.get(targetFaction))
                .send(player, false);

            return 1;
        }

        new Message(target.getName().getString() + " is not in your faction").format(ChatFormatting.RED).send(player, false);
        return 0;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("rank")
            .requires(Requires.isLeader())
            .then(
                Commands
                .literal("promote")
                .requires(Requires.hasPerms("factions.rank.promote", 0))
                .then(
                    Commands.argument("player", EntityArgument.player())
                    .executes(this::promote)
                )
            )
            .then(
                Commands
                .literal("demote")
                .requires(Requires.hasPerms("factions.rank.demote", 0))
                .then(
                    Commands.argument("player", EntityArgument.player())
                    .executes(this::demote)
                )
            )
            .then(
                Commands
                .literal("transfer")
                .requires(Requires.multiple(Requires.hasPerms("factions.rank.transfer", 0), Requires.isOwner()))
                .then(
                    Commands.argument("player", EntityArgument.player())
                    .executes(this::transfer)
                )
            )
            .build();
    }
}
