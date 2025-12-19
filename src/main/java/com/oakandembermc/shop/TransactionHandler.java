package com.oakandembermc.shop;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Handles chest shop transactions.
 */
public class TransactionHandler {
    
    /**
     * Result of a transaction attempt.
     */
    public enum TransactionResult {
        SUCCESS,
        SHOP_OUT_OF_STOCK,
        SHOP_FULL,
        BUYER_INSUFFICIENT_FUNDS,
        BUYER_INVENTORY_FULL,
        SELLER_INSUFFICIENT_ITEMS,
        SHOP_INACTIVE,
        INVALID_SHOP,
        OWNER_INSUFFICIENT_FUNDS
    }
    
    /**
     * Attempts to execute a BUY transaction (player buys from shop).
     * 
     * @param player The buying player
     * @param shop The chest shop
     * @param entry The shop entry being purchased
     * @param chestInventory The chest's inventory
     * @return The result of the transaction
     */
    public static TransactionResult executeBuy(ServerPlayerEntity player, ChestShop shop, 
                                                ShopEntry entry, Inventory chestInventory) {
        if (!shop.isActive()) {
            return TransactionResult.SHOP_INACTIVE;
        }
        
        if (!entry.canPlayerBuy()) {
            return TransactionResult.INVALID_SHOP;
        }
        
        int price = entry.getTotalPrice();
        int quantity = entry.getQuantityPerTransaction();
        Item item = entry.getItem();
        
        // Check buyer has enough currency
        if (!CurrencyHandler.hasBalance(player, price)) {
            return TransactionResult.BUYER_INSUFFICIENT_FUNDS;
        }
        
        // Check shop has enough stock
        int stockCount = countItemInInventory(chestInventory, item);
        if (stockCount < quantity) {
            return TransactionResult.SHOP_OUT_OF_STOCK;
        }
        
        // Check buyer can receive items (basic check)
        if (!canReceiveItems(player, item, quantity)) {
            return TransactionResult.BUYER_INVENTORY_FULL;
        }
        
        // Execute transaction
        // 1. Remove currency from buyer
        CurrencyHandler.withdraw(player, price);
        
        // 2. Remove items from shop chest
        removeItemsFromInventory(chestInventory, item, quantity);
        
        // 3. Give items to buyer
        giveItemsToPlayer(player, item, quantity);
        
        // 4. Add currency to shop chest (for owner to collect)
        addItemsToInventory(chestInventory, new ItemStack(CurrencyHandler.CURRENCY_NAME.equals("Diamond") ? 
                net.minecraft.item.Items.DIAMOND : net.minecraft.item.Items.DIAMOND, price));
        
        return TransactionResult.SUCCESS;
    }
    
    /**
     * Attempts to execute a SELL transaction (player sells to shop).
     * 
     * @param player The selling player
     * @param shop The chest shop
     * @param entry The shop entry
     * @param chestInventory The chest's inventory
     * @return The result of the transaction
     */
    public static TransactionResult executeSell(ServerPlayerEntity player, ChestShop shop,
                                                 ShopEntry entry, Inventory chestInventory) {
        if (!shop.isActive()) {
            return TransactionResult.SHOP_INACTIVE;
        }
        
        if (!entry.canPlayerSell()) {
            return TransactionResult.INVALID_SHOP;
        }
        
        int price = entry.getTotalPrice();
        int quantity = entry.getQuantityPerTransaction();
        Item item = entry.getItem();
        
        // Check seller has items
        int sellerStock = countItemInInventory(player.getInventory(), item);
        if (sellerStock < quantity) {
            return TransactionResult.SELLER_INSUFFICIENT_ITEMS;
        }
        
        // Check shop has currency to pay (diamonds in chest)
        int shopCurrency = countItemInInventory(chestInventory, net.minecraft.item.Items.DIAMOND);
        if (shopCurrency < price) {
            return TransactionResult.OWNER_INSUFFICIENT_FUNDS;
        }
        
        // Check shop can receive items
        if (!canInventoryReceive(chestInventory, item, quantity)) {
            return TransactionResult.SHOP_FULL;
        }
        
        // Execute transaction
        // 1. Remove items from seller
        removeItemsFromInventory(player.getInventory(), item, quantity);
        
        // 2. Add items to shop chest
        addItemsToInventory(chestInventory, new ItemStack(item, quantity));
        
        // 3. Remove currency from shop chest
        removeItemsFromInventory(chestInventory, net.minecraft.item.Items.DIAMOND, price);
        
        // 4. Give currency to seller
        CurrencyHandler.deposit(player, price);
        
        return TransactionResult.SUCCESS;
    }
    
