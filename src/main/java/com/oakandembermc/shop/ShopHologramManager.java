package com.oakandembermc.shop;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.BillboardMode;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages floating text displays above shop chests using Text Display entities.
 */
public class ShopHologramManager {
    
    // Scoreboard tag used to identify our holograms reliably
    private static final String HOLOGRAM_TAG = "chestshop_hologram";
    
    // Track holograms by shop ID
    private static final Map<UUID, UUID> shopToHologram = new ConcurrentHashMap<>();
    
    /**
     * Creates or updates the hologram for a shop.
     */
    public static void updateHologram(ChestShop shop, ServerWorld world) {
        // Remove existing hologram if any
        removeHologram(shop, world);
        
        ShopEntry entry = shop.getEntry();
        if (entry == null || !shop.isActive()) {
            return; // No hologram needed for unconfigured/inactive shops
        }
        
        BlockPos pos = shop.getPosition();
        
        // Build display text based on mode
        String itemName = getSimpleItemName(entry.getItemIdString());
        String modeIndicator;
        Formatting modeColor;
        switch (entry.getMode()) {
            case SELL:
                modeIndicator = "SELLING";
                modeColor = Formatting.GREEN;
                break;
            case BUY:
                modeIndicator = "BUYING";
                modeColor = Formatting.YELLOW;
                break;
            case BOTH:
                modeIndicator = "BUY/SELL";
                modeColor = Formatting.AQUA;
                break;
            default:
                modeIndicator = "SHOP";
                modeColor = Formatting.WHITE;
        }
        
        // Format: [SELLING] Diamond Sword - 16x for 5◆
        Text displayText = Text.literal("[" + modeIndicator + "] ").formatted(modeColor)
                .append(Text.literal(itemName).formatted(Formatting.WHITE))
                .append(Text.literal(" - " + entry.getQuantityPerTransaction() + "x for " + entry.getTotalPrice() + "◆").formatted(Formatting.GRAY));
        
        // Create Text Display entity
        TextDisplayEntity hologram = new TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        
        // Position above the chest (centered)
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.3;  // Above the chest
        double z = pos.getZ() + 0.5;
        
        hologram.refreshPositionAndAngles(x, y, z, 0, 0);
        
        // Set display properties via DataTracker (the internal mechanism for synced entity data)
        hologram.setText(displayText);
        hologram.setBillboardMode(BillboardMode.CENTER);  // Always face the player
        hologram.setBackground(0x40000000);  // Semi-transparent black background (ARGB)
        hologram.setLineWidth(200);
        
        // Add our identification tag for reliable cleanup
        hologram.addCommandTag(HOLOGRAM_TAG);
        // Also add shop-specific tag for targeted removal
        hologram.addCommandTag("shop_" + shop.getShopId().toString());
        
        boolean spawned = world.spawnEntity(hologram);
        
        if (spawned) {
            shopToHologram.put(shop.getShopId(), hologram.getUuid());
        }
    }
    
    /**
     * Removes the hologram for a shop.
     */
    public static void removeHologram(ChestShop shop, ServerWorld world) {
        UUID hologramId = shopToHologram.remove(shop.getShopId());
        if (hologramId != null) {
            var entity = world.getEntity(hologramId);
            if (entity != null) {
                entity.discard();
            }
        }
    }
    
    /**
     * Refreshes all holograms in a world (called on server start/world load).
     */
    public static void refreshAllHolograms(ServerWorld world) {
        String dimensionId = world.getRegistryKey().getValue().toString();
        
        // First, clean up any existing shop holograms in the world
        // This handles holograms that were saved with the world
        cleanupOldHolograms(world);
        
        // Clear our tracking map for this dimension
        shopToHologram.clear();
        
        // Create fresh holograms for all shops
        for (ChestShop shop : ShopManager.get().getAllShops()) {
            if (shop.getDimensionId().equals(dimensionId)) {
                updateHologram(shop, world);
            }
        }
    }
    
    /**
     * Removes any old shop holograms from the world.
     * Uses scoreboard tags for reliable identification.
     */
    private static void cleanupOldHolograms(ServerWorld world) {
        // Find all entities with our hologram tag and remove them
        world.getEntitiesByType(EntityType.TEXT_DISPLAY, entity -> 
            entity.getCommandTags().contains(HOLOGRAM_TAG)
        ).forEach(Entity::discard);
    }
    
    /**
     * Removes all holograms (called on server stop).
     */
    public static void removeAllHolograms(ServerWorld world) {
        String dimensionId = world.getRegistryKey().getValue().toString();
        
        for (ChestShop shop : ShopManager.get().getAllShops()) {
            if (shop.getDimensionId().equals(dimensionId)) {
                removeHologram(shop, world);
            }
        }
        
        shopToHologram.clear();
    }
    
    private static String getSimpleItemName(String itemId) {
        String name = itemId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
