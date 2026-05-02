package com.oakandembermc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.io.File;

import com.oakandembermc.command.ChestShopCommand;
import com.oakandembermc.command.LookupCommand;
import com.oakandembermc.command.ShopTradeCommand;
import com.oakandembermc.config.ChestShopConfig;
import com.oakandembermc.config.ConfigSyncPayload;
import com.oakandembermc.networking.PayloadRegistrar;
import com.oakandembermc.shop.ShopHologramManager;
import com.oakandembermc.shop.ShopInteractionHandler;
import com.oakandembermc.shop.ShopManager;

public class ChestShopMod implements ModInitializer {
    public static final String MOD_ID = "com.oakandembermc.chest_shop";

    @Override
    public void onInitialize() {
        PayloadRegistrar.registerPayloads();

        File configDir = new File("config/" + MOD_ID + "/storagefolder");
        ChestShopConfig.initialize(configDir);

        File baseDir = new File("config/" + MOD_ID);
        File storageFolder = new File(baseDir, "storagefolder");
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }

        ShopManager.initialize(storageFolder.toPath());
        ChestShopAPI.initialize(ShopManager.get());

        CommandRegistrationCallback.EVENT.register(ChestShopCommand::register);
        CommandRegistrationCallback.EVENT.register(LookupCommand::register);
        CommandRegistrationCallback.EVENT.register(ShopTradeCommand::register);

        ShopInteractionHandler.register();

        ServerLevelEvents.LOAD
                .register((server, level) -> server.execute(() -> ShopHologramManager.refreshAllHolograms(level)));

        ServerLevelEvents.UNLOAD.register((server, level) -> ShopHologramManager.removeAllHolograms(level));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
            ChestShopConfig config = ChestShopConfig.get();
            ServerPlayNetworking.send(handler.getPlayer(), new ConfigSyncPayload(
                    config.isChestShopsEnabled(),
                    config.getMaxShopsPerPlayer(),
                    config.getShopCreationCost(),
                    config.getTransactionTax()));
        }));

        System.out.println("ChestShop mod initialized!");
    }
}
