package com.oakandembermc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.io.File;

import com.oakandembermc.config.ChestShopConfig;
import com.oakandembermc.config.ConfigSyncPayload;
import com.oakandembermc.networking.PayloadRegistrar;

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

        // Future: Initialize shop manager, commands, etc.
        // ShopManager.initialize(storageFolder);
        // ChestShopCommand.register();

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
