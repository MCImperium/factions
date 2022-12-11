package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.api.persistents.Relationship.Permissions;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class PermissionCommand implements Command {
    private int change(CommandContext<CommandSourceStack> context, boolean add) {
        String permissionName = StringArgumentType.getString(context, "permission");
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        Faction sourceFaction = User.get(player.getUUID()).getFaction();
        Faction targetFaction = Faction.getByName(StringArgumentType.getString(context, "faction"));

        if (sourceFaction == null || targetFaction == null) {
            new Message("You must be in a faction and you must provide a valid function").fail().send(player, false);
            return 0;
        }

        Relationship rel = sourceFaction.getRelationship(targetFaction.getID());

        Permissions permission;

        try {
            permission = Permissions.valueOf(permissionName);
        } catch (IllegalArgumentException e) {
            new Message("Not a valid permission").fail().send(player, false);
            return 0;
        }

        if ((!rel.permissions.contains(permission) && !add) || (rel.permissions.contains(permission) && add)) {
            new Message(String.format("Could not change because the permission %s", rel.permissions.contains(permission) ? "already exists" : "doesn't exist")).fail().send(player, false);
            return 0;
        }

        if (add) {
            rel.permissions.add(permission);
        } else {
            rel.permissions.remove(permission);
        }

        sourceFaction.setRelationship(rel);

        new Message("Successfully changed permissions").send(player, false);
        return 1;
    }

    private int add(CommandContext<CommandSourceStack> context) {
        return change(context, true);
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        return change(context, false);
    }

    private int changeGuest(CommandContext<CommandSourceStack> context, boolean add) {
        String permissionName = StringArgumentType.getString(context, "permission");
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        Faction faction = User.get(player.getUUID()).getFaction();

        if (faction == null) {
            new Message("You must be in a faction").fail().send(player, false);
            return 0;
        }

        Permissions permission;

        try {
            permission = Permissions.valueOf(permissionName);
        } catch (IllegalArgumentException e) {
            new Message("Not a valid permission").fail().send(player, false);
            return 0;
        }

        if ((!faction.guest_permissions.contains(permission) && !add) || (faction.guest_permissions.contains(permission) && add)) {
            new Message(String.format("Could not change because the permission %s", faction.guest_permissions.contains(permission) ? "already exists" : "doesn't exist")).fail().send(player, false);
            return 0;
        }

        if (add) {
            faction.guest_permissions.add(permission);
        } else {
            faction.guest_permissions.remove(permission);
        }

        new Message("Successfully changed permissions").send(player, false);
        return 1;
    }

    private int addGuest(CommandContext<CommandSourceStack> context) {
        return changeGuest(context, true);
    }

    private int removeGuest(CommandContext<CommandSourceStack> context) {
        return changeGuest(context, false);
    }

    private int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        Faction sourceFaction = User.get(player.getUUID()).getFaction();
        Faction targetFaction = Faction.getByName(StringArgumentType.getString(context, "faction"));

        if (sourceFaction == null || targetFaction == null) {
            new Message("You must be in a faction and you must provide a valid function").fail().send(player, false);
            return 0;
        }

        String permissionsList = sourceFaction.getRelationship(targetFaction.getID()).permissions
            .stream()
            .map(Enum::toString)
            .collect(Collectors.joining(","));

        new Message("")
            .add(
                new Message(targetFaction.getName())
                    .format(targetFaction.getColor())
                    .format(ChatFormatting.BOLD)
            )
            .add(String.format(" has the permissions: %s", permissionsList))
            .send(player, false);

        return 1;
    }
    private int listGuest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        Faction faction = User.get(player.getUUID()).getFaction();

        if (faction == null) {
            new Message("You must be in a faction").fail().send(player, false);
            return 0;
        }

        String permissionsList = faction.guest_permissions
                .stream()
                .map(Enum::toString)
                .collect(Collectors.joining(","));

        new Message(String.format("Guests have the permissions: %s", permissionsList)).send(player, false);
        return 1;
    }


    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("permissions")
            .requires(Requires.multiple(Requires.isLeader(), Requires.hasPerms("factions.permission", 0)))
            .then(
                Commands.literal("add")
                    .requires(Requires.hasPerms("factions.permission.add", 0))
                    .then(
                        Commands.argument("permission", StringArgumentType.word())
                            .suggests(Suggests.enumSuggestion(Permissions.class))
                            .then(
                                Commands.literal("faction")
                                    .requires(Requires.hasPerms("factions.permission.add.faction", 0))
                                    .then(
                                        Commands.argument("faction", StringArgumentType.greedyString())
                                            .suggests(Suggests.allFactions(false))
                                            .executes(this::add)
                                    )
                            )
                            .then(
                                Commands.literal("guest")
                                    .requires(Requires.hasPerms("factions.permission.add.guest", 0))
                                    .executes(this::addGuest)
                            )
                    )
            )
            .then(
                Commands.literal("remove")
                    .requires(Requires.hasPerms("factions.permission.remove", 0))
                    .then(
                        Commands.argument("permission", StringArgumentType.word())
                            .suggests(Suggests.enumSuggestion(Permissions.class))
                            .then(
                                Commands.literal("faction")
                                    .requires(Requires.hasPerms("factions.permission.remove.faction", 0))
                                    .then(
                                        Commands.argument("faction", StringArgumentType.greedyString())
                                            .suggests(Suggests.allFactions(false))
                                            .executes(this::remove)
                                    )
                            )
                            .then(
                                Commands.literal("guest")
                                    .requires(Requires.hasPerms("factions.permission.remove.guest", 0))
                                    .executes(this::removeGuest)
                            )
                    )
            )
            .then(
                Commands.literal("list")
                    .requires(Requires.hasPerms("factions.permission.list", 0))
                        .then(
                            Commands.literal("faction")
                                .requires(Requires.hasPerms("factions.permission.list.faction", 0))
                                .then(
                                    Commands.argument("faction", StringArgumentType.greedyString())
                                        .suggests(Suggests.allFactions(false))
                                        .executes(this::list)
                                )
                        )
                        .then(
                            Commands.literal("guest")
                                .requires(Requires.hasPerms("factions.permission.list.guest", 0))
                                .executes(this::listGuest)
                        )
            )
            .build();
    }
}
