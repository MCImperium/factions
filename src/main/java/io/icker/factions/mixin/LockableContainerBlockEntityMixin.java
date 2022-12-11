package io.icker.factions.mixin;

import io.icker.factions.api.events.PlayerEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseContainerBlockEntity.class)
public class LockableContainerBlockEntityMixin {
    @Inject(method = "canOpen", at = @At("RETURN"), cancellable = true)
    private void checkUnlocked(Player player, CallbackInfoReturnable<Boolean> cir) {
        Event event = new PlayerEvents.UseInventory(player, ((BaseContainerBlockEntity) (Object) this).getBlockPos(), ((BaseContainerBlockEntity) (Object) this).getLevel());
        MinecraftForge.EVENT_BUS.post(event);
        Event.Result result = event.getResult();
        cir.setReturnValue(cir.getReturnValue() && result != Event.Result.DENY);
    }
}
