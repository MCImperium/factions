package io.icker.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.command.*;
import io.icker.factions.config.Config;
import io.icker.factions.core.*;
import io.icker.factions.util.Command;
import io.icker.factions.util.DynmapWrapper;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(FactionsMod.MODID)
public class FactionsMod {
    public static Logger LOGGER = LogManager.getLogger("Factions");
    public static final String MODID = "factions";

    public static Config CONFIG = Config.load();
    public static DynmapWrapper dynmap;
    //public static LuckPerms api;

    public FactionsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onInitialize);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onInitialize(final FMLCommonSetupEvent event) {
        LOGGER.info("Initialized Factions Mod for Minecraft v1.19");

        dynmap = ModList.get().isLoaded("dynmap") ? new DynmapWrapper() : null;

        ChatManager.register();
        FactionsManager.register();
        InteractionManager.register();
        ServerManager.register();
        SoundManager.register();
        WorldManager.register();

        MinecraftForge.EVENT_BUS.register(ClaimEvents.class);
        if (dynmap != null) {
            MinecraftForge.EVENT_BUS.register(dynmap);
        }
        MinecraftForge.EVENT_BUS.register(WorldManager.class);
        MinecraftForge.EVENT_BUS.register(ChatManager.class);
        MinecraftForge.EVENT_BUS.register(FactionsManager.class);
        MinecraftForge.EVENT_BUS.register(InteractionManager.class);
        MinecraftForge.EVENT_BUS.register(ServerManager.class);
        MinecraftForge.EVENT_BUS.register(SoundManager.class);
        MinecraftForge.EVENT_BUS.register(WorldManager.class);
    }

    @SubscribeEvent
    public void startServer(ServerStartedEvent event) {
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext registryAccess = event.getBuildContext();
        Commands.CommandSelection environment = event.getCommandSelection();
        LiteralCommandNode<CommandSourceStack> factions = Commands
            .literal("factions")
            .build();

        LiteralCommandNode<CommandSourceStack> alias = Commands
            .literal("f")
            .build();

        dispatcher.getRoot().addChild(factions);
        dispatcher.getRoot().addChild(alias);

        Command[] commands = new Command[] {
            new AdminCommand(),
            new SettingsCommand(),
            new ClaimCommand(),
            new CreateCommand(),
            new DeclareCommand(),
            new DisbandCommand(),
            new HomeCommand(),
            new InfoCommand(),
            new InviteCommand(),
            new JoinCommand(),
            new KickCommand(),
            new LeaveCommand(),
            new ListCommand(),
            new MapCommand(),
            new MemberCommand(),
            new ModifyCommand(),
            new RankCommand(),
            new SafeCommand(),
            new PermissionCommand()
        };

        for (Command command : commands) {
            factions.addChild(command.getNode());
            alias.addChild(command.getNode());
        }
    }
}
