package com.oakandembermc.shop;

import com.oakandembermc.ChestShopAPI;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Handles currency operations.
 * <p>
 * By default, uses diamonds as currency. If a custom currency provider
 * is registered via {@link ChestShopAPI#registerCurrencyProvider}, that
 * provider will be used instead.
 */
public class CurrencyHandler {
    
    /**
     * Gets the player's current balance.
     * Uses custom provider if registered, otherwise counts diamonds.
     */
    public static int getBalance(PlayerEntity player) {
        if (ChestShopAPI.hasCustomCurrencyProvider()) {
            return ChestShopAPI.getCurrencyBalance(player);
        }
        return getDiamondCount(player);
    }
    
    /**
     * Counts diamonds in player inventory (default currency).
     */
    private static int getDiamondCount(PlayerEntity player) {
        int count = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(Items.DIAMOND)) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Checks if the player has at least the specified amount of diamonds.
     */
    public static boolean hasBalance(PlayerEntity player, int amount) {
        return getBalance(player) >= amount;
    }
    
    /**
     * Removes currency from the player.
     * Uses custom provider if registered, otherwise removes diamonds.
     * 
     * @param player The player
     * @param amount Amount to remove
     * @return true if successful, false if player doesn't have enough
     */
    public static boolean withdraw(PlayerEntity player, int amount) {
        if (!hasBalance(player, amount)) {
            return false;
        }
        
        if (ChestShopAPI.hasCustomCurrencyProvider()) {
            ChestShopAPI.addCurrencyBalance(player, -amount);
            return true;
        }
        
        return withdrawDiamonds(player, amount);
    }
    
    /**
     * Removes diamonds from inventory (default currency).
     */
    private static boolean withdrawDiamonds(PlayerEntity player, int amount) {
        int remaining = amount;
        PlayerInventory inv = player.getInventory();
        
        for (int i = 0; i < inv.size() && remaining > 0; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(Items.DIAMOND)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.decrement(toRemove);
                remaining -= toRemove;
                
                if (stack.isEmpty()) {
                    inv.setStack(i, ItemStack.EMPTY);
                }
            }
        }
        
        return remaining == 0;
    }
    
    /**
     * Adds currency to the player.
     * Uses custom provider if registered, otherwise adds diamonds to inventory.
     * 
     * @param player The player
     * @param amount Amount to add
     */
    public static void deposit(PlayerEntity player, int amount) {
        if (ChestShopAPI.hasCustomCurrencyProvider()) {
            ChestShopAPI.addCurrencyBalance(player, amount);
            return;
        }
        
        depositDiamonds(player, amount);
    }
    
    /**
     * Adds diamonds to inventory (default currency).
     * If inventory is full, remaining diamonds are dropped at the player's feet.
     */
    private static void depositDiamonds(PlayerEntity player, int amount) {
        int remaining = amount;
        
        while (remaining > 0) {
            int stackSize = Math.min(remaining, Items.DIAMOND.getMaxCount());
            ItemStack diamonds = new ItemStack(Items.DIAMOND, stackSize);
            
            if (!player.getInventory().insertStack(diamonds)) {
                // Inventory full, drop the rest
                player.dropItem(diamonds, false);
            }
            
            remaining -= stackSize;
        }
    }
    
    /**
     * Formats a currency amount for display.
     */
    public static String format(int amount) {
        return ChestShopAPI.formatCurrency(amount);
    }
    
    /**
     * Gets the singular currency name.
     */
    public static String getCurrencyName() {
        return ChestShopAPI.getCurrencyName();
    }
    
    /**
     * Gets the plural currency name.
     */
    public static String getCurrencyNamePlural() {
        return ChestShopAPI.getCurrencyNamePlural();
    }
}
