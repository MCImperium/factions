package io.icker.factions.command;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

public class InviteCommand implements Command {
    private int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        List<UUID> invites = Command.getUser(source.getPlayer()).getFaction().invites;
        int count = invites.size();

        new Message("You have ")
                .add(new Message(String.valueOf(count)).format(ChatFormatting.YELLOW))
                .add(" outgoing invite%s", count == 1 ? "" : "s")
                .send(source.getPlayer(), false);

        if (count == 0) return 1;

        GameProfileCache cache = source.getServer().getProfileCache();
        String players = invites.stream()
            .map(invite -> cache.get(invite).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
            .collect(Collectors.joining(", "));

        new Message(players).format(ChatFormatting.ITALIC).send(source.getPlayer(), false);
        return 1;
    }

    private int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction faction = Command.getUser(source.getPlayer()).getFaction();
        if (faction.isInvited(player.getUUID())) {
            new Message(target.getName().getString() + " was already invited to your faction").format(ChatFormatting.RED).send(player, false);
            return 0;
        }

        User targetUser = User.get(target.getUUID());
        UUID targetFaction = targetUser.isInFaction() ? targetUser.getFaction().getID() : null;
        if (faction.getID().equals(targetFaction)) {
            new Message(target.getName().getString() + " is already in your faction").format(ChatFormatting.RED).send(player, false);
            return 0;
        }

        faction.invites.add(target.getUUID());

        new Message(target.getName().getString() + " has been invited")
                .send(faction);
        new Message("You have been invited to join this faction").format(ChatFormatting.YELLOW)
                .hover("Click to join").click("/factions join " + faction.getName())
                .prependFaction(faction)
                .send(target, false);
        return 1;
    }

    private int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();
        faction.invites.remove(target.getUUID());

        new Message(target.getName().getString() + " is no longer invited to your faction").send(player, false);
        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("invite")
            .requires(Requires.isCommander())
            .then(
                Commands
                .literal("list")
                .requires(Requires.hasPerms("factions.invite.list", 0))
                .executes(this::list)
            )
            .then(
                Commands
                .literal("add")
                .requires(Requires.hasPerms("factions.invite.add", 0))
                .then(
                    Commands.argument("player", EntityArgument.player())
                    .executes(this::add)
                )
            )
            .then(
                Commands
                .literal("remove")
                .requires(Requires.hasPerms("factions.invite.remove", 0))
                .then(
                    Commands.argument("player", EntityArgument.player())
                    .executes(this::remove)
                )
            )
            .build();
    }
}