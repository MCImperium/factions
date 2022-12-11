package io.icker.factions.api.persistents;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Name("Claim")
public class Claim {
    private static final HashMap<String, Claim> STORE = Database.load(Claim.class, c -> c.getKey());

    @Field("X")
    public int x;

    @Field("Z")
    public int z;

    /**
     * The dimension of the claim
     */
    @Field("Level")
    public String level;

    @Field("FactionID")
    public UUID factionID;

    @Field("AccessLevel")
    public Rank accessLevel;

    public Claim(int x, int z, String level, UUID factionID) {
        this.x = x;
        this.z = z;
        this.level = level;
        this.factionID = factionID;
        this.accessLevel = Rank.MEMBER;
    }

    @SuppressWarnings("unused")
    public Claim() {}

    public String getKey() {
        return String.format("%s-%d-%d", level, x, z);
    }

    public static Claim get(int x, int z, String level) {
        return STORE.get(String.format("%s-%d-%d", level, x, z));
    }

    public static List<Claim> getByFaction(UUID factionID) {
        return STORE.values()
            .stream()
            .filter(c -> c.factionID.equals(factionID))
            .toList();
    }

    public static void add(Claim claim) {
        STORE.put(claim.getKey(), claim);
        MinecraftForge.EVENT_BUS.post(new ClaimEvents.Add(claim));
    }

    public io.icker.factions.api.persistents.Faction getFaction() {
        return io.icker.factions.api.persistents.Faction.get(factionID);
    }

    public void remove() {
        STORE.remove(getKey());
        MinecraftForge.EVENT_BUS.post(new ClaimEvents.Remove(x, z, level, Faction.get(factionID)));
    }

    public static void save() {
        Database.save(Claim.class, STORE.values().stream().toList());
    }
}
