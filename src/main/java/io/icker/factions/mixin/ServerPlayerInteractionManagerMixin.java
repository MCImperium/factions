package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerInteractionManagerMixin {
    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    public InteractionResult place(ItemStack instance, UseOnContext context) {
        Event ev = new PlayerEvents.PlaceBlock(context);
        MinecraftForge.EVENT_BUS.post(ev);
        if (ev.getResult() == Event.Result.DENY) {
            return InteractionResult.FAIL;
        }
        return instance.useOn(context);
    }
}
