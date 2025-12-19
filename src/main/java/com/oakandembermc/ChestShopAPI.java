package com.oakandembermc;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.oakandembermc.shop.ChestShop;
import com.oakandembermc.shop.ShopManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Public API for interacting with the ChestShop system.
 * <p>
 * Other mods can use this API to query chest shops after the
 * ChestShopMod has initialized the system.
 */
public final class ChestShopAPI {

    // The backing ShopManager instance.
    private static ShopManager shopManager;

    // Custom currency provider callbacks (null = use default diamond-based system)
    private static Function<PlayerEntity, Integer> balanceGetter;
    private static BiConsumer<PlayerEntity, Integer> balanceAdder;
    private static String currencyName = "Diamond";
    private static String currencyNamePlural = "Diamonds";

    // Private constructor to prevent instantiation.
    private ChestShopAPI() {
    }

    /**
     * Initializes the API with the given ShopManager.
     * This is called during mod initialization.
     *
     * @param manager the ShopManager instance to use
     */
    public static void initialize(ShopManager manager) {
        if (shopManager != null) {
            throw new IllegalStateException("ChestShopAPI is already initialized!");
        }
        shopManager = manager;
    }

    /**
     * Gets a shop by its unique ID.
     *
     * @param shopId the shop's UUID
     * @return the ChestShop, or null if not found
     */
    public static ChestShop getShop(UUID shopId) {
        ensureInitialized();
        return shopManager.getShop(shopId);
    }

    /**
     * Gets a shop at the given block position in the given world.
     *
     * @param position the block position of the chest
     * @param world the world the chest is in
     * @return the ChestShop at that location, or null if none exists
     */
    public static ChestShop getShopAt(BlockPos position, World world) {
        ensureInitialized();
        return shopManager.getShopAt(position, world);
    }

    /**
     * Gets a shop at the given block position in the given dimension.
     *
     * @param position the block position of the chest
     * @param dimensionId the dimension registry key string (e.g., "minecraft:overworld")
     * @return the ChestShop at that location, or null if none exists
     */
    public static ChestShop getShopAt(BlockPos position, String dimensionId) {
        ensureInitialized();
        return shopManager.getShopAt(position, dimensionId);
    }

    /**
     * Gets all shops owned by a specific player.
     *
     * @param ownerUuid the player's UUID
     * @return a list of shops owned by that player (may be empty)
     */
    public static List<ChestShop> getShopsByOwner(UUID ownerUuid) {
        ensureInitialized();
        return shopManager.getShopsByOwner(ownerUuid);
    }

    /**
     * Gets all registered shops.
     *
     * @return an unmodifiable collection of all shops
     */
    public static Collection<ChestShop> getAllShops() {
        ensureInitialized();
        return shopManager.getAllShops();
    }

    /**
     * Gets the total number of registered shops.
     *
     * @return the shop count
     */
    public static int getShopCount() {
        ensureInitialized();
        return shopManager.getAllShops().size();
    }

    /**
     * Checks if a shop exists at the given location.
     *
     * @param position the block position
     * @param world the world
     * @return true if a shop exists at that location
     */
    public static boolean hasShopAt(BlockPos position, World world) {
        ensureInitialized();
        return shopManager.getShopAt(position, world) != null;
    }

    // ========== Currency Provider API ==========

    /**
     * Registers a custom currency provider to replace the default diamond-based system.
     * <p>
     * When registered, the ChestShop mod will use these callbacks instead of
     * counting/adding/removing diamonds from player inventories.
     * <p>
     * Example usage from EconomyMod:
     * <pre>
     * ChestShopAPI.registerCurrencyProvider(
     *     player -> (int) EconomyAPI.getBalance(player.getUuid()),
     *     (player, amount) -> {
     *         double current = EconomyAPI.getBalance(player.getUuid());
     *         EconomyAPI.setBalance(player.getUuid(), current + amount);
     *     },
     *     "Coin",
     *     "Coins"
     * );
     * </pre>
     *
     * @param getBalance function that returns the player's current balance
     * @param addBalance consumer that adds (or subtracts if negative) from the player's balance
     * @param singularName the singular name of the currency (e.g., "Coin")
     * @param pluralName the plural name of the currency (e.g., "Coins")
     */
    public static void registerCurrencyProvider(
            Function<PlayerEntity, Integer> getBalance,
            BiConsumer<PlayerEntity, Integer> addBalance,
            String singularName,
            String pluralName) {
        if (balanceGetter != null) {
            System.out.println("ChestShopAPI: Warning - overwriting existing currency provider");
        }
        balanceGetter = getBalance;
        balanceAdder = addBalance;
        currencyName = singularName;
        currencyNamePlural = pluralName;
        System.out.println("ChestShopAPI: Custom currency provider registered (" + singularName + ")");
    }

    /**
     * Checks if a custom currency provider has been registered.
     *
     * @return true if a custom provider is active
     */
    public static boolean hasCustomCurrencyProvider() {
        return balanceGetter != null;
    }

    /**
     * Gets the player's currency balance.
     * Uses the custom provider if registered, otherwise counts diamonds.
     *
     * @param player the player
     * @return the player's balance
     */
    public static int getCurrencyBalance(PlayerEntity player) {
        if (balanceGetter != null) {
            return balanceGetter.apply(player);
        }
        // Fall back to default diamond counting (handled by CurrencyHandler)
        return -1; // Signal to use default
    }

    /**
     * Adds to (or subtracts from) the player's currency balance.
     * Uses the custom provider if registered, otherwise manipulates diamonds.
     *
     * @param player the player
     * @param amount the amount to add (negative to subtract)
     * @return true if handled by custom provider, false to use default
     */
    public static boolean addCurrencyBalance(PlayerEntity player, int amount) {
        if (balanceAdder != null) {
            balanceAdder.accept(player, amount);
            return true;
        }
        return false; // Signal to use default
    }

    /**
     * Gets the singular name of the currency (e.g., "Diamond" or "Coin").
     *
     * @return the currency name
     */
    public static String getCurrencyName() {
        return currencyName;
    }

    /**
     * Gets the plural name of the currency (e.g., "Diamonds" or "Coins").
     *
     * @return the plural currency name
     */
    public static String getCurrencyNamePlural() {
        return currencyNamePlural;
    }

    /**
     * Formats a currency amount for display.
     *
     * @param amount the amount
     * @return formatted string like "1 Coin" or "5 Diamonds"
     */
    public static String formatCurrency(int amount) {
        if (amount == 1) {
            return "1 " + currencyName;
        }
        return amount + " " + currencyNamePlural;
    }

    // ========== Internal ==========

    private static void ensureInitialized() {
        if (shopManager == null) {
            throw new IllegalStateException("ChestShopAPI has not been initialized yet!");
        }
    }
}
