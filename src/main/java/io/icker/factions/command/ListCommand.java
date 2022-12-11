package io.icker.factions.command;

import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

public class ListCommand implements Command {
    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Collection<Faction> factions = Faction.all();
        int size = factions.size();

        new Message("There %s ", size == 1 ? "is" : "are")
                .add(new Message(String.valueOf(size)).format(ChatFormatting.YELLOW))
                .add(" faction%s", size == 1 ? "" : "s")
                .send(player, false);

        if (size == 0) return 1;

        Message list = new Message("");
        for (Faction faction : factions) {
            String name = faction.getName();
            list.add(new Message(name).click("/factions info " + name).format(faction.getColor())).add(", ");
        }

        list.send(player, false);
        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("list")
            .requires(Requires.hasPerms("factions.list", 0))
            .executes(this::run)
            .build();
    }
}