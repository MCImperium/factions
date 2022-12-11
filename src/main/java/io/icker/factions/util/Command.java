package io.icker.factions.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraftforge.fml.ModList;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;


public interface Command {
    LiteralCommandNode<CommandSourceStack> getNode();
    //public static final boolean permissions = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
    //boolean permissions = ModList.get().isLoaded("luckperms");
    boolean permissions = false;

    interface Requires {
        boolean run(User user);

        @SafeVarargs
        static Predicate<CommandSourceStack> multiple(Predicate<CommandSourceStack>... args) {
            return source -> {
                for (Predicate<CommandSourceStack> predicate : args) {
                    if (!predicate.test(source)) return false;
                }

                return true;
            };
        }

        static Predicate<CommandSourceStack> isFactionless() {
            return require(user -> !user.isInFaction());
        }

        static Predicate<CommandSourceStack> isMember() {
            return require(user -> user.isInFaction());
        }

        static Predicate<CommandSourceStack> isCommander() {
            return require(user -> user.rank == User.Rank.COMMANDER || user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER);
        }

        static Predicate<CommandSourceStack> isLeader() {
            return require(user -> user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER);
        }

        static Predicate<CommandSourceStack> isOwner() {
            return require(user -> user.rank == User.Rank.OWNER);
        }
        
        static Predicate<CommandSourceStack> isAdmin() {
            return source -> source.hasPermission(FactionsMod.CONFIG.REQUIRED_BYPASS_LEVEL);
        }

        static Predicate<CommandSourceStack> hasPerms(String permission, int defaultValue) {
            return source -> true;
            /*return source -> {
                ServerPlayer player = source.getPlayer();
                net.luckperms.api.model.user.User user = FactionsMod.api.getPlayerAdapter(ServerPlayer.class).getUser(player);
                return !permissions || hasPermission(user, permission);
            };*/
        }

        static boolean hasPermission(net.luckperms.api.model.user.User user, String permission) {
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        }

        static Predicate<CommandSourceStack> require(Requires req) {
            return source -> {
                ServerPlayer entity = source.getPlayer();
                User user = Command.getUser(entity);
                return req.run(user);
            };
        }
    }

    interface Suggests {
        String[] run(User user);

        static SuggestionProvider<CommandSourceStack> allFactions() {
            return allFactions(true);
        }

        static SuggestionProvider<CommandSourceStack> allFactions(boolean includeYou) {
            return suggest(user -> 
                Faction.all()
                    .stream()
                    .filter(f -> includeYou || !user.isInFaction() || !user.getFaction().getID().equals(f.getID()))
                    .map(f -> f.getName())
                    .toArray(String[]::new)
            );
        }

        static SuggestionProvider<CommandSourceStack> allPlayers() {
            return (context, builder) -> {
                GameProfileCache cache = context.getSource().getServer().getProfileCache();

                for (User user : User.all()) {
                    Optional<GameProfile> player;
                    if ((player = cache.get(user.getID())).isPresent()) {
                        builder.suggest(player.get().getName());
                    } else {
                        builder.suggest(user.getID().toString());
                    }
                }
                return builder.buildFuture();
            };
        }

        static SuggestionProvider<CommandSourceStack> openFactions() {
            return suggest(user ->
                Faction.all()
                    .stream()
                    .filter(f -> f.isOpen())
                    .map(f -> f.getName())
                    .toArray(String[]::new)
            );
        }

        static SuggestionProvider<CommandSourceStack> openInvitedFactions() {
            return suggest(user ->
                Faction.all()
                    .stream()
                    .filter(f -> f.isOpen() || f.isInvited(user.getID()))
                    .map(f -> f.getName())
                    .toArray(String[]::new)
            );
        }

        static <T extends Enum<T>> SuggestionProvider<CommandSourceStack> enumSuggestion (Class<T> clazz) {
            return suggest(user ->
                    Arrays.stream(clazz.getEnumConstants())
                        .map(Enum::toString)
                        .toArray(String[]::new)
            );
        }

        static SuggestionProvider<CommandSourceStack> suggest(Suggests sug) {
            return (context, builder) -> {
                ServerPlayer entity = context.getSource().getPlayer();
                User user = User.get(entity.getUUID());
                for (String suggestion : sug.run(user)) {
                    builder.suggest(suggestion);
                }
                return builder.buildFuture();
            };
        }
    }

    static User getUser(ServerPlayer player) {
        User user = User.get(player.getUUID());
        if (user.getSpoof() == null) {
            return user;
        }
        return user.getSpoof();
    }
}