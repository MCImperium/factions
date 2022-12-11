package io.icker.factions.command;


import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class HomeCommand implements Command {
    private int go(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        Faction faction = Command.getUser(player).getFaction();
        Home home = faction.getHome();

        if (home == null) {
            new Message("No faction home set").fail().send(player, false);
            return 0;
        }

        if (player.getServer() == null) return 0;

        Optional<ResourceKey<Level>> worldKey = player.getServer().levelKeys().stream().filter(key -> Objects.equals(key.location(), new ResourceLocation(home.level))).findAny();

        if (worldKey.isEmpty()) {
            new Message("Cannot find dimension").fail().send(player, false);
            return 0;
        }

        ServerLevel world = player.getServer().getLevel(worldKey.get());

        if (checkLimitToClaim(faction, world, new BlockPos(home.x, home.y, home.z))) {
            new Message("Cannot warp home to an unclaimed chunk").fail().send(player, false);
            return 0;
        }

        CombatEntry damageRecord = player.getCombatTracker().getLastEntry();
        if (damageRecord == null || player.tickCount - damageRecord.getTime() > FactionsMod.CONFIG.HOME.DAMAGE_COOLDOWN) {
            player.teleportTo(
                    world,
                    home.x, home.y, home.z,
                    home.yaw, home.pitch
            );
            new Message("Warped to faction home").send(player, false);
        } else {
            new Message("Cannot warp while in combat").fail().send(player, false);
        }
        return 1;
    }

    private int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();

        if (checkLimitToClaim(faction, player.getLevel(), player.blockPosition())) {
            new Message("Cannot set home to an unclaimed chunk").fail().send(player, false);
            return 0;
        }

        Home home = new Home(
            faction.getID(),
            player.getX(), player.getY(), player.getZ(),
            player.getYHeadRot(), player.getXRot(),
            player.getLevel().dimension().location().toString()
        );

        faction.setHome(home);
        new Message(
            "Home set to %.2f, %.2f, %.2f by %s",
            home.x,
            home.y,
            home.z,
            player.getName().getString()
        ).send(faction);
        return 1;
    }

    private static boolean checkLimitToClaim(Faction faction, ServerLevel world, BlockPos pos) {
        if (!FactionsMod.CONFIG.HOME.CLAIM_ONLY) return false;

        ChunkPos chunkPos = world.getChunk(pos).getPos();
        String dimension = world.dimension().location().toString();

        Claim possibleClaim = Claim.get(chunkPos.x, chunkPos.z, dimension);
        return possibleClaim == null || possibleClaim.getFaction().getID() != faction.getID();
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("home")
            .requires(Requires.multiple(Requires.isMember(), s -> FactionsMod.CONFIG.HOME != null, Requires.hasPerms("factions.home", 0)))
            .executes(this::go)
            .then(
                Commands.literal("set")
                .requires(Requires.multiple(Requires.hasPerms("factions.home.set", 0), Requires.isLeader()))
                .executes(this::set)
            )
            .build();
    }
}
