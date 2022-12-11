package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship.Permissions;
import io.icker.factions.api.persistents.User;
import io.icker.factions.mixin.BucketItemMixin;
import io.icker.factions.mixin.ItemMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InteractionManager {
    public static void register() {

    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        Level world = (Level) event.getLevel();
        Player player = event.getPlayer();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);
        boolean result = checkPermissions(player, pos, world, Permissions.BREAK_BLOCKS) == InteractionResult.FAIL;
        if (result) {
            io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) player, "break blocks");
        }
        event.setResult(result ? Event.Result.DENY : Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level world = event.getLevel();
        InteractionHand hand = event.getHand();
        BlockHitResult hitResult = event.getHitVec();
        ItemStack stack = player.getItemInHand(hand);

        BlockPos hitPos = hitResult.getBlockPos();
        if (checkPermissions(player, hitPos, world, Permissions.USE_BLOCKS) == InteractionResult.FAIL) {
            io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) player, "use blocks");
            io.icker.factions.core.InteractionsUtil.sync(player, stack, hand);
            event.setCanceled(true);
            return;
        }

        BlockPos placePos = hitPos.offset(hitResult.getDirection().getNormal());
        if (checkPermissions(player, placePos, world, Permissions.USE_BLOCKS) == InteractionResult.FAIL) {
            io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) player, "use blocks");
            io.icker.factions.core.InteractionsUtil.sync(player, stack, hand);
            event.setCanceled(true);
            return;
        }

        //return InteractionResult.PASS;
        // no-op
    }

    @SubscribeEvent
    public static void onPlaceBlock(PlayerEvents.PlaceBlock event) {
        UseOnContext context = event.context;
        if (checkPermissions(context.getPlayer(), context.getClickedPos(), context.getLevel(), Permissions.PLACE_BLOCKS) == InteractionResult.FAIL) {
            io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) context.getPlayer(), "place blocks");
            io.icker.factions.core.InteractionsUtil.sync(context.getPlayer(), context.getItemInHand(), context.getHand());
            event.setResult(Event.Result.DENY);
            return;
        }

        //return InteractionResult.PASS;
        //no-op
    }

    @SubscribeEvent
    public static void onUseBucket(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level world = event.getLevel();
        InteractionHand hand = event.getHand();
        Item item = player.getItemInHand(hand).getItem();

        if (item instanceof BucketItem) {
            InteractionResult playerResult = checkPermissions(player, player.blockPosition(), world, Permissions.PLACE_BLOCKS);
            if (playerResult == InteractionResult.FAIL) {
                io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) player, "pick up/place liquids");
                io.icker.factions.core.InteractionsUtil.sync(player, player.getItemInHand(hand), hand);
                event.setUseItem(Event.Result.DENY);
                return;
                //return InteractionResultHolder.fail(player.getItemInHand(hand));
            }

            Fluid fluid = ((BucketItemMixin) item).getFluid();
            net.minecraft.world.level.ClipContext.Fluid handling = fluid == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;

            BlockHitResult raycastResult = ItemMixin.raycast(world, player, handling);

            if (raycastResult.getType() != BlockHitResult.Type.MISS) {
                BlockPos raycastPos = raycastResult.getBlockPos();
                if (checkPermissions(player, raycastPos, world, Permissions.PLACE_BLOCKS) == InteractionResult.FAIL) {
                    io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) player, "pick up/place liquids");
                    io.icker.factions.core.InteractionsUtil.sync(player, player.getItemInHand(hand), hand);
                    event.setUseItem(Event.Result.DENY);
                    return;
                    //return InteractionResultHolder.fail(player.getItemInHand(hand));
                }

                BlockPos placePos = raycastPos.offset(raycastResult.getDirection().getNormal());
                if (checkPermissions(player, placePos, world, Permissions.PLACE_BLOCKS) == InteractionResult.FAIL) {
                    io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) player, "pick up/place liquids");
                    io.icker.factions.core.InteractionsUtil.sync(player, player.getItemInHand(hand), hand);
                    event.setUseItem(Event.Result.DENY);
                    return;
                    //return InteractionResultHolder.fail(player.getItemInHand(hand));
                }
            }
        }


        //return InteractionResultHolder.pass(player.getItemInHand(hand));
        //no-op
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level world = event.getEntity().level;
        Entity entity = event.getTarget();
        if (entity != null && checkPermissions(player, entity.blockPosition(), world, Permissions.ATTACK_ENTITIES) == InteractionResult.FAIL) {
            io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) player, "attack entities");
            event.setCanceled(true);
        }

        //return InteractionResult.PASS;
        //no-op
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerEvents.UseEntity event) {

        Player player = event.player;
        Entity entity = event.entity;
        Level world = event.world;
        if (checkPermissions(player, entity.blockPosition(), world, Permissions.USE_ENTITIES) == InteractionResult.FAIL) {
            io.icker.factions.core.InteractionsUtil.warn((ServerPlayer) player, "use entities");
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onUseInventory(PlayerEvents.UseInventory event) {
        Player player = event.player;
        BlockPos pos = event.pos;
        Level world = event.world;
        if (checkPermissions(player, pos, world, Permissions.USE_INVENTORIES) == InteractionResult.FAIL) {
            InteractionsUtil.warn((ServerPlayer) player, "use inventories");
            event.setResult(Event.Result.DENY);
        }

        //return InteractionResult.PASS;
    }

    @SubscribeEvent
    public static void isInvulnerableTo(PlayerEvents.IsInvulnerable event) {
        Entity source = event.source;
        Entity target = event.target;
        if (!source.isAlwaysTicking() || FactionsMod.CONFIG.FRIENDLY_FIRE) return;

        User sourceUser = User.get(source.getUUID());
        User targetUser = User.get(target.getUUID());

        if (!sourceUser.isInFaction() || !targetUser.isInFaction()) {
            return;
        }

        Faction sourceFaction = sourceUser.getFaction();
        Faction targetFaction = targetUser.getFaction();

        if (sourceFaction.getID() == targetFaction.getID()) {
            event.setResult(Event.Result.ALLOW);
            return;
        }

        if (sourceFaction.isMutualAllies(targetFaction.getID())) {
            event.setResult(Event.Result.ALLOW);
            return;
        }
    }

    private static InteractionResult checkPermissions(Player player, BlockPos position, Level world, Permissions permission) {
        User user = User.get(player.getUUID());
        if (player.hasPermissions(FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL) && user.bypass) {
            return InteractionResult.PASS;
        }

        String dimension = world.dimension().location().toString();
        ChunkPos chunkPosition = world.getChunk(position).getPos();

        Claim claim = Claim.get(chunkPosition.x, chunkPosition.z, dimension);
        if (claim == null) return InteractionResult.PASS;

        Faction claimFaction = claim.getFaction();

        if (claimFaction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT > claimFaction.getPower()) {
            return InteractionResult.PASS;
        }

        if (!user.isInFaction()) {
            return InteractionResult.FAIL;
        }

        Faction userFaction = user.getFaction();

        if (claimFaction == userFaction && (getRankLevel(claim.accessLevel) <= getRankLevel(user.rank) || (user.rank == User.Rank.GUEST && claimFaction.guest_permissions.contains(permission) && claim.accessLevel == User.Rank.MEMBER))) {
            return InteractionResult.SUCCESS;
        }

        if (FactionsMod.CONFIG.RELATIONSHIPS.ALLY_OVERRIDES_PERMISSIONS && claimFaction.isMutualAllies(userFaction.getID()) && claim.accessLevel == User.Rank.MEMBER) {
            return InteractionResult.SUCCESS;
        }

        if (claimFaction.getRelationship(userFaction.getID()).permissions.contains(permission) && claim.accessLevel == User.Rank.MEMBER) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private static int getRankLevel(User.Rank rank) {
        switch (rank) {
            case OWNER -> {
                return 3;
            }
            case LEADER -> {
                return 2;
            }
            case COMMANDER -> {
                return 1;
            }
            case MEMBER -> {
                return 0;
            }
            case GUEST -> {
                return -1;
            }
            default -> {
                return -2;
            }
        }
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Entity entity = event.getTarget();
        Level world = event.getLevel();
        Event ev = new PlayerEvents.UseEntity(player, entity, world);
        MinecraftForge.EVENT_BUS.post(ev);
        if (ev.getResult() == Event.Result.DENY) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        Entity entity = event.getTarget();
        Level world = event.getLevel();
        Event ev = new PlayerEvents.UseEntity(player, entity, world);
        MinecraftForge.EVENT_BUS.post(ev);
        if (ev.getResult() == Event.Result.DENY) {
            event.setCanceled(true);
        }
    }
}
