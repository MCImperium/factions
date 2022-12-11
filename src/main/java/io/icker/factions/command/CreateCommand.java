package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class CreateCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (FactionsMod.CONFIG.DISPLAY.NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
            new Message("Cannot create a faction with this name as it is on the blacklist").fail().send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH >= 0 & FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH < name.length()) {
            new Message("Cannot create a faction with this name as it is too long").fail().send(player, false);
            return 0;
        }

        if (Faction.getByName(name) != null) {
            new Message("Cannot create a faction as a one with that name already exists").fail().send(player, false);
            return 0;
        }

        Faction faction = new Faction(name, "No description set", "No faction MOTD set", ChatFormatting.WHITE, false, FactionsMod.CONFIG.POWER.BASE + FactionsMod.CONFIG.POWER.MEMBER);
        Faction.add(faction);
        Command.getUser(player).joinFaction(faction.getID(), User.Rank.OWNER);

        source.getServer().getPlayerList().sendPlayerPermissionLevel(player);
        new Message("Successfully created faction").send(player, false);
        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("create")
            .requires(Requires.multiple(Requires.isFactionless(), Requires.hasPerms("factions.create", 0)))
            .then(
                Commands.argument("name", StringArgumentType.greedyString()).executes(this::run)
            )
            .build();
    }
}