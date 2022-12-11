package io.icker.factions.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.server.level.ServerPlayer;

public class ModifyCommand implements Command {
    private int name(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (FactionsMod.CONFIG.DISPLAY.NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
            new Message("Cannot rename a faction to that name as it is on the blacklist").fail().send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH >= 0 & FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH > name.length()) {
            new Message("Cannot rename a faction to this that as it is too long").fail().send(player, false);
            return 0;
        }

        if (Faction.getByName(name) != null) {
            new Message("A faction with that name already exists").fail().send(player, false);
            return 0;
        }

        Faction faction = Command.getUser(player).getFaction();

        faction.setName(name);
        new Message("Successfully renamed faction to '" + name + "'")
            .prependFaction(faction)
            .send(player, false);

        return 1;
    }

    private int description(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String description = StringArgumentType.getString(context, "description");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setDescription(description);
        new Message("Successfully updated faction description to '" + description + "'")
            .prependFaction(faction)
            .send(player, false);

        return 1;
    }

    private int motd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String motd = StringArgumentType.getString(context, "motd");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setMOTD(motd);
        new Message("Successfully updated faction MOTD to '" + motd + "'")
            .prependFaction(faction)
            .send(player, false);

        return 1;
    }

    private int color(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ChatFormatting color = ColorArgument.getColor(context, "color");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setColor(color);
        new Message("Successfully updated faction color to " + ChatFormatting.BOLD + color + color.name())
            .prependFaction(faction)
            .send(player, false);

        return 1;
    }

    private int open(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean open = BoolArgumentType.getBool(context, "open");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        faction.setOpen(open);
        new Message("Successfully updated faction to ")
            .add(
                new Message(open ? "Open" : "Closed")
                    .format(open ? ChatFormatting.GREEN : ChatFormatting.RED)
            )
            .prependFaction(faction)
            .send(player, false);
            
        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("modify")
            .requires(Requires.isLeader())
            .then(
                Commands
                .literal("name")
                .requires(Requires.multiple(Requires.hasPerms("factions.modify.name", 0), Requires.isOwner()))
                .then(
                    Commands.argument("name", StringArgumentType.greedyString())
                    .executes(this::name)
                )
            )
            .then(
                Commands
                .literal("description")
                .requires(Requires.hasPerms("factions.modify.description", 0))
                .then(
                    Commands.argument("description", StringArgumentType.greedyString())
                    .executes(this::description)
                )
            )
            .then(
                Commands
                .literal("motd")
                .requires(Requires.hasPerms("factions.modify.motd", 0))
                .then(
                    Commands.argument("motd", StringArgumentType.greedyString())
                    .executes(this::motd)
                )
            )
            .then(
                Commands
                .literal("color")
                .requires(Requires.hasPerms("factions.modify.color", 0))
                .then(
                    Commands.argument("color", ColorArgument.color())
                    .executes(this::color)
                )
            )
            .then(
                Commands
                .literal("open")
                .requires(Requires.hasPerms("factions.modify.open", 0))
                .then(
                    Commands.argument("open", BoolArgumentType.bool())
                    .executes(this::open)
                )
            )
            .build();
    }
}
