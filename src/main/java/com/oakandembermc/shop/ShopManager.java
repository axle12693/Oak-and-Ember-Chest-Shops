package com.oakandembermc.shop;

import com.google.gson.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all chest shops in the server.
 * Handles persistence, lookup, creation, and deletion of shops.
 */
public class ShopManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ShopManager INSTANCE;
    
    private final Path storageDir;
    private final Map<UUID, ChestShop> shopsById = new ConcurrentHashMap<>();
    // Index for fast lookup by position: "dimension:x,y,z" -> shopId
    private final Map<String, UUID> shopsByLocation = new ConcurrentHashMap<>();
    
    private ShopManager(Path storageDir) {
        this.storageDir = storageDir;
    }
    
    // ========== Initialization ==========
    
    /**
     * Initialize the shop manager.
     * @param storageDir Directory to store shop data
     */
    public static void initialize(Path storageDir) {
        if (INSTANCE != null) {
            throw new IllegalStateException("ShopManager already initialized!");
        }
        INSTANCE = new ShopManager(storageDir);
        INSTANCE.loadAllShops();
    }
    
    /**
     * Get the singleton instance.
     */
    public static ShopManager get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ShopManager not initialized! Call initialize() first.");
        }
        return INSTANCE;
    }
    
    // ========== Shop CRUD Operations ==========
    
    /**
     * Creates a new shop at the given location.
     * 
     * @param ownerUuid UUID of the shop owner
     * @param ownerName Display name of the owner
     * @param position Block position of the chest
     * @param world The world the chest is in
     * @return The created shop, or null if a shop already exists at that location
     */
    public ChestShop createShop(UUID ownerUuid, String ownerName, BlockPos position, World world) {
        String locationKey = makeLocationKey(position, world);
        
        // Check if shop already exists at this location
        if (shopsByLocation.containsKey(locationKey)) {
            return null;
        }
        
        String dimensionId = world.getRegistryKey().getValue().toString();
        ChestShop shop = new ChestShop(ownerUuid, ownerName, position, dimensionId);
        
        shopsById.put(shop.getShopId(), shop);
        shopsByLocation.put(locationKey, shop.getShopId());
        
        saveShop(shop);
        
        return shop;
    }
    
    /**
     * Gets a shop by its ID.
     */
    public ChestShop getShop(UUID shopId) {
        return shopsById.get(shopId);
    }
    
    /**
     * Gets a shop at the given location.
     */
    public ChestShop getShopAt(BlockPos position, World world) {
        String locationKey = makeLocationKey(position, world);
        UUID shopId = shopsByLocation.get(locationKey);
        return shopId != null ? shopsById.get(shopId) : null;
    }
    
    /**
     * Gets a shop at the given location (by dimension string).
     */
    public ChestShop getShopAt(BlockPos position, String dimensionId) {
        String locationKey = makeLocationKey(position, dimensionId);
        UUID shopId = shopsByLocation.get(locationKey);
        return shopId != null ? shopsById.get(shopId) : null;
    }
    
    /**
     * Gets all shops owned by a player.
     */
    public List<ChestShop> getShopsByOwner(UUID ownerUuid) {
        return shopsById.values().stream()
                .filter(shop -> shop.isOwner(ownerUuid))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all shops.
     */
    public Collection<ChestShop> getAllShops() {
        return Collections.unmodifiableCollection(shopsById.values());
    }
    
    /**
     * Deletes a shop.
     * 
     * @param shopId The shop's UUID
     * @return true if the shop was deleted, false if it didn't exist
     */
    public boolean deleteShop(UUID shopId) {
        ChestShop shop = shopsById.remove(shopId);
        if (shop == null) {
            return false;
        }
        
        String locationKey = makeLocationKey(shop.getPosition(), shop.getDimensionId());
        shopsByLocation.remove(locationKey);
        
        // Delete the save file
        Path shopFile = storageDir.resolve("shops").resolve(shopId.toString() + ".json");
        try {
            Files.deleteIfExists(shopFile);
        } catch (IOException e) {
            System.err.println("ChestShop: Failed to delete shop file: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Updates a shop and saves it.
     */
    public void updateShop(ChestShop shop) {
        saveShop(shop);
    }
    
    // ========== Persistence ==========
    
    private void loadAllShops() {
        Path shopsDir = storageDir.resolve("shops");
        if (!Files.exists(shopsDir)) {
            try {
                Files.createDirectories(shopsDir);
            } catch (IOException e) {
                System.err.println("ChestShop: Failed to create shops directory: " + e.getMessage());
            }
            return;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(shopsDir, "*.json")) {
            for (Path file : stream) {
                try {
                    ChestShop shop = loadShopFromFile(file);
                    if (shop != null) {
                        shopsById.put(shop.getShopId(), shop);
                        String locationKey = makeLocationKey(shop.getPosition(), shop.getDimensionId());
                        shopsByLocation.put(locationKey, shop.getShopId());
                    }
                } catch (Exception e) {
                    System.err.println("ChestShop: Failed to load shop from " + file + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("ChestShop: Failed to read shops directory: " + e.getMessage());
        }
        
        System.out.println("ChestShop: Loaded " + shopsById.size() + " shops");
    }
    
    private ChestShop loadShopFromFile(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return deserializeShop(json);
        }
    }
    
    private void saveShop(ChestShop shop) {
        Path shopsDir = storageDir.resolve("shops");
        try {
            Files.createDirectories(shopsDir);
            Path shopFile = shopsDir.resolve(shop.getShopId().toString() + ".json");
            try (Writer writer = Files.newBufferedWriter(shopFile)) {
                GSON.toJson(serializeShop(shop), writer);
            }
        } catch (IOException e) {
            System.err.println("ChestShop: Failed to save shop " + shop.getShopId() + ": " + e.getMessage());
        }
    }
    
    // ========== JSON Serialization ==========
    
    private JsonObject serializeShop(ChestShop shop) {
        JsonObject json = new JsonObject();
        json.addProperty("shopId", shop.getShopId().toString());
        json.addProperty("ownerUuid", shop.getOwnerUuid().toString());
        json.addProperty("ownerName", shop.getOwnerName());
        json.addProperty("dimensionId", shop.getDimensionId());
        json.addProperty("active", shop.isActive());
        
        // Position
        JsonObject pos = new JsonObject();
        pos.addProperty("x", shop.getPosition().getX());
        pos.addProperty("y", shop.getPosition().getY());
        pos.addProperty("z", shop.getPosition().getZ());
        json.add("position", pos);
        
        // Entries
        JsonArray entriesArray = new JsonArray();
        for (ShopEntry entry : shop.getEntries()) {
            JsonObject entryJson = new JsonObject();
            entryJson.addProperty("itemId", entry.getItemIdString());
            entryJson.addProperty("pricePerUnit", entry.getPricePerUnit());
            entryJson.addProperty("quantity", entry.getQuantityPerTransaction());
            entryJson.addProperty("mode", entry.getMode().name());
            entriesArray.add(entryJson);
        }
        json.add("entries", entriesArray);
        
        return json;
    }
    
    private ChestShop deserializeShop(JsonObject json) {
        UUID shopId = UUID.fromString(json.get("shopId").getAsString());
        UUID ownerUuid = UUID.fromString(json.get("ownerUuid").getAsString());
        String ownerName = json.get("ownerName").getAsString();
        String dimensionId = json.get("dimensionId").getAsString();
        boolean active = json.get("active").getAsBoolean();
        
        JsonObject pos = json.getAsJsonObject("position");
        BlockPos position = new BlockPos(
                pos.get("x").getAsInt(),
                pos.get("y").getAsInt(),
                pos.get("z").getAsInt()
        );
        
        List<ShopEntry> entries = new ArrayList<>();
        JsonArray entriesArray = json.getAsJsonArray("entries");
        for (JsonElement elem : entriesArray) {
            JsonObject entryJson = elem.getAsJsonObject();
            ShopEntry entry = ShopEntry.fromIdString(
                    entryJson.get("itemId").getAsString(),
                    entryJson.get("pricePerUnit").getAsInt(),
                    entryJson.get("quantity").getAsInt(),
                    ShopMode.valueOf(entryJson.get("mode").getAsString())
            );
            entries.add(entry);
        }
        
        return new ChestShop(shopId, ownerUuid, ownerName, position, dimensionId, entries, active);
    }
    
    // ========== Utility ==========
    
    private String makeLocationKey(BlockPos pos, World world) {
        return makeLocationKey(pos, world.getRegistryKey().getValue().toString());
    }
    
    private String makeLocationKey(BlockPos pos, String dimensionId) {
        return dimensionId + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
