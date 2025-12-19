package com.oakandembermc.shop;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Handles currency operations using diamonds.
 * This is the default implementation that can later be extended or replaced
 * via a connector mod that integrates with EconomyMod.
 */
public class CurrencyHandler {
    
    public static final String CURRENCY_NAME = "Diamond";
    public static final String CURRENCY_NAME_PLURAL = "Diamonds";
    
    /**
     * Gets the player's current balance (diamond count).
     */
    public static int getBalance(PlayerEntity player) {
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
     * Removes diamonds from the player's inventory.
     * 
     * @param player The player
     * @param amount Amount to remove
     * @return true if successful, false if player doesn't have enough
     */
    public static boolean withdraw(PlayerEntity player, int amount) {
        if (!hasBalance(player, amount)) {
            return false;
        }
        
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
     * Adds diamonds to the player's inventory.
     * If inventory is full, remaining diamonds are dropped at the player's feet.
     * 
     * @param player The player
     * @param amount Amount to add
     */
    public static void deposit(PlayerEntity player, int amount) {
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
        if (amount == 1) {
            return "1 " + CURRENCY_NAME;
        }
        return amount + " " + CURRENCY_NAME_PLURAL;
    }
}