    /**
     * Sends a transaction result message to the player.
     */
    public static void sendResultMessage(ServerPlayerEntity player, TransactionResult result, ShopEntry entry) {
        Text message;
        switch (result) {
            case SUCCESS:
                message = Text.literal("Transaction successful!").formatted(Formatting.GREEN);
                break;
            case SHOP_OUT_OF_STOCK:
                message = Text.literal("Shop is out of stock!").formatted(Formatting.RED);
                break;
            case SHOP_FULL:
                message = Text.literal("Shop chest is full!").formatted(Formatting.RED);
                break;
            case BUYER_INSUFFICIENT_FUNDS:
                message = Text.literal("You don't have enough " + CurrencyHandler.CURRENCY_NAME_PLURAL + "!")
                        .formatted(Formatting.RED);
                break;
            case BUYER_INVENTORY_FULL:
                message = Text.literal("Your inventory is full!").formatted(Formatting.RED);
                break;
            case SELLER_INSUFFICIENT_ITEMS:
                message = Text.literal("You don't have enough items to sell!").formatted(Formatting.RED);
                break;
            case SHOP_INACTIVE:
                message = Text.literal("This shop is currently inactive.").formatted(Formatting.YELLOW);
                break;
            case OWNER_INSUFFICIENT_FUNDS:
                message = Text.literal("Shop doesn't have enough " + CurrencyHandler.CURRENCY_NAME_PLURAL + " to buy!")
                        .formatted(Formatting.RED);
                break;
            default:
                message = Text.literal("Transaction failed.").formatted(Formatting.RED);
        }
        player.sendMessage(message, false);
    }
    
    // ========== Inventory Utilities ==========
    
    private static int countItemInInventory(Inventory inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    private static void removeItemsFromInventory(Inventory inventory, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.decrement(toRemove);
                remaining -= toRemove;
                if (stack.isEmpty()) {
                    inventory.setStack(i, ItemStack.EMPTY);
                }
            }
        }
        inventory.markDirty();
    }
    
    private static void addItemsToInventory(Inventory inventory, ItemStack toAdd) {
        int remaining = toAdd.getCount();
        Item item = toAdd.getItem();
        
        // First, try to merge with existing stacks
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item) && stack.getCount() < stack.getMaxCount()) {
                int canAdd = Math.min(remaining, stack.getMaxCount() - stack.getCount());
                stack.increment(canAdd);
                remaining -= canAdd;
            }
        }
        
        // Then, fill empty slots
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                int toPlace = Math.min(remaining, item.getMaxCount());
                inventory.setStack(i, new ItemStack(item, toPlace));
                remaining -= toPlace;
            }
        }
        
        inventory.markDirty();
    }
    
    private static boolean canReceiveItems(PlayerEntity player, Item item, int amount) {
        // Simplified check - just see if there's any space at all
        int emptySlots = 0;
        int partialSpace = 0;
        
        // Main inventory is slots 0-35 (hotbar 0-8, main 9-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                emptySlots++;
            } else if (stack.isOf(item) && stack.getCount() < stack.getMaxCount()) {
                partialSpace += stack.getMaxCount() - stack.getCount();
            }
        }
        
        int totalSpace = (emptySlots * item.getMaxCount()) + partialSpace;
        return totalSpace >= amount;
    }
    
    private static void giveItemsToPlayer(ServerPlayerEntity player, Item item, int amount) {
        int remaining = amount;
        
        while (remaining > 0) {
            int stackSize = Math.min(remaining, item.getMaxCount());
            ItemStack stack = new ItemStack(item, stackSize);
            
            if (!player.getInventory().insertStack(stack)) {
                // Inventory full, drop the rest
                player.dropItem(stack, false);
            }
            
            remaining -= stackSize;
        }
    }
    
    private static boolean canInventoryReceive(Inventory inventory, Item item, int amount) {
        int totalSpace = 0;
        
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                totalSpace += item.getMaxCount();
            } else if (stack.isOf(item) && stack.getCount() < stack.getMaxCount()) {
                totalSpace += stack.getMaxCount() - stack.getCount();
            }
        }
        
        return totalSpace >= amount;
    }
}
