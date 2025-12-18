package com.oakandembermc.networking;

import com.oakandembermc.config.ConfigSyncPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers all network payloads for the ChestShop mod.
 * Call registerPayloads() early in mod initialization.
 */
public class PayloadRegistrar {
    public static boolean registered = false;

    public static void registerPayloads() {
        if (!registered) {
            // Config sync payload (server to client)
            PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);

            // Future: Add chest shop specific payloads here
            // e.g., ShopCreateRequestPayload, ShopDataSyncPayload, etc.

            registered = true;
        }
    }
}
