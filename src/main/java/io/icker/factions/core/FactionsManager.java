package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class FactionsManager {
    public static PlayerList playerManager;

    public static void register() {

    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        playerManager = server.getPlayerList();
        Message.manager = server.getPlayerList();
    }

    @SubscribeEvent
    public static void factionModified(FactionEvents.Modify event) {
        Faction faction = event.faction;
        ServerPlayer[] players = faction.getUsers()
                .stream()
                .map(user -> playerManager.getPlayer(user.getID()))
                .filter(player -> player != null)
                .toArray(ServerPlayer[]::new);
        updatePlayerList(players);
    }

    @SubscribeEvent
    public static void memberJoin(FactionEvents.MemberJoin event) {
        Faction faction = event.faction;
        User user = event.user;
        handleMemberChange(faction, user);
    }

    @SubscribeEvent
    public static void memberLeave(FactionEvents.MemberLeave event) {
        Faction faction = event.faction;
        User user = event.user;
        handleMemberChange(faction, user);
    }

    private static void handleMemberChange(Faction faction, User user) {
        ServerPlayer player = playerManager.getPlayer(user.getID());
        if (player != null) {
            updatePlayerList(player);
        }
    }

    @SubscribeEvent
    public static void playerDeath(PlayerEvents.OnKilledByPlayer event) {
        ServerPlayer player = event.player;
        User member = User.get(player.getUUID());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(-FactionsMod.CONFIG.POWER.DEATH_PENALTY);
        new Message(
                "%s lost %d power from dying",
                player.getName().getString(),
                adjusted
        ).send(faction);
    }

    @SubscribeEvent
    public static void powerTick(PlayerEvents.OnPowerTick event) {
        ServerPlayer player = event.player;
        User member = User.get(player.getUUID());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(FactionsMod.CONFIG.POWER.POWER_TICKS.REWARD);
        if (adjusted != 0)
            new Message(
                    "%s gained %d power from surviving",
                    player.getName().getString(),
                    adjusted
            ).send(faction);
    }

    private static void updatePlayerList(ServerPlayer... players) {
        playerManager.broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, List.of(players)));
    }

    @SubscribeEvent
    public static void openSafe(PlayerEvents.OpenSafe event) {
        Player player = event.player;
        Faction faction = event.faction;
        User user = User.get(player.getUUID());

        if (!user.isInFaction()) {
            if (FactionsMod.CONFIG.SAFE != null && FactionsMod.CONFIG.SAFE.ENDER_CHEST) {
                new Message("Cannot use enderchests when not in a faction").fail().send(player, false);
                event.setResult(Event.Result.DENY);
                return;
            }
            //return InteractionResult.PASS;
            //no-op
        }

        player.openMenu(
                new SimpleMenuProvider(
                        (syncId, inventory, p) -> {
                            if (FactionsMod.CONFIG.SAFE.DOUBLE) {
                                return ChestMenu.sixRows(syncId, inventory, faction.getSafe());
                            } else {
                                return ChestMenu.threeRows(syncId, inventory, faction.getSafe());
                            }
                        },
                        Component.nullToEmpty(String.format("%s's Safe", faction.getName()))
                )
        );

        //return InteractionResult.SUCCESS;
    }
}
