package io.icker.factions.mixin;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.User;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> info) {
        if (FactionsMod.CONFIG.SAFE == null || !FactionsMod.CONFIG.SAFE.ENDER_CHEST) return;

        Event event = new PlayerEvents.OpenSafe(player, User.get(player.getUUID()).getFaction());
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) {
            info.setReturnValue(InteractionResult.FAIL);
        }
    }
}
