package io.icker.factions.api.persistents;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.database.Database;
import io.icker.factions.database.Field;
import io.icker.factions.database.Name;
import net.minecraft.ChatFormatting;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Name("Faction")
public class Faction {
    private static final HashMap<UUID, Faction> STORE = Database.load(Faction.class, Faction::getID);

    @Field("ID")
    private UUID id;

    @Field("Name")
    private String name;

    @Field("Description")
    private String description;

    @Field("MOTD")
    private String motd;

    @Field("Color")
    private String color;

    /**
     * Whether a player can join without an invitation
     */
    @Field("Open")
    private boolean open;

    @Field("Power")
    private int power;

    @Field("Home")
    private io.icker.factions.api.persistents.Home home;

    @Field("Safe")
    private SimpleContainer safe = new SimpleContainer(54);

    @Field("Invites")
    public ArrayList<UUID> invites = new ArrayList<>();

    @Field("Relationships")
    private ArrayList<io.icker.factions.api.persistents.Relationship> relationships = new ArrayList<>();

    @Field("GuestPermissions")
    public ArrayList<io.icker.factions.api.persistents.Relationship.Permissions> guest_permissions = new ArrayList<>(FactionsMod.CONFIG.RELATIONSHIPS.DEFAULT_GUEST_PERMISSIONS);

    public Faction(String name, String description, String motd, ChatFormatting color, boolean open, int power) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.motd = motd;
        this.description = description;
        this.color = color.getName();
        this.open = open;
        this.power = power;
    }

    @SuppressWarnings("unused")
    public Faction() {}

    @SuppressWarnings("unused")
    public String getKey() {
        return id.toString();
    }

    @Nullable
    public static Faction get(UUID id) {
        return STORE.get(id);
    }

    @Nullable
    public static Faction getByName(String name) {
        return STORE.values()
            .stream()
            .filter(f -> f.name.equals(name))
            .findFirst()
            .orElse(null);
    }

    public static void add(Faction faction) {
        STORE.put(faction.id, faction);
    }

    public static Collection<Faction> all() {
        return STORE.values();
    }

    @SuppressWarnings("unused")
    public static List<Faction> allBut(UUID id) {
        return STORE.values()
            .stream()
            .filter(f -> f.id != id)
            .toList();
    }

    public UUID getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChatFormatting getColor() {
        return ChatFormatting.getByName(color);
    }

    public String getDescription() {
        return description;
    }

    public String getMOTD() {
        return motd;
    }

    public int getPower() {
        return power;
    }

    public SimpleContainer getSafe() {
        return safe;
    }

    public boolean isOpen() {
        return open;
    }

    public void setName(String name) {
        this.name = name;
        MinecraftForge.EVENT_BUS.post(new FactionEvents.Modify(this));
    }

    public void setDescription(String description) {
        this.description = description;
        MinecraftForge.EVENT_BUS.post(new FactionEvents.Modify(this));
    }

    public void setMOTD(String motd) {
        this.motd = motd;
        MinecraftForge.EVENT_BUS.post(new FactionEvents.Modify(this));
    }

    public void setColor(ChatFormatting color) {
        this.color = color.getName();
        MinecraftForge.EVENT_BUS.post(new FactionEvents.Modify(this));
    }

    public void setOpen(boolean open) {
        this.open = open;
        MinecraftForge.EVENT_BUS.post(new FactionEvents.Modify(this));
    }

    public int adjustPower(int adjustment) {
        int maxPower = calculateMaxPower();
        int newPower = Math.min(Math.max(0, power + adjustment), maxPower);
        int oldPower = this.power;

        if (newPower == oldPower) return 0;

        power = newPower;
        MinecraftForge.EVENT_BUS.post(new FactionEvents.PowerChange(this, oldPower));
        return Math.abs(newPower - oldPower);
    }

    public List<io.icker.factions.api.persistents.User> getUsers() {
        return io.icker.factions.api.persistents.User.getByFaction(id);
    }

    public List<io.icker.factions.api.persistents.Claim> getClaims() {
        return io.icker.factions.api.persistents.Claim.getByFaction(id);
    }

    public void removeAllClaims() {
        io.icker.factions.api.persistents.Claim.getByFaction(id)
            .stream()
            .forEach(io.icker.factions.api.persistents.Claim::remove);
        MinecraftForge.EVENT_BUS.post(new FactionEvents.RemoveAllClaims(this));
    }

    public void addClaim(int x, int z, String level) {
        io.icker.factions.api.persistents.Claim.add(new Claim(x, z, level, id));
    }

    public boolean isInvited(UUID playerID) {
        return invites.stream().anyMatch(invite -> invite.equals(playerID));
    }

    public io.icker.factions.api.persistents.Home getHome() {
        return home;
    }

    public void setHome(Home home) {
        this.home = home;
        MinecraftForge.EVENT_BUS.post(new FactionEvents.SetHome(this, home));
    }

    public io.icker.factions.api.persistents.Relationship getRelationship(UUID target) {
        return relationships.stream().filter(rel -> rel.target.equals(target)).findFirst().orElse(new io.icker.factions.api.persistents.Relationship(target, io.icker.factions.api.persistents.Relationship.Status.NEUTRAL));
    }

    public io.icker.factions.api.persistents.Relationship getReverse(io.icker.factions.api.persistents.Relationship rel) {
        return Faction.get(rel.target).getRelationship(id);
    }

    public boolean isMutualAllies(UUID target) {
        io.icker.factions.api.persistents.Relationship rel = getRelationship(target);
        return rel.status == io.icker.factions.api.persistents.Relationship.Status.ALLY && getReverse(rel).status == io.icker.factions.api.persistents.Relationship.Status.ALLY;
    }

    public List<io.icker.factions.api.persistents.Relationship> getMutualAllies() {
        return relationships.stream().filter(rel -> isMutualAllies(rel.target)).toList();
    }

    public List<io.icker.factions.api.persistents.Relationship> getEnemiesWith() {
        return relationships.stream().filter(rel -> rel.status == io.icker.factions.api.persistents.Relationship.Status.ENEMY).toList();
    }

    public List<io.icker.factions.api.persistents.Relationship> getEnemiesOf() {
        return relationships.stream().filter(rel -> getReverse(rel).status == io.icker.factions.api.persistents.Relationship.Status.ENEMY).toList();
    }

    public void removeRelationship(UUID target) {
        relationships = new ArrayList<>(relationships.stream().filter(rel -> !rel.target.equals(target)).toList());
    }

    public void setRelationship(io.icker.factions.api.persistents.Relationship relationship) {
        if (getRelationship(relationship.target) != null) {
            removeRelationship(relationship.target);
        }
        if (relationship.status != io.icker.factions.api.persistents.Relationship.Status.NEUTRAL || !relationship.permissions.isEmpty())
            relationships.add(relationship);
    }

    public void remove() {
        for (User user : getUsers()) {
            user.leaveFaction();
        }
        for (Relationship rel : relationships) {
            Faction.get(rel.target).removeRelationship(id);
        }
        removeAllClaims();
        STORE.remove(id);
        MinecraftForge.EVENT_BUS.post(new FactionEvents.Disband(this));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Faction faction = (Faction) o;
        return id.equals(faction.id);
    }

    public static void save() {
        Database.save(Faction.class, STORE.values().stream().toList());
    }

//  TODO(samu): import per-player power patch
    public int calculateMaxPower(){
        return FactionsMod.CONFIG.POWER.BASE + (getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER);
    }
}