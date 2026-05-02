package com.oakandembermc;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.oakandembermc.shop.ChestShop;
import com.oakandembermc.shop.ShopManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ChestShopAPI {

    private static ShopManager shopManager;

    private static Function<Player, Integer> balanceGetter;
    private static BiConsumer<Player, Integer> balanceAdder;
    private static String currencyName = "Diamond";
    private static String currencyNamePlural = "Diamonds";

    private ChestShopAPI() {
    }

    public static void initialize(ShopManager manager) {
        if (shopManager != null) {
            throw new IllegalStateException("ChestShopAPI is already initialized!");
        }
        shopManager = manager;
    }

    public static ChestShop getShop(UUID shopId) {
        ensureInitialized();
        return shopManager.getShop(shopId);
    }

    public static ChestShop getShopAt(BlockPos position, Level world) {
        ensureInitialized();
        return shopManager.getShopAt(position, world);
    }

    public static ChestShop getShopAt(BlockPos position, String dimensionId) {
        ensureInitialized();
        return shopManager.getShopAt(position, dimensionId);
    }

    public static List<ChestShop> getShopsByOwner(UUID ownerUuid) {
        ensureInitialized();
        return shopManager.getShopsByOwner(ownerUuid);
    }

    public static Collection<ChestShop> getAllShops() {
        ensureInitialized();
        return shopManager.getAllShops();
    }

    public static int getShopCount() {
        ensureInitialized();
        return shopManager.getAllShops().size();
    }

    public static boolean hasShopAt(BlockPos position, Level world) {
        ensureInitialized();
        return shopManager.getShopAt(position, world) != null;
    }

    public static void registerCurrencyProvider(
            Function<Player, Integer> getBalance,
            BiConsumer<Player, Integer> addBalance,
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

    public static boolean hasCustomCurrencyProvider() {
        return balanceGetter != null;
    }

    public static int getCurrencyBalance(Player player) {
        if (balanceGetter != null) {
            return balanceGetter.apply(player);
        }
        return -1;
    }

    public static boolean addCurrencyBalance(Player player, int amount) {
        if (balanceAdder != null) {
            balanceAdder.accept(player, amount);
            return true;
        }
        return false;
    }

    public static String getCurrencyName() {
        return currencyName;
    }

    public static String getCurrencyNamePlural() {
        return currencyNamePlural;
    }

    public static String formatCurrency(int amount) {
        if (amount == 1) {
            return "1 " + currencyName;
        }
        return amount + " " + currencyNamePlural;
    }

    private static void ensureInitialized() {
        if (shopManager == null) {
            throw new IllegalStateException("ChestShopAPI has not been initialized yet!");
        }
    }
}
