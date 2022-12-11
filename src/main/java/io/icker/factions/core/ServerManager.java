package io.icker.factions.core;

import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerManager {
    public static void register() {
    }

    @SubscribeEvent
    public static void save(MiscEvents.OnSave event) {
        Claim.save();
        Faction.save();
        User.save();
    }

    @SubscribeEvent
    public static void save(ServerStoppingEvent event) {
        Claim.save();
        Faction.save();
        User.save();
    }

    @SubscribeEvent
    public static void saveLvl(LevelEvent.Save event) {
        Claim.save();
        Faction.save();
        User.save();
    }



    @SubscribeEvent
    public static void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        User user = User.get(player.getUUID());

        if (user.isInFaction()) {
            Faction faction = user.getFaction();
            new Message("Welcome back " + player.getName().getString() + "!").send(player, false);
            new Message(faction.getMOTD()).prependFaction(faction).send(player, false);
        }
    }
}