package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayer player;

    @Final
    @Shadow
    private MinecraftServer server;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    public void onPlayerMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PlayerEvents.OnMove(player));
    }

    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    public void handleDecoratedMessage(PlayerChatMessage signedMessage, CallbackInfo ci) {
        User member = User.get(signedMessage.f_240875_().f_240866_());

        boolean factionChat = member.chat == User.ChatMode.FACTION || member.chat == User.ChatMode.FOCUS;

        if (factionChat && !member.isInFaction()) {
            new Message("You can't send a message to faction chat if you aren't in a faction.")
                .fail()
                .hover("Click to switch to global chat")
                .click("/factions settings chat global")
                .send(server.getPlayerList().getPlayer(signedMessage.f_240875_().f_240866_()), false);

            ci.cancel();
        }
    }

    /*@Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    public void onPlayerInteractEntity(ServerboundInteractPacket packet, CallbackInfo ci) {
        //PlayerInteractEvent.EntityInteract;
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(InteractionHand hand) {

                if (PlayerEvents.USE_ENTITY.invoker().onUseEntity(player, packet.getTarget(player.getLevel()), player.getLevel()) == InteractionResult.FAIL) {
                    ci.cancel();
                }
            }

            @Override
            public void onInteraction(InteractionHand hand, Vec3 pos) {
                if (PlayerEvents.USE_ENTITY.invoker().onUseEntity(player, packet.getTarget(player.getLevel()), player.getLevel()) == InteractionResult.FAIL) {
                    ci.cancel();
                }
            }

            @Override
            public void onAttack() {}
        });
    }*/
}
