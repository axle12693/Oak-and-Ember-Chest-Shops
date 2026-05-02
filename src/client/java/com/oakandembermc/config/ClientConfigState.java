package com.oakandembermc.config;

/**
 * Client-side config state holder.
 * Values are synced from the server when the player joins.
 * Use this to check config values on the client side.
 */
public class ClientConfigState {
    private ClientConfigState() {
        /* This utility class should not be instantiated */
    }

    private static boolean chestShopsEnabled = true;
    private static int maxShopsPerPlayer = -1;
    private static int shopCreationCost = 0;
    private static int transactionTax = 0;
    private static boolean synced = false;

    /**
     * Update the client config state from a server sync packet.
     */
    public static void updateFromServer(boolean chestShopsEnabled, int maxShopsPerPlayer,
            int shopCreationCost, int transactionTax) {
        ClientConfigState.chestShopsEnabled = chestShopsEnabled;
        ClientConfigState.maxShopsPerPlayer = maxShopsPerPlayer;
        ClientConfigState.shopCreationCost = shopCreationCost;
        ClientConfigState.transactionTax = transactionTax;
        ClientConfigState.synced = true;
        System.out.println("ChestShop mod: Config synced from server - enabled=" + chestShopsEnabled +
                ", maxShops=" + maxShopsPerPlayer + ", creationCost=" + shopCreationCost +
                ", tax=" + transactionTax + "%");
    }

    /**
     * Reset to defaults (e.g., when disconnecting from server).
     */
    public static void reset() {
        chestShopsEnabled = true;
        maxShopsPerPlayer = -1;
        shopCreationCost = 0;
        transactionTax = 0;
        synced = false;
    }

    /**
     * Check if the chest shop system is enabled.
     */
    public static boolean isChestShopsEnabled() {
        return chestShopsEnabled;
    }

    /**
     * Get the maximum shops per player (-1 for unlimited).
     */
    public static int getMaxShopsPerPlayer() {
        return maxShopsPerPlayer;
    }

    /**
     * Get the cost in diamonds to create a shop.
     */
    public static int getShopCreationCost() {
        return shopCreationCost;
    }

    /**
     * Get the transaction tax percentage.
     */
    public static int getTransactionTax() {
        return transactionTax;
    }

    /**
     * Check if config has been synced from the server.
     * Useful for waiting until config is available.
     */
    public static boolean isSynced() {
        return synced;
    }
}
