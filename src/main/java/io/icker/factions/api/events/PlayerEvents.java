package io.icker.factions.api.events;

import io.icker.factions.api.persistents.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * Events related to player actions
 */
public class PlayerEvents {
    /**
     * Called when a player tries to interact with an entity
     */
    @Event.HasResult
    public static class UseEntity extends PlayerEvent {


        public final ServerPlayer player;
        public final Entity entity;
        public final Level world;

        public UseEntity(ServerPlayer player, Entity entity, Level world) {
            super(player);
            this.player = player;
            this.entity = entity;
            this.world = world;
        }
    }

    @Event.HasResult
    public static class PlaceBlock extends Event {
        public final UseOnContext context;

        public PlaceBlock(UseOnContext context) {
            this.context = context;
        }
    }

    /**
     * Called when a player tries to use a block that has an inventory (uses the locking mechanism)
     */
    @Event.HasResult
    public static class UseInventory extends Event {
        public final Player player;
        public final BlockPos pos;
        public final Level world;

        public UseInventory(Player player, BlockPos pos, Level world) {
            this.player = player;
            this.pos = pos;
            this.world = world;
        }
    }


    /**
     * Called when a player is attacked and decides whether to allow the hit
     */
    @Event.HasResult
    public static class IsInvulnerable extends Event {
        public final Entity source;
        public final Entity target;
        public IsInvulnerable(Entity source, Entity target) {
            this.source = source;
            this.target = target;
        }
    }

    /**
     * Called when a player moves
     */
    public static class OnMove extends Event {
        public final ServerPlayer player;
        public OnMove(ServerPlayer player) {
            this.player = player;
        }
    }

    /**
     * Called when a player is killed by another player
     */
    public static class OnKilledByPlayer extends Event {
        public final ServerPlayer player;
        public final DamageSource source;
        public OnKilledByPlayer(ServerPlayer player, DamageSource source) {
            this.player = player;
            this.source = source;
        }
    }

    /**
     * Called on a power reward will be given
     */
    public static class OnPowerTick extends Event {
        public final ServerPlayer player;
        public OnPowerTick(ServerPlayer player) {
            this.player = player;
        }
    }

    /**
     * Called when a player attempts to open a safe
     */
    @Event.HasResult
    public static class OpenSafe extends Event {
        public final Player player;
        public final Faction faction;
        public OpenSafe(Player player, Faction faction) {
            this.player = player;
            this.faction = faction;
        }
    }
}
