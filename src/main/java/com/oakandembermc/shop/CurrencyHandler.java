package com.oakandembermc.shop;

import com.oakandembermc.ChestShopAPI;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CurrencyHandler {
    private CurrencyHandler() {
        /* This utility class should not be instantiated */
    }

    public static int getBalance(Player player) {
        if (ChestShopAPI.hasCustomCurrencyProvider()) {
            return ChestShopAPI.getCurrencyBalance(player);
        }
        return getDiamondCount(player);
    }

    private static int getDiamondCount(Player player) {
        int count = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.DIAMOND)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static boolean hasBalance(Player player, int amount) {
        return getBalance(player) >= amount;
    }

    public static boolean withdraw(Player player, int amount) {
        if (!hasBalance(player, amount)) {
            return false;
        }

        if (ChestShopAPI.hasCustomCurrencyProvider()) {
            ChestShopAPI.addCurrencyBalance(player, -amount);
            return true;
        }

        return withdrawDiamonds(player, amount);
    }

    private static boolean withdrawDiamonds(Player player, int amount) {
        int remaining = amount;
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.DIAMOND)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;

                if (stack.isEmpty()) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        return remaining == 0;
    }

    public static void deposit(Player player, int amount) {
        if (ChestShopAPI.hasCustomCurrencyProvider()) {
            ChestShopAPI.addCurrencyBalance(player, amount);
            return;
        }

        depositDiamonds(player, amount);
    }

    private static void depositDiamonds(Player player, int amount) {
        int remaining = amount;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, Items.DIAMOND.getDefaultMaxStackSize());
            ItemStack diamonds = new ItemStack(Items.DIAMOND, stackSize);

            if (!player.getInventory().add(diamonds)) {
                player.drop(diamonds, false);
            }

            remaining -= stackSize;
        }
    }

    public static String format(int amount) {
        return ChestShopAPI.formatCurrency(amount);
    }

    public static String getCurrencyName() {
        return ChestShopAPI.getCurrencyName();
    }

    public static String getCurrencyNamePlural() {
        return ChestShopAPI.getCurrencyNamePlural();
    }
}
