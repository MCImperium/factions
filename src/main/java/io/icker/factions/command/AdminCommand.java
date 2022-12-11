package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class AdminCommand implements Command {
    private int bypass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();

        User user = User.get(player.getUUID());
        boolean bypass = !user.bypass;
        user.bypass = bypass;

        new Message("Successfully toggled claim bypass")
                .filler("·")
                .add(
                    new Message(user.bypass ? "ON" : "OFF")
                        .format(user.bypass ? ChatFormatting.GREEN : ChatFormatting.RED)
                )
                .send(player, false);

        return 1;
    }

    private int reload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        FactionsMod.dynmap.reloadAll();
        new Message("Reloaded dynmap marker").send(context.getSource().getPlayer(), false);
        return 1;
    }

    private int power(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();

        Faction target = Faction.getByName(StringArgumentType.getString(context, "faction"));
        int power = IntegerArgumentType.getInteger(context, "power");

        int adjusted = target.adjustPower(power);
        if (adjusted != 0) {
            if (power > 0) {
                new Message(
                    "Admin %s added %d power",
                    player.getName().getString(),
                    adjusted
                ).send(target);
                new Message(
                    "Added %d power",
                    adjusted
                ).send(player, false);
            } else {
                new Message(
                    "Admin %s removed %d power",
                    player.getName().getString(),
                    adjusted
                ).send(target);
                new Message(
                    "Removed %d power",
                    adjusted
                ).send(player, false);
            }
        } else {
            new Message("Could not change power").fail().send(player, false);
        }

        return 1;
    }

    private int spoof(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        User user = User.get(player.getUUID());

        String name = StringArgumentType.getString(context, "player");

        User target;

        Optional<GameProfile> profile;
        if ((profile = source.getServer().getProfileCache().get(name)).isPresent()) {
            target = User.get(profile.get().getId());
        } else {
            target = User.get(UUID.fromString(name));
        }

        user.setSpoof(target);

        new Message("Set spoof to player %s", name).send(player, false);

        return 1;
    }

    private int clearSpoof(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        User user = User.get(player.getUUID());

        user.setSpoof(null);

        new Message("Cleared spoof").send(player, false);

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("admin")
            .then(
                Commands.literal("bypass")
                .requires(Requires.hasPerms("factions.admin.bypass", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .executes(this::bypass)
            )
            .then(
                Commands.literal("reload")
                .requires(Requires.multiple(Requires.hasPerms("factions.admin.reload", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL), source -> FactionsMod.dynmap != null))
                .executes(this::reload)
            )
            .then(
                Commands.literal("power")
                .requires(Requires.hasPerms("factions.admin.power", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    Commands.argument("power", IntegerArgumentType.integer())
                    .then(
                        Commands.argument("faction", StringArgumentType.greedyString())
                        .suggests(Suggests.allFactions())
                        .executes(this::power)
                    )
                )
            )
            .then(
                Commands.literal("spoof")
                .requires(Requires.hasPerms("factions.admin.spoof", FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL))
                .then(
                    Commands.argument("player", StringArgumentType.string())
                        .suggests(Suggests.allPlayers())
                        .executes(this::spoof)
                )
                .executes(this::clearSpoof)
            )
            .build();
    }
}
