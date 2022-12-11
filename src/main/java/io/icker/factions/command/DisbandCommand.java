package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class DisbandCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        new Message(player.getName().getString() + " disbanded the faction").send(faction);
        faction.remove();

        PlayerList manager = source.getServer().getPlayerList();
        for (ServerPlayer p : manager.getPlayers()) {
            manager.sendPlayerPermissionLevel(p);
        }
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("disband")
            .requires(Requires.multiple(Requires.isOwner(), Requires.hasPerms("factions.disband", 0)))
            .executes(this::run)
            .build();
    }
}