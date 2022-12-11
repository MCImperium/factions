package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class SettingsCommand implements Command{
    private int setChat(CommandContext<CommandSourceStack> context, User.ChatMode option) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();
        User user = User.get(player.getUUID());
        user.chat = option;   

        new Message("Successfully set your chat preference")
            .filler("·")
            .add(
                new Message(user.getChatName())
                    .format(ChatFormatting.BLUE)
            )
            .send(player, false);

        return 1;
    }

    private int setSounds(CommandContext<CommandSourceStack> context, User.SoundMode option) {
        ServerPlayer player = context.getSource().getPlayer();
        User user = User.get(player.getUUID());
        user.sounds = option;

        new Message("Successfully set your sound preference")
            .filler("·")
            .add(
                new Message(user.getSoundName())
                    .format(ChatFormatting.BLUE)
            )
            .send(player, false);
            
        return 1;
    }

    private int radar(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        User config = User.get(player.getUUID());
        boolean radar = !config.radar;
        config.radar = radar;

        new Message("Successfully toggled claim radar")
            .filler("·")
            .add(
                new Message(radar ? "ON" : "OFF")
                    .format(radar ? ChatFormatting.GREEN : ChatFormatting.RED)
            )
            .send(player, false);

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("settings")
            .requires(Requires.hasPerms("factions.settings", 0))
            .then(
                Commands.literal("chat")
                .requires(Requires.hasPerms("factions.settings.chat", 0))
                .then(Commands.literal("global").executes(context -> setChat(context, User.ChatMode.GLOBAL)))
                .then(Commands.literal("faction").executes(context -> setChat(context, User.ChatMode.FACTION)))
                .then(Commands.literal("focus").executes(context -> setChat(context, User.ChatMode.FOCUS)))
            )
            .then(
                Commands.literal("radar")
                .requires(Requires.hasPerms("factions.settings.radar", 0))
                .executes(this::radar)
            )
            .then(
                Commands.literal("sounds")
                .requires(Requires.hasPerms("factions.settings.sounds", 0))
                .then(Commands.literal("none").executes(context -> setSounds(context, User.SoundMode.NONE)))
                .then(Commands.literal("warnings").executes(context -> setSounds(context, User.SoundMode.WARNINGS)))
                .then(Commands.literal("faction").executes(context -> setSounds(context, User.SoundMode.FACTION)))
                .then(Commands.literal("all").executes(context -> setSounds(context, User.SoundMode.ALL)))
            )
            .build();
    }
}
