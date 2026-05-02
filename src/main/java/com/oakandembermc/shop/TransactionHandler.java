package com.oakandembermc.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TransactionHandler {

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

    public static TransactionResult executeBuy(ServerPlayer player, ChestShop shop,
            ShopEntry entry, Container chestInventory) {
        if (!shop.isActive()) {
            return TransactionResult.SHOP_INACTIVE;
        }

        if (!entry.canPlayerBuy()) {
            return TransactionResult.INVALID_SHOP;
        }

        int price = entry.getTotalPrice();
        int quantity = entry.getQuantityPerTransaction();
        Item item = entry.getItem();

        if (!CurrencyHandler.hasBalance(player, price)) {
            return TransactionResult.BUYER_INSUFFICIENT_FUNDS;
        }

        int stockCount = countItemInInventory(chestInventory, item);
        if (stockCount < quantity) {
            return TransactionResult.SHOP_OUT_OF_STOCK;
        }

        if (!canReceiveItems(player, item, quantity)) {
            return TransactionResult.BUYER_INVENTORY_FULL;
        }

        CurrencyHandler.withdraw(player, price);
        removeItemsFromInventory(chestInventory, item, quantity);
        giveItemsToPlayer(player, item, quantity);
        addItemsToInventory(chestInventory, new ItemStack(Items.DIAMOND, price));

        return TransactionResult.SUCCESS;
    }

    public static TransactionResult executeSell(ServerPlayer player, ChestShop shop,
            ShopEntry entry, Container chestInventory) {
        if (!shop.isActive()) {
            return TransactionResult.SHOP_INACTIVE;
        }

        if (!entry.canPlayerSell()) {
            return TransactionResult.INVALID_SHOP;
        }

        int price = entry.getTotalPrice();
        int quantity = entry.getQuantityPerTransaction();
        Item item = entry.getItem();

        int sellerStock = countItemInInventory(player.getInventory(), item);
        if (sellerStock < quantity) {
            return TransactionResult.SELLER_INSUFFICIENT_ITEMS;
        }

        int shopCurrency = countItemInInventory(chestInventory, Items.DIAMOND);
        if (shopCurrency < price) {
            return TransactionResult.OWNER_INSUFFICIENT_FUNDS;
        }

        if (!canInventoryReceive(chestInventory, item, quantity)) {
            return TransactionResult.SHOP_FULL;
        }

        removeItemsFromInventory(player.getInventory(), item, quantity);
        addItemsToInventory(chestInventory, new ItemStack(item, quantity));
        removeItemsFromInventory(chestInventory, Items.DIAMOND, price);
        CurrencyHandler.deposit(player, price);

        return TransactionResult.SUCCESS;
    }

    public static void sendResultMessage(ServerPlayer player, TransactionResult result, ShopEntry entry) {
        Component message;
        switch (result) {
            case SUCCESS:
                message = Component.literal("Transaction successful!").withStyle(ChatFormatting.GREEN);
                break;
            case SHOP_OUT_OF_STOCK:
                message = Component.literal("Shop is out of stock!").withStyle(ChatFormatting.RED);
                break;
            case SHOP_FULL:
                message = Component.literal("Shop chest is full!").withStyle(ChatFormatting.RED);
                break;
            case BUYER_INSUFFICIENT_FUNDS:
                message = Component.literal("You don't have enough " + CurrencyHandler.getCurrencyNamePlural() + "!")
                        .withStyle(ChatFormatting.RED);
                break;
            case BUYER_INVENTORY_FULL:
                message = Component.literal("Your inventory is full!").withStyle(ChatFormatting.RED);
                break;
            case SELLER_INSUFFICIENT_ITEMS:
                message = Component.literal("You don't have enough items to sell!").withStyle(ChatFormatting.RED);
                break;
            case SHOP_INACTIVE:
                message = Component.literal("This shop is currently inactive.").withStyle(ChatFormatting.YELLOW);
                break;
            case OWNER_INSUFFICIENT_FUNDS:
                message = Component
                        .literal("Shop doesn't have enough " + CurrencyHandler.getCurrencyNamePlural() + " to buy!")
                        .withStyle(ChatFormatting.RED);
                break;
            default:
                message = Component.literal("Transaction failed.").withStyle(ChatFormatting.RED);
        }
        player.sendSystemMessage(message);
    }

    private static int countItemInInventory(Container inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItemsFromInventory(Container inventory, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        inventory.setChanged();
    }

    private static void addItemsToInventory(Container inventory, ItemStack toAdd) {
        int remaining = toAdd.getCount();
        Item item = toAdd.getItem();

        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item) && stack.getCount() < stack.getMaxStackSize()) {
                int canAdd = Math.min(remaining, stack.getMaxStackSize() - stack.getCount());
                stack.grow(canAdd);
                remaining -= canAdd;
            }
        }

        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                int toPlace = Math.min(remaining, item.getDefaultMaxStackSize());
                inventory.setItem(i, new ItemStack(item, toPlace));
                remaining -= toPlace;
            }
        }

        inventory.setChanged();
    }

    private static boolean canReceiveItems(Player player, Item item, int amount) {
        int emptySlots = 0;
        int partialSpace = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                emptySlots++;
            } else if (stack.is(item) && stack.getCount() < stack.getMaxStackSize()) {
                partialSpace += stack.getMaxStackSize() - stack.getCount();
            }
        }

        int totalSpace = (emptySlots * item.getDefaultMaxStackSize()) + partialSpace;
        return totalSpace >= amount;
    }

    private static void giveItemsToPlayer(ServerPlayer player, Item item, int amount) {
        int remaining = amount;

        while (remaining > 0) {
            int stackSize = Math.min(remaining, item.getDefaultMaxStackSize());
            ItemStack stack = new ItemStack(item, stackSize);

            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }

            remaining -= stackSize;
        }
    }

    private static boolean canInventoryReceive(Container inventory, Item item, int amount) {
        int totalSpace = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                totalSpace += item.getDefaultMaxStackSize();
            } else if (stack.is(item) && stack.getCount() < stack.getMaxStackSize()) {
                totalSpace += stack.getMaxStackSize() - stack.getCount();
            }
        }

        return totalSpace >= amount;
    }
}
