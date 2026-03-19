package com.meowhuan.commandalias;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class CommandAliasMod implements ModInitializer {
    public static final String MOD_ID = "command_alias";
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CustomCommandManager.initialize(dispatcher);
            ManagementCommands.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(srv -> server = srv);
        ServerLifecycleEvents.SERVER_STOPPED.register(srv -> server = null);
    }

    public static MinecraftServer getServer() {
        return server;
    }
}
