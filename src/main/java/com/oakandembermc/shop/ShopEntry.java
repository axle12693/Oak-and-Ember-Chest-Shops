package com.oakandembermc.shop;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Represents a single item listing within a chest shop.
 * Each entry defines what item is being traded, at what price, and in what mode.
 */
public class ShopEntry {
    
    private final Identifier itemId;
    private int pricePerUnit;      // Price in diamonds per unit
    private int quantityPerTransaction;  // How many items per transaction
    private ShopMode mode;
    
    /**
     * Creates a new shop entry.
     * 
     * @param itemId The registry identifier of the item (e.g., "minecraft:diamond_sword")
     * @param pricePerUnit Price in diamonds for one unit of this item
     * @param quantityPerTransaction Number of items traded per transaction
     * @param mode Whether players can buy, sell, or both
     */
    public ShopEntry(Identifier itemId, int pricePerUnit, int quantityPerTransaction, ShopMode mode) {
        this.itemId = itemId;
        this.pricePerUnit = pricePerUnit;
        this.quantityPerTransaction = quantityPerTransaction;
        this.mode = mode;
    }
    
    /**
     * Creates a shop entry from an Item instance.
     */
    public ShopEntry(Item item, int pricePerUnit, int quantityPerTransaction, ShopMode mode) {
        this(Registries.ITEM.getId(item), pricePerUnit, quantityPerTransaction, mode);
    }
    
    // ========== Getters ==========
    
    public Identifier getItemId() {
        return itemId;
    }
    
    /**
     * Gets the Item instance for this entry.
     * @return The Item, or null if the item ID is not registered (e.g., mod removed)
     */
    public Item getItem() {
        return Registries.ITEM.get(itemId);
    }
    
    public int getPricePerUnit() {
        return pricePerUnit;
    }
    
    public int getQuantityPerTransaction() {
        return quantityPerTransaction;
    }
    
    /**
     * Gets the total price for one transaction.
     */
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
    
    // ========== Setters ==========
    
    public void setPricePerUnit(int price) {
        this.pricePerUnit = Math.max(0, price);
    }
    
    public void setQuantityPerTransaction(int quantity) {
        this.quantityPerTransaction = Math.max(1, quantity);
    }
    
    public void setMode(ShopMode mode) {
        this.mode = mode;
    }
    
    // ========== Serialization helpers (for JSON) ==========
    
    public String getItemIdString() {
        return itemId.toString();
    }
    
    public static ShopEntry fromIdString(String itemIdStr, int price, int quantity, ShopMode mode) {
        return new ShopEntry(Identifier.of(itemIdStr), price, quantity, mode);
    }
}
