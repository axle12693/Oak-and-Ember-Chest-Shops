package com.oakandembermc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.io.File;
import java.nio.file.Path;

import com.oakandembermc.command.ChestShopCommand;
import com.oakandembermc.command.ShopTradeCommand;
import com.oakandembermc.config.ChestShopConfig;
import com.oakandembermc.config.ConfigSyncPayload;
import com.oakandembermc.networking.PayloadRegistrar;
import com.oakandembermc.shop.ShopHologramManager;
import com.oakandembermc.shop.ShopInteractionHandler;
import com.oakandembermc.shop.ShopManager;

/**
 * Main server-side entry point for the ChestShop mod.
 * Handles initialization of config, networking, and core systems.
 */
public class ChestShopMod implements ModInitializer {
    public static final String MOD_ID = "com.oakandembermc.chest_shop";

    @Override
    public void onInitialize() {
        // Register network payloads first
        PayloadRegistrar.registerPayloads();

        // Initialize config
        File configDir = new File("config/" + MOD_ID + "/storagefolder");
        ChestShopConfig.initialize(configDir);

        // Setup storage folder for shop data
        File baseDir = new File("config/" + MOD_ID);
        File storageFolder = new File(baseDir, "storagefolder");
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }

        // Initialize shop manager
        ShopManager.initialize(storageFolder.toPath());
        
        // Register commands
        CommandRegistrationCallback.EVENT.register(ChestShopCommand::register);
        CommandRegistrationCallback.EVENT.register(ShopTradeCommand::register);
        
        // Register interaction handler
        ShopInteractionHandler.register();
        
        // Register world load/unload events for holograms
        ServerWorldEvents.LOAD.register((server, world) -> {
            // Delay hologram creation slightly to ensure world is ready
            server.execute(() -> {
                ShopHologramManager.refreshAllHolograms(world);
            });
        });
        
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            ShopHologramManager.removeAllHolograms(world);
        });

        // Register player join listener to sync config
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Run on server thread to safely access player data
            server.execute(() -> {
                // Sync config to client
                ChestShopConfig config = ChestShopConfig.get();
                ServerPlayNetworking.send(handler.getPlayer(), new ConfigSyncPayload(
                        config.isChestShopsEnabled(),
                        config.getMaxShopsPerPlayer(),
                        config.getShopCreationCost(),
                        config.getTransactionTax()));
            });
        });

        System.out.println("ChestShop mod initialized!");
    }
}
