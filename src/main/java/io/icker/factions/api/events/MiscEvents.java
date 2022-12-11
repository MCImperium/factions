package io.icker.factions.api.events;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
* Events related to miscellaneous actions
*/
public final class MiscEvents {
    /**
     * Called when the Factions database is saved (which is also when the server saves world and player files)
     */
    public static class OnSave extends Event {
        public final MinecraftServer server;

        public OnSave(MinecraftServer server) {
            this.server = server;
        }
    }
    /**
     * Called when the game attempts to spawn in mobs (UNIMPLEMENTED)
     */
    public static class OnModSpawnAttempt extends Event {
        public OnModSpawnAttempt() {
        }
    }
}
