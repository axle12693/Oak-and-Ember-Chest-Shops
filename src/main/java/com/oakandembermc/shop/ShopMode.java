package com.oakandembermc.shop;

/**
 * Defines the transaction mode for a shop entry.
 */
public enum ShopMode {
    /**
     * Players can buy this item from the shop (shop sells to players).
     * Requires the shop chest to have stock.
     */
    SELL,

    /**
     * Players can sell this item to the shop (shop buys from players).
     * Requires the shop chest to have space and owner to have currency.
     */
    BUY,

    /**
     * Players can both buy from and sell to the shop.
     */
    BOTH
}
