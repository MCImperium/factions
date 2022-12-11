package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class WorldManager {
    public static void register() {

    }

    @SubscribeEvent
    public static void onMobSpawnAttempt(MiscEvents.OnModSpawnAttempt event) {
        // TODO Implement this
    }

    @SubscribeEvent
    public static void onMove(PlayerEvents.OnMove event) {
        ServerPlayer player = event.player;
        User user = User.get(player.getUUID());
        ServerLevel world = player.getLevel();
        String dimension = world.dimension().location().toString();

        ChunkPos chunkPos = world.getChunk(player.blockPosition()).getPos();

        Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);
        if (user.autoclaim && claim == null) {
            Faction faction = user.getFaction();
            int requiredPower = (faction.getClaims().size() + 1) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
            int maxPower = faction.getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER + FactionsMod.CONFIG.POWER.BASE;

            if (maxPower < requiredPower) {
                new Message("Not enough faction power to claim chunk, autoclaim toggled off").fail().send(player, false);
                user.autoclaim = false;
            } else {
                faction.addClaim(chunkPos.x, chunkPos.z, dimension);
                claim = Claim.get(chunkPos.x, chunkPos.z, dimension);
                new Message(
                        "Chunk (%d, %d) claimed by %s",
                        chunkPos.x,
                        chunkPos.z,
                        player.getName().getString()
                ).send(faction);
            }
        }
        if (user.radar) {
            if (claim != null) {
                new Message(claim.getFaction().getName())
                        .format(claim.getFaction().getColor())
                        .send(player, true);
            } else {
                new Message("Wilderness")
                        .format(ChatFormatting.GREEN)
                        .send(player, true);
            }
        }
    }
}
