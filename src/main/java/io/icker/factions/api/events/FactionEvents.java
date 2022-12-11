package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.api.persistents.User;
import net.minecraftforge.eventbus.api.Event;

/**
 * Events related to {@link Faction}
 */
public final class FactionEvents {
    /**
     * Called when a {@link Faction} is created
     */
    public static class Create extends Event {
        public final Faction faction;
        public final User owner;

        public Create(Faction faction, User owner) {
            this.faction = faction;
            this.owner = owner;
        }
    }

    /**
     * Called when a {@link Faction} is disbanded
     */
    public static class Disband extends Event {
        public final Faction faction;

        public Disband(Faction faction) {
            this.faction = faction;
        }
    }

    /**
     * Called when a {@link User} joins a {@link Faction}
     */
    public static class MemberJoin extends Event {
        public final Faction faction;
        public final User user;

        public MemberJoin(Faction faction, User user) {
            this.faction = faction;
            this.user = user;
        }
    }

    /**
     * Called when a {@link User} leaves a {@link Faction}
     */
    public static class MemberLeave extends Event {
        public final Faction faction;
        public final User user;
        public MemberLeave(Faction faction, User user) {
            this.faction = faction;
            this.user = user;
        }
    }

    /**
     * Called when a factions name, description, MOTD, color or open status is modified
     */
    public static class Modify extends Event {
        public final Faction faction;
        public Modify(Faction faction) {
            this.faction = faction;
        }
    }

    /**
     * Called when a factions power changes
     */
    public static class PowerChange extends Event {
        public final Faction faction;
        public final int oldPower;

        public PowerChange(Faction faction, int oldPower) {
            this.faction = faction;
            this.oldPower = oldPower;
        }
    }

    /**
     * Called when a faction sets its {@link Home}
     */
    public static class SetHome extends Event {
        public final Faction faction;
        public final Home home;
        public SetHome(Faction faction, Home home) {
            this.faction = faction;
            this.home = home;
        }
    }

    /**
     * Called when a faction removes all its claims. (Note that each claim will also run a {@link ClaimEvents} REMOVE event)
     */
    public static class RemoveAllClaims extends Event {
        public final Faction faction;

        public RemoveAllClaims(Faction faction) {
            this.faction = faction;
        }
    }
}
