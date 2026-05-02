package com.oakandembermc;

import com.oakandembermc.config.ClientConfigState;
import com.oakandembermc.config.ConfigSyncPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side entry point for the ChestShop mod.
 * Handles client initialization, networking, and UI registration.
 */
@Environment(EnvType.CLIENT)
public class ChestShopModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register config sync handler
        registerConfigSyncHandler();

        // Reset client config state when disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientConfigState.reset();
        });

        // Future: Register client-side commands, screens, etc.
        // registerCommands();
        // registerScreens();

        System.out.println("ChestShop mod client initialized!");
    }

    /**
     * Register the handler for config sync packets from the server.
     */
    private void registerConfigSyncHandler() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            // Update client config state on the render thread
            context.client().execute(() -> {
                ClientConfigState.updateFromServer(
                        payload.chestShopsEnabled(),
                        payload.maxShopsPerPlayer(),
                        payload.shopCreationCost(),
                        payload.transactionTax());
            });
        });
    }
}
