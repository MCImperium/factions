package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class ChatManager {
    public static void register() {

    }

    @SubscribeEvent
    public static void handleChat(ServerChatEvent event) {
        if (FactionsMod.CONFIG.DISPLAY.MODIFY_CHAT) {
            Component message = ChatManager.handleMessage(event.getPlayer(), event.getMessage().getString());
            event.setMessage(message);
        }
        // no-op
    }

    public static Component handleMessage(ServerPlayer sender, String message) {
        UUID id = sender.getUUID();
        User member = User.get(id);

        if (member.chat == User.ChatMode.GLOBAL) {
            if (member.isInFaction()) {
                return ChatManager.inFactionGlobal(sender, member.getFaction(), message);
            } else {
                return ChatManager.global(sender, message);
            }
        } else {
            if (member.isInFaction()) {
                return ChatManager.faction(sender, member.getFaction(), message);
            } else {
                return ChatManager.global(sender, message);
            }
        }
    }

    private static Component global(ServerPlayer sender, String message) {
        return new Message(message).format(ChatFormatting.GRAY)
                .raw();
    }

    private static Component inFactionGlobal(ServerPlayer sender, Faction faction, String message) {
        return new Message("")
                .add(new Message(faction.getName()).format(ChatFormatting.BOLD, faction.getColor()))
                .filler("»")
                .add(new Message(message).format(ChatFormatting.GRAY))
                .raw();
    }

    private static Component faction(ServerPlayer sender, Faction faction, String message) {
        return new Message("")
                .add(new Message("F").format(ChatFormatting.BOLD, faction.getColor()))
                .filler("»")
                .add(new Message(message).format(ChatFormatting.GRAY))
                .raw();
    }
}
