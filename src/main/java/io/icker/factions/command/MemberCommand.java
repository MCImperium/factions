package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;

public class MemberCommand implements Command {
    private int self(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message("Command can only be used whilst in a faction").fail().send(player, false);
            return 0;
        }

        return members(player, user.getFaction());
    }

    private int any(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message("Faction does not exist").fail().send(player, false);
            return 0;
        }

        return members(player, faction);
    }

    public static int members(ServerPlayer player, Faction faction) {
        List<User> users = faction.getUsers();
        GameProfileCache cache = player.getServer().getProfileCache();

        long memberCount = users.stream().filter(u -> u.rank == User.Rank.MEMBER).count();
        String members = ChatFormatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.MEMBER)
                .map(user -> cache.get(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        long commanderCount = users.stream().filter(u -> u.rank == User.Rank.COMMANDER).count();
        String commanders = ChatFormatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.COMMANDER)
                .map(user -> cache.get(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        long leaderCount = users.stream().filter(u -> u.rank == User.Rank.LEADER).count();
        String leaders = ChatFormatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.LEADER)
                .map(user -> cache.get(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        String owner = ChatFormatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.OWNER)
                .map(user -> cache.get(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        // generate the ---
        int numDashes = 32 - faction.getName().length();
        String dashes = new StringBuilder("--------------------------------").substring(0, numDashes/2);

        new Message(ChatFormatting.BLACK + dashes + "[ " + faction.getColor() + faction.getName() + ChatFormatting.BLACK + " ]" + dashes)
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Total Members: ")
            .add(ChatFormatting.WHITE.toString() + users.size())
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Owner: ")
            .add(owner)
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Leaders (" + ChatFormatting.WHITE.toString() + leaderCount + ChatFormatting.GOLD.toString() + "): ")
            .add(leaders)
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Commanders (" + ChatFormatting.WHITE.toString() + commanderCount + ChatFormatting.GOLD.toString() + "): ")
            .add(commanders)
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Members (" + ChatFormatting.WHITE.toString() + memberCount + ChatFormatting.GOLD.toString() + "): ")
            .add(members)
            .send(player, false);

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("members")
            .requires(Command.Requires.hasPerms("factions.members", 0))
            .executes(this::self)
            .then(
                Commands.argument("faction", StringArgumentType.greedyString())
                    .requires(Command.Requires.hasPerms("factions.members.other", 0))
                    .suggests(Command.Suggests.allFactions())
                    .executes(this::any)
            )
            .build();
    }
}
