package io.icker.factions.core;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SoundManager {
    public static PlayerList playerManager;

    public static void register() {

    }

    @SubscribeEvent
    public static void add(ClaimEvents.Add event) {
        Claim claim = event.claim;
        playFaction(claim.getFaction(), SoundEvents.NOTE_BLOCK_PLING, 2.0F);
    }

    @SubscribeEvent
    public static void remove(ClaimEvents.Remove event) {
        Faction faction = event.faction;
        playFaction(faction, SoundEvents.NOTE_BLOCK_PLING, 0.5F);
    }

    @SubscribeEvent
    public static void powerChange(ClaimEvents.Remove event) {
        Faction faction = event.faction;
        playFaction(faction, SoundEvents.NOTE_BLOCK_CHIME, 1F);
    }

    @SubscribeEvent
    public static void memberJoin(ClaimEvents.Remove event) {
        Faction faction = event.faction;
        playFaction(faction, SoundEvents.NOTE_BLOCK_BIT, 2.0F);
    }

    @SubscribeEvent
    public static void memberLeave(ClaimEvents.Remove event) {
        Faction faction = event.faction;
        playFaction(faction, SoundEvents.NOTE_BLOCK_BIT, 0.5F);
    }

    private static void playFaction(Faction faction, SoundEvent soundEvent, float pitch) {
        for (User user : faction.getUsers()) {
            ServerPlayer player = FactionsManager.playerManager.getPlayer(user.getID());
            if (player != null && (user.sounds == User.SoundMode.ALL || user.sounds == User.SoundMode.FACTION)) {
                player.playNotifySound(soundEvent, SoundSource.PLAYERS, 0.2F, pitch);
            }
        }
    }

    public static void warningSound(ServerPlayer player) {
        User user = User.get(player.getUUID());
        if (user.sounds == User.SoundMode.ALL || user.sounds == User.SoundMode.WARNINGS) {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }
}
