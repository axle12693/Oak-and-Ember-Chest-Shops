package com.oakandembermc.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChestShop {

    private final UUID shopId;
    private final UUID ownerUuid;
    private String ownerName;
    private final BlockPos position;
    private final String dimensionId;
    private final List<ShopEntry> entries;
    private boolean active;

    public ChestShop(UUID ownerUuid, String ownerName, BlockPos position, String dimensionId) {
        this.shopId = UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.position = position;
        this.dimensionId = dimensionId;
        this.entries = new ArrayList<>();
        this.active = true;
    }

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

    public UUID getShopId() { return shopId; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public BlockPos getPosition() { return position; }
    public String getDimensionId() { return dimensionId; }
    public List<ShopEntry> getEntries() { return entries; }
    public boolean isActive() { return active; }

    public boolean isAt(BlockPos pos, String dimension) {
        return position.equals(pos) && dimensionId.equals(dimension);
    }

    public boolean isAt(BlockPos pos, Level world) {
        return isAt(pos, world.dimension().identifier().toString());
    }

    public boolean isOwner(UUID playerUuid) {
        return ownerUuid.equals(playerUuid);
    }

    public void setOwnerName(String name) { this.ownerName = name; }
    public void setActive(boolean active) { this.active = active; }

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
