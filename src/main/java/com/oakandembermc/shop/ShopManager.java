package com.oakandembermc.shop;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ShopManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ShopManager INSTANCE;

    private final Path storageDir;
    private final Map<UUID, ChestShop> shopsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> shopsByLocation = new ConcurrentHashMap<>();

    private ShopManager(Path storageDir) {
        this.storageDir = storageDir;
    }

    public static void initialize(Path storageDir) {
        if (INSTANCE != null) {
            throw new IllegalStateException("ShopManager already initialized!");
        }
        INSTANCE = new ShopManager(storageDir);
        INSTANCE.loadAllShops();
    }

    public static ShopManager get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ShopManager not initialized! Call initialize() first.");
        }
        return INSTANCE;
    }

    public ChestShop createShop(UUID ownerUuid, String ownerName, BlockPos position, Level world) {
        String locationKey = makeLocationKey(position, world);

        if (shopsByLocation.containsKey(locationKey)) {
            return null;
        }

        String dimensionId = world.dimension().identifier().toString();
        ChestShop shop = new ChestShop(ownerUuid, ownerName, position, dimensionId);

        shopsById.put(shop.getShopId(), shop);
        shopsByLocation.put(locationKey, shop.getShopId());

        saveShop(shop);

        return shop;
    }

    public ChestShop getShop(UUID shopId) {
        return shopsById.get(shopId);
    }

    public ChestShop getShopAt(BlockPos position, Level world) {
        String locationKey = makeLocationKey(position, world);
        UUID shopId = shopsByLocation.get(locationKey);
        return shopId != null ? shopsById.get(shopId) : null;
    }

    public ChestShop getShopAt(BlockPos position, String dimensionId) {
        String locationKey = makeLocationKey(position, dimensionId);
        UUID shopId = shopsByLocation.get(locationKey);
        return shopId != null ? shopsById.get(shopId) : null;
    }

    public List<ChestShop> getShopsByOwner(UUID ownerUuid) {
        return shopsById.values().stream()
                .filter(shop -> shop.isOwner(ownerUuid))
                .collect(Collectors.toList());
    }

    public Collection<ChestShop> getAllShops() {
        return Collections.unmodifiableCollection(shopsById.values());
    }

    public boolean deleteShop(UUID shopId) {
        ChestShop shop = shopsById.remove(shopId);
        if (shop == null) {
            return false;
        }

        String locationKey = makeLocationKey(shop.getPosition(), shop.getDimensionId());
        shopsByLocation.remove(locationKey);

        Path shopFile = storageDir.resolve("shops").resolve(shopId.toString() + ".json");
        try {
            Files.deleteIfExists(shopFile);
        } catch (IOException e) {
            System.err.println("ChestShop: Failed to delete shop file: " + e.getMessage());
        }

        return true;
    }

    public void updateShop(ChestShop shop) {
        saveShop(shop);
    }

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

    private JsonObject serializeShop(ChestShop shop) {
        JsonObject json = new JsonObject();
        json.addProperty("shopId", shop.getShopId().toString());
        json.addProperty("ownerUuid", shop.getOwnerUuid().toString());
        json.addProperty("ownerName", shop.getOwnerName());
        json.addProperty("dimensionId", shop.getDimensionId());
        json.addProperty("active", shop.isActive());

        JsonObject pos = new JsonObject();
        pos.addProperty("x", shop.getPosition().getX());
        pos.addProperty("y", shop.getPosition().getY());
        pos.addProperty("z", shop.getPosition().getZ());
        json.add("position", pos);

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
                pos.get("z").getAsInt());

        List<ShopEntry> entries = new ArrayList<>();
        JsonArray entriesArray = json.getAsJsonArray("entries");
        for (JsonElement elem : entriesArray) {
            JsonObject entryJson = elem.getAsJsonObject();
            ShopEntry entry = ShopEntry.fromIdString(
                    entryJson.get("itemId").getAsString(),
                    entryJson.get("pricePerUnit").getAsInt(),
                    entryJson.get("quantity").getAsInt(),
                    ShopMode.valueOf(entryJson.get("mode").getAsString()));
            entries.add(entry);
        }

        return new ChestShop(shopId, ownerUuid, ownerName, position, dimensionId, entries, active);
    }

    private String makeLocationKey(BlockPos pos, Level world) {
        return makeLocationKey(pos, world.dimension().identifier().toString());
    }

    private String makeLocationKey(BlockPos pos, String dimensionId) {
        return dimensionId + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
