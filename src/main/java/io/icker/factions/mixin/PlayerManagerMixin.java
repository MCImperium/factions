package io.icker.factions.mixin;

import io.icker.factions.api.persistents.User;
import net.minecraft.network.chat.ChatSender;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingPlayerChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.function.Predicate;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @Redirect(method = "m_243140_", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;m_243093_(Lnet/minecraft/network/chat/OutgoingPlayerChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"))
    public void sendChatMessage(ServerPlayer player, OutgoingPlayerChatMessage message, boolean bl, ChatType.Bound parameters, PlayerChatMessage p_243322_, Predicate<ServerPlayer> p_243313_, @Nullable ServerPlayer p_243233_, ChatSender p_243228_, ChatType.Bound p_243291_) {
        if (message instanceof OutgoingPlayerChatMessage.NotTracked) {
            player.m_243093_(message, bl, parameters);
            return;
        }
        
        User sender = User.get(p_243228_.f_240364_());

        User target = User.get(player.getUUID());

        if (sender.chat == User.ChatMode.GLOBAL && target.chat != User.ChatMode.FOCUS) {
            player.m_243093_(message, bl, parameters);
        }

        if ((sender.chat == User.ChatMode.FACTION || sender.chat == User.ChatMode.FOCUS)&& sender.getFaction().equals(target.getFaction())) {
            player.m_243093_(message, bl, parameters);
        }
    }
}
