package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.events.RelationshipEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public class DeclareCommand implements Command {
    private int ally(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.ALLY);
    }

    private int neutral(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.NEUTRAL);
    }

    private int enemy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.ENEMY);
    }

    private int updateRelationship(CommandContext<CommandSourceStack> context, Relationship.Status status) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "faction");
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        Faction targetFaction = Faction.getByName(name);

        if (targetFaction == null) {
            new Message("Cannot change faction relationship with a faction that doesn't exist").fail().send(player, false);
            return 0;
        }
        
        Faction sourceFaction = Command.getUser(player).getFaction();

        if (sourceFaction.equals(targetFaction)) {
            new Message("Cannot use the declare command on your own faction").fail().send(player, false);
            return 0;
        }

        if (sourceFaction.getRelationship(targetFaction.getID()).status == status) {
            new Message("That faction relationship has already been declared with this faction").fail().send(player, false);
            return 0;
        }

        Relationship.Status mutual = null;

        if (sourceFaction.getRelationship(targetFaction.getID()).status == targetFaction.getRelationship(sourceFaction.getID()).status) {
            mutual = sourceFaction.getRelationship(targetFaction.getID()).status;
        }

        Relationship rel = new Relationship(targetFaction.getID(), status);
        Relationship rev = targetFaction.getRelationship(sourceFaction.getID());
        sourceFaction.setRelationship(rel);

        MinecraftForge.EVENT_BUS.post(new RelationshipEvents.NewDeclaration(rel));

        Message msgStatus = rel.status == Relationship.Status.ALLY ? new Message("allies").format(ChatFormatting.GREEN) 
        : rel.status == Relationship.Status.ENEMY ? new Message("enemies").format(ChatFormatting.RED) 
        : new Message("neutral");

        if (rel.status == rev.status) {
            MinecraftForge.EVENT_BUS.post(new RelationshipEvents.NewMutual(rel));
            new Message("You are now mutually ").add(msgStatus).add(" with " + targetFaction.getName()).send(sourceFaction);
            new Message("You are now mutually ").add(msgStatus).add(" with " + sourceFaction.getName()).send(targetFaction);
            return 1;
        } else if (mutual != null) {
            MinecraftForge.EVENT_BUS.post(new RelationshipEvents.EndMutual(rel, mutual));
        }

        new Message("You have declared " + targetFaction.getName() + " as ").add(msgStatus).send(sourceFaction);

        if (rel.status != Relationship.Status.NEUTRAL)
            new Message(sourceFaction.getName() + " have declared you as ")
                .add(msgStatus)
                .hover("Click to add them back")
                .click(String.format("/factions declare %s %s", rel.status.toString().toLowerCase(Locale.ROOT), sourceFaction.getName()))
                .send(targetFaction);
      
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands
            .literal("declare")
            .requires(Requires.isLeader())
            .then(
                Commands.literal("ally")
                .requires(Requires.hasPerms("factions.declare.ally", 0))
                .then(
                    Commands.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions(false))
                    .executes(this::ally)
                )
            )
            .then(
                Commands.literal("neutral")
                .requires(Requires.hasPerms("factions.declare.neutral", 0))
                .then(
                    Commands.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions(false))
                    .executes(this::neutral)
                )
            )
            .then(
                Commands.literal("enemy")
                .requires(Requires.hasPerms("factions.declare.enemy", 0))
                .then(
                    Commands.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions(false))
                    .executes(this::enemy)
                )
            )
            .build();
    }
    
}
