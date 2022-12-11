package io.icker.factions.mixin;

import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.icker.factions.api.events.MiscEvents;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Shadow @Final private static Logger LOGGER;

    @Inject(at = @At("HEAD"), method = "saveAllChunks(ZZZ)Z")
    public void save(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> ci) {
        LOGGER.warn("Saving chunks from factions!");
        Claim.save();
        Faction.save();
        User.save();
        MinecraftForge.EVENT_BUS.post(new MiscEvents.OnSave((MinecraftServer) (Object) this));
    }
}
