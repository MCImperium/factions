package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import net.minecraftforge.eventbus.api.Event;

/**
* Events related to {@link Claim}
*/
public final class ClaimEvents {
    /**
     * Called when a chunk claim is added by a faction (See {@link Claim})
     */
    public static class Add extends Event {
        public final Claim claim;
        public Add(Claim claim) {
            this.claim = claim;
        }
    }
    /**
     * Called when a faction removes a claim (See {@link Claim})
     */
    public static class Remove extends Event {

        public final int x;
        public final int z;
        public final String level;
        public final Faction faction;
        public Remove(int x, int z, String level, Faction faction) {
            this.x = x;
            this.z = z;
            this.level = level;
            this.faction = faction;
        }
    }
}
