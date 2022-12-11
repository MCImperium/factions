package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class LeaveCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        user.leaveFaction();
        new Message(player.getName().getString() + " left").send(faction);
        new Message("You have left this faction.")
            .prependFaction(faction)
            .send(player, false);

        context.getSource().getServer().getPlayerList().sendPlayerPermissionLevel(player);

        if (faction.getUsers().size() == 0) {
            faction.remove();
        } else {
            faction.adjustPower(-FactionsMod.CONFIG.POWER.MEMBER);
        }

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("leave")
            .requires(Requires.multiple(Requires.require(m -> m.isInFaction() && m.rank != User.Rank.OWNER), Requires.hasPerms("factions.leave", 0)))
            .executes(this::run)
            .build();
    }
}