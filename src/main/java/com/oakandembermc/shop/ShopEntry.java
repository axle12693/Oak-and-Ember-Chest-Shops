package com.oakandembermc.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class ShopEntry {

    private final Identifier itemId;
    private int pricePerUnit;
    private int quantityPerTransaction;
    private ShopMode mode;

    public ShopEntry(Identifier itemId, int pricePerUnit, int quantityPerTransaction, ShopMode mode) {
        this.itemId = itemId;
        this.pricePerUnit = pricePerUnit;
        this.quantityPerTransaction = quantityPerTransaction;
        this.mode = mode;
    }

    public ShopEntry(Item item, int pricePerUnit, int quantityPerTransaction, ShopMode mode) {
        this(BuiltInRegistries.ITEM.getKey(item), pricePerUnit, quantityPerTransaction, mode);
    }

    public Identifier getItemId() {
        return itemId;
    }

    public Item getItem() {
        return BuiltInRegistries.ITEM.getValue(itemId);
    }

    public int getPricePerUnit() {
        return pricePerUnit;
    }

    public int getQuantityPerTransaction() {
        return quantityPerTransaction;
    }

    public int getTotalPrice() {
        return pricePerUnit * quantityPerTransaction;
    }

    public ShopMode getMode() {
        return mode;
    }

    public boolean canPlayerBuy() {
        return mode == ShopMode.SELL || mode == ShopMode.BOTH;
    }

    public boolean canPlayerSell() {
        return mode == ShopMode.BUY || mode == ShopMode.BOTH;
    }

    public void setPricePerUnit(int price) {
        this.pricePerUnit = Math.max(0, price);
    }

    public void setQuantityPerTransaction(int quantity) {
        this.quantityPerTransaction = Math.max(1, quantity);
    }

    public void setMode(ShopMode mode) {
        this.mode = mode;
    }

    public String getItemIdString() {
        return itemId.toString();
    }

    public static ShopEntry fromIdString(String itemIdStr, int price, int quantity, ShopMode mode) {
        return new ShopEntry(Identifier.parse(itemIdStr), price, quantity, mode);
    }
}
