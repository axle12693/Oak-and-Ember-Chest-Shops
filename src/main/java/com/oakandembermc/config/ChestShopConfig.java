package com.oakandembermc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oakandembermc.ChestShopMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Configuration class for the ChestShop mod.
 * Handles loading, saving, and accessing config options.
 */
public class ChestShopConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ChestShopConfig INSTANCE;
    private static Path configPath;

    // ========== Config Options ==========

    /**
     * Enables or disables the entire chest shop system.
     * When false, chest shops cannot be created or used.
     */
    private boolean chestShopsEnabled = true;

    /**
     * Maximum number of shops a single player can own.
     * Set to -1 for unlimited.
     */
    private int maxShopsPerPlayer = -1;

    /**
     * Cost in diamonds to create a new shop.
     * Set to 0 for free shop creation.
     */
    private int shopCreationCost = 0;

    /**
     * Percentage tax on transactions (0-100).
     * Tax is taken from the seller's proceeds.
     */
    private int transactionTax = 0;

    // ========== Getters ==========

    public boolean isChestShopsEnabled() {
        return chestShopsEnabled;
    }

    public int getMaxShopsPerPlayer() {
        return maxShopsPerPlayer;
    }

    public int getShopCreationCost() {
        return shopCreationCost;
    }

    public int getTransactionTax() {
        return transactionTax;
    }

    // ========== Setters (for runtime changes if needed) ==========

    public void setChestShopsEnabled(boolean enabled) {
        this.chestShopsEnabled = enabled;
        save();
    }

    public void setMaxShopsPerPlayer(int max) {
        this.maxShopsPerPlayer = max;
        save();
    }

    public void setShopCreationCost(int cost) {
        this.shopCreationCost = Math.max(0, cost);
        save();
    }

    public void setTransactionTax(int tax) {
        this.transactionTax = Math.max(0, Math.min(100, tax));
        save();
    }

    // ========== Static Access ==========

    /**
     * Get the config instance.
     * Must call initialize() first.
     */
    public static ChestShopConfig get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ChestShopConfig not initialized! Call initialize() first.");
        }
        return INSTANCE;
    }

    /**
     * Initialize the config system.
     * Should be called early in mod initialization.
     *
     * @param configDir The config directory (usually "config")
     */
    public static void initialize(File configDir) {
        configPath = configDir.toPath().resolve("chestshop_config.json");
        INSTANCE = load();
    }

    /**
     * Load config from file, or create default if it doesn't exist.
     * If no config file exists, copies the default from bundled resources.
     */
    private static ChestShopConfig load() {
        if (!Files.exists(configPath)) {
            // Copy default config from bundled resources
            try (InputStream defaultConfig = ChestShopConfig.class.getResourceAsStream("/data/" + ChestShopMod.MOD_ID + "/storagefolder/chestshop_config.json")) {
                if (defaultConfig != null) {
                    Files.createDirectories(configPath.getParent());
                    Files.copy(defaultConfig, configPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("ChestShop mod: Created default config from bundled resources at " + configPath);
                } else {
                    System.err.println("ChestShop mod: Default chestshop_config.json not found in mod resources! Creating with defaults.");
                    ChestShopConfig config = new ChestShopConfig();
                    config.save();
                    return config;
                }
            } catch (IOException e) {
                System.err.println("ChestShop mod: Failed to copy default config: " + e.getMessage());
                e.printStackTrace();
                return new ChestShopConfig();
            }
        }

        // Load from existing file
        try (Reader reader = Files.newBufferedReader(configPath)) {
            ChestShopConfig config = GSON.fromJson(reader, ChestShopConfig.class);
            if (config != null) {
                System.out.println("ChestShop mod: Config loaded from " + configPath);
                return config;
            }
        } catch (IOException e) {
            System.err.println("ChestShop mod: Failed to load config, using defaults: " + e.getMessage());
        }

        return new ChestShopConfig();
    }

    /**
     * Save the current config to file.
     */
    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("ChestShop mod: Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Reload config from file.
     */
    public static void reload() {
        INSTANCE = load();
    }
}
