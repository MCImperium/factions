package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Relationship;
import net.minecraftforge.eventbus.api.Event;

/**
 * All events related to relationships
 */
public final class RelationshipEvents {
    /**
     * When a faction is declared as a different status
     */
    public static class NewDeclaration extends Event {
        public final Relationship relationship;
        public NewDeclaration(Relationship relationship) {
            this.relationship = relationship;
        }
    }

    /**
     * When two factions are declared to have the same status
     *
     * For example, mutual allies
     */
    public static class NewMutual extends Event {
        public final Relationship relationship;
        public NewMutual(Relationship relationship) {
            this.relationship = relationship;
        }
    }

    /**
     * When a mutual relationship is ended by either of the two factions
     */
    public static class EndMutual extends Event {
        public final Relationship relationship;
        public final Relationship.Status oldStatus;
        public EndMutual(Relationship relationship, Relationship.Status oldStatus) {
            this.relationship = relationship;
            this.oldStatus = oldStatus;
        }
    }
}
