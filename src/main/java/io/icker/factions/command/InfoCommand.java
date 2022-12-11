package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
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

public class InfoCommand implements Command {
    private int self(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message("Command can only be used whilst in a faction").fail().send(player, false);
            return 0;
        }

        return info(player, user.getFaction());
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

        return info(player, faction);
    }

    public static int info(ServerPlayer player, Faction faction) {
        List<User> users = faction.getUsers();

        GameProfileCache cache = player.getServer().getProfileCache();
        String owner = ChatFormatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.OWNER)
                .map(user -> cache.get(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        String usersList = users.stream()
            .map(user -> cache.get(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
            .collect(Collectors.joining(", "));
        
        String mutualAllies = faction.getMutualAllies().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(ChatFormatting.GRAY + ", "));

        String enemiesWith = ChatFormatting.GRAY + faction.getEnemiesWith().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(ChatFormatting.GRAY + ", "));

        int requiredPower = faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower = users.size() * FactionsMod.CONFIG.POWER.MEMBER + FactionsMod.CONFIG.POWER.BASE;

        // generate the ---
        int numDashes = 32 - faction.getName().length();
        String dashes = new StringBuilder("--------------------------------").substring(0, numDashes/2);

        new Message(ChatFormatting.BLACK + dashes + "[ " + faction.getColor() + faction.getName() + ChatFormatting.BLACK + " ]" + dashes)
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Description: ")
            .add(ChatFormatting.WHITE + faction.getDescription())
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Owner: ")
            .add(ChatFormatting.WHITE + owner)
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Members (" + ChatFormatting.WHITE.toString() + users.size() + ChatFormatting.GOLD.toString() + "): ")
            .add(usersList)
            .send(player, false);
        new Message(ChatFormatting.GOLD + "Power: ")
            .add(ChatFormatting.GREEN.toString() + faction.getPower() + slash() + requiredPower + slash() + maxPower)
            .hover("Current / Required / Max")
            .send(player, false);
        new Message(ChatFormatting.GREEN + "Allies (" + ChatFormatting.WHITE + faction.getMutualAllies().size() + ChatFormatting.GREEN + "): ")
            .add(mutualAllies)
            .send(player, false);
        new Message(ChatFormatting.RED + "Enemies (" + ChatFormatting.WHITE + faction.getEnemiesWith().size() + ChatFormatting.RED + "): ")
            .add(enemiesWith)
            .send(player, false);

        return 1;
    }

    private static String slash() {
        return ChatFormatting.GRAY + " / " + ChatFormatting.GREEN;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("info")
            .requires(Requires.hasPerms("factions.info", 0))
            .executes(this::self)
            .then(
                Commands.argument("faction", StringArgumentType.greedyString())
                .requires(Requires.hasPerms("factions.info.other", 0))
                .suggests(Suggests.allFactions())
                .executes(this::any)
            )
            .build();
    }
}
