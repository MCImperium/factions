package io.icker.factions.util;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import net.minecraft.world.level.ChunkPos;

public class DynmapWrapper {
    private DynmapCommonAPI api;
    private MarkerAPI markerApi;
    private MarkerSet markerSet;

    public DynmapWrapper() {
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI dCAPI) {
                api = dCAPI;
                markerApi = api.getMarkerAPI();
                markerSet = markerApi.getMarkerSet("dynmap-factions");
                if (markerSet == null) {
                    markerSet = markerApi.createMarkerSet("dynmap-factions", "The Dynmap Factions integration", null, true);
                }
                generateMarkers();
            }
        });
    }

    private void generateMarkers() {
        for (Faction faction : Faction.all()) {
            Home home = faction.getHome();
            if (home != null) {
                actuallySetHome(faction, home);
            }

            String info = getInfo(faction);
            for (Claim claim : faction.getClaims()) {
                addClaim(claim, info);
            }
        }
    }

    private void addClaim(Claim claim, String factionInfo) {
        Faction faction = claim.getFaction();
        ChunkPos pos = new ChunkPos(claim.x, claim.z);

        AreaMarker marker = markerSet.createAreaMarker(
                claim.getKey(), factionInfo,
                true, dimensionTagToID(claim.level),
                new double[]{pos.getMinBlockX(), pos.getMaxBlockX() + 1},
                new double[]{pos.getMinBlockZ(), pos.getMaxBlockZ() + 1},
                true
        );
        if (marker != null) {
            marker.setFillStyle(marker.getFillOpacity(), faction.getColor().getColor());
            marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.getColor().getColor());
        }
    }

    @SubscribeEvent
    public void addClaim(ClaimEvents.Add event) {
        Claim claim = event.claim;
        addClaim(claim, getInfo(claim.getFaction()));
    }

    @SubscribeEvent
    public void removeClaim(ClaimEvents.Remove event) {
        int x = event.x;
        int z = event.z;
        String level = event.level;
        String areaMarkerId = String.format("%s-%d-%d", level, x, z);
        markerSet.findAreaMarker(areaMarkerId).deleteMarker();
    }
    @SubscribeEvent
    public void updateFaction(FactionEvents.Modify event) {
        Faction faction = event.faction;
        actuallyUpdateFaction(faction);
    }

    @SubscribeEvent
    public void updateFaction(FactionEvents.MemberJoin event) {
        Faction faction = event.faction;
        actuallyUpdateFaction(faction);
    }

    @SubscribeEvent
    public void updateFaction(FactionEvents.MemberLeave event) {
        Faction faction = event.faction;
        actuallyUpdateFaction(faction);
    }

    @SubscribeEvent
    public void updateFaction(FactionEvents.PowerChange event) {
        Faction faction = event.faction;
        actuallyUpdateFaction(faction);
    }

    private void actuallyUpdateFaction(Faction faction) {
        String info = getInfo(faction);

        for (Claim claim : faction.getClaims()) {
            AreaMarker marker = markerSet.findAreaMarker(claim.getKey());

            marker.setFillStyle(marker.getFillOpacity(), faction.getColor().getColor());
            marker.setLineStyle(marker.getLineWeight(), marker.getLineOpacity(), faction.getColor().getColor());
            marker.setDescription(info);
        }
    }

    @SubscribeEvent
    public void setHome(FactionEvents.SetHome event) {
        Faction faction = event.faction;
        Home home = event.home;
        actuallySetHome(faction, home);

    }
    private void actuallySetHome(Faction faction, Home home) {
        Marker marker = markerSet.findMarker(faction.getID().toString() + "-home");
        if (marker == null) {
            markerSet.createMarker("home", faction.getName() + "'s Home", dimensionTagToID(home.level), home.x, home.y, home.z, null, true);
        } else {
            marker.setLocation(dimensionTagToID(home.level), home.x, home.y, home.z);
        }
    }

    private String dimensionTagToID(String level) { // TODO: allow custom dimensions
        if (level.equals("minecraft:overworld")) return "world";
        if (level.equals("minecraft:the_nether")) return "DIM-1";
        if (level.equals("minecraft:the_end")) return "DIM1";
        return level;
    }

    private String getInfo(Faction faction) {
        return "Name: " + faction.getName() + "<br>"
                + "Description: " + faction.getDescription() + "<br>"
                + "Power: " + faction.getPower() + "<br>"
                + "Number of members: " + faction.getUsers().size();// + "<br>"
        //+ "Allies: " + Ally.getAllies(faction.getName).stream().map(ally -> ally.target).collect(Collectors.joining(", "));
    }

    public void reloadAll() {
        markerSet.deleteMarkerSet();
        markerSet = markerApi.createMarkerSet("dynmap-factions", "The Dynmap Factions integration", null, true);
        generateMarkers();
    }
}
