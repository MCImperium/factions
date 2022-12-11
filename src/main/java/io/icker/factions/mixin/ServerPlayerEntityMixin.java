package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends LivingEntity {

    protected ServerPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(at = @At("HEAD"), method = "updateOptions")
    public void setClientSettings(ServerboundClientInformationPacket packet, CallbackInfo info) {
        User member = User.get(((ServerPlayer)(Object) this).getUUID());
        member.language = packet.language();
    }

    @Inject(at = @At("HEAD"), method = "die")
    public void onDeath(DamageSource source, CallbackInfo info) {
        Entity entity = source.getDirectEntity();
        if (entity == null || !entity.isAlwaysTicking()) return;
        MinecraftForge.EVENT_BUS.post(new PlayerEvents.OnKilledByPlayer((ServerPlayer) (Object) this, source));
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo info) {
        if (tickCount % FactionsMod.CONFIG.POWER.POWER_TICKS.TICKS != 0 || tickCount == 0) return;
        MinecraftForge.EVENT_BUS.post(new PlayerEvents.OnPowerTick((ServerPlayer) (Object) this));
    }

    @Inject(method = "isInvulnerableTo", at = @At("RETURN"), cancellable = true)
    private void isInvulnerableTo(DamageSource damageSource, CallbackInfoReturnable<Boolean> info) {
        Entity source = damageSource.getEntity();
        if (source == null) return;
        Event.Result result = new PlayerEvents.IsInvulnerable(source, (ServerPlayer) (Object) this).getResult();
        if (result != Event.Result.DEFAULT) info.setReturnValue(result == Event.Result.ALLOW);
    }

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    public void getPlayerListName(CallbackInfoReturnable<Component> cir) {
        if (FactionsMod.CONFIG.DISPLAY.TAB_MENU) {
            User member = User.get(((ServerPlayer) (Object) this).getUUID());
            if (member.isInFaction()) {
                Faction faction = member.getFaction();
                cir.setReturnValue(new Message(String.format("[%s] ", faction.getName())).format(faction.getColor()).add(
                        new Message(((ServerPlayer) (Object) this).getName().getString()).format(ChatFormatting.WHITE)
                ).raw());
            } else {
                cir.setReturnValue(new Message("[FACTIONLESS] ").format(ChatFormatting.GRAY).add(
                        new Message(((ServerPlayer) (Object) this).getName().getString()).format(ChatFormatting.WHITE)
                ).raw());
            }
        }
    }
}