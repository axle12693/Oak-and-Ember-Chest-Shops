package com.oakandembermc.shop;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a chest shop in the world.
 * A shop is tied to a specific chest location and owned by a player.
 */
public class ChestShop {
    
    private final UUID shopId;
    private final UUID ownerUuid;
    private String ownerName;  // Cached for display, may be stale
    private final BlockPos position;
    private final String dimensionId;  // e.g., "minecraft:overworld"
    private final List<ShopEntry> entries;
    private boolean active;
    
    /**
     * Creates a new chest shop.
     * 
     * @param ownerUuid UUID of the player who owns this shop
     * @param ownerName Display name of the owner (cached)
     * @param position Block position of the chest
     * @param dimensionId Registry key string of the dimension
     */
    public ChestShop(UUID ownerUuid, String ownerName, BlockPos position, String dimensionId) {
        this.shopId = UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.position = position;
        this.dimensionId = dimensionId;
        this.entries = new ArrayList<>();
        this.active = true;
    }
    
    /**
     * Creates a chest shop with an existing ID (for deserialization).
     */
    public ChestShop(UUID shopId, UUID ownerUuid, String ownerName, BlockPos position, 
                     String dimensionId, List<ShopEntry> entries, boolean active) {
        this.shopId = shopId;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.position = position;
        this.dimensionId = dimensionId;
        this.entries = new ArrayList<>(entries);
        this.active = active;
    }
    
    // ========== Getters ==========
    
    public UUID getShopId() {
        return shopId;
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public String getDimensionId() {
        return dimensionId;
    }
    
    public List<ShopEntry> getEntries() {
        return entries;
    }
    
    public boolean isActive() {
        return active;
    }
    
    /**
     * Checks if this shop is at the given location.
     */
    public boolean isAt(BlockPos pos, String dimension) {
        return position.equals(pos) && dimensionId.equals(dimension);
    }
    
    /**
     * Checks if this shop is at the given location in the given world.
     */
    public boolean isAt(BlockPos pos, World world) {
        return isAt(pos, world.getRegistryKey().getValue().toString());
    }
    
    /**
     * Checks if the given player owns this shop.
     */
    public boolean isOwner(UUID playerUuid) {
        return ownerUuid.equals(playerUuid);
    }
    
    // ========== Setters/Mutators ==========
    
    public void setOwnerName(String name) {
        this.ownerName = name;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void setEntry(ShopEntry entry) {
        entries.clear();
        if (entry != null) {
            entries.add(entry);
        }
    }
    
    public ShopEntry getEntry() {
        return entries.isEmpty() ? null : entries.get(0);
    }
    
    public boolean hasEntry() {
        return !entries.isEmpty();
    }
    
    /**
     * @deprecated Use setEntry() instead - shops now only support one item
     */
    @Deprecated
    public void addEntry(ShopEntry entry) {
        setEntry(entry);
    }
    
    public boolean removeEntry(ShopEntry entry) {
        return entries.remove(entry);
    }
    
    public void clearEntries() {
        entries.clear();
    }
    
    // ========== Utility ==========
    
    /**
     * Gets a string representation of the position for display.
     */
    public String getPositionString() {
        return String.format("(%d, %d, %d)", position.getX(), position.getY(), position.getZ());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChestShop other = (ChestShop) obj;
        return shopId.equals(other.shopId);
    }
    
    @Override
    public int hashCode() {
        return shopId.hashCode();
    }
}
