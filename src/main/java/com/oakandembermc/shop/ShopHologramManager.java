package com.oakandembermc.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopHologramManager {
    private ShopHologramManager() {
        /* This utility class should not be instantiated */
    }

    private static final String HOLOGRAM_TAG = "chestshop_hologram";

    private static final Map<UUID, UUID> shopToHologram = new ConcurrentHashMap<>();

    public static void updateHologram(ChestShop shop, ServerLevel world) {
        removeHologram(shop, world);

        ShopEntry entry = shop.getEntry();
        if (entry == null || !shop.isActive()) {
            return;
        }

        BlockPos pos = shop.getPosition();

        String itemName = getSimpleItemName(entry.getItemIdString());
        String modeIndicator;
        ChatFormatting modeColor;
        switch (entry.getMode()) {
            case SELL:
                modeIndicator = "SELLING";
                modeColor = ChatFormatting.GREEN;
                break;
            case BUY:
                modeIndicator = "BUYING";
                modeColor = ChatFormatting.YELLOW;
                break;
            case BOTH:
                modeIndicator = "BUY/SELL";
                modeColor = ChatFormatting.AQUA;
                break;
            default:
                modeIndicator = "SHOP";
                modeColor = ChatFormatting.WHITE;
        }

        Component displayText = Component.literal("[" + modeIndicator + "] ").withStyle(modeColor)
                .append(Component.literal(itemName).withStyle(ChatFormatting.WHITE))
                .append(Component
                        .literal(" - " + entry.getQuantityPerTransaction() + "x for " + entry.getTotalPrice() + "◆")
                        .withStyle(ChatFormatting.GRAY));

        Display.TextDisplay hologram = new Display.TextDisplay(EntityType.TEXT_DISPLAY, world);

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.3;
        double z = pos.getZ() + 0.5;

        // Build NBT to configure the display entity properties via the load path,
        // since Display setters are private in MC 26.1.
        CompoundTag displayNbt = new CompoundTag();

        ComponentSerialization.CODEC
                .encodeStart(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), displayText)
                .result()
                .ifPresent((Tag textNbt) -> displayNbt.put("text", textNbt));

        displayNbt.putString(Display.TAG_BILLBOARD, "center");
        displayNbt.putInt("background", 0x40000000);
        displayNbt.putInt("line_width", 200);

        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), displayNbt);
        hologram.load(input);

        // Re-apply position; load() resets it to 0,0,0 since Pos was absent from NBT.
        hologram.setPos(x, y, z);

        hologram.addTag(HOLOGRAM_TAG);
        hologram.addTag("shop_" + shop.getShopId().toString());

        boolean spawned = world.addFreshEntity(hologram);

        if (spawned) {
            shopToHologram.put(shop.getShopId(), hologram.getUUID());
        }
    }

    public static void removeHologram(ChestShop shop, ServerLevel world) {
        UUID hologramId = shopToHologram.remove(shop.getShopId());
        if (hologramId != null) {
            Entity entity = world.getEntity(hologramId);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    public static void refreshAllHolograms(ServerLevel world) {
        String dimensionId = world.dimension().identifier().toString();

        cleanupOldHolograms(world);

        shopToHologram.clear();

        for (ChestShop shop : ShopManager.get().getAllShops()) {
            if (shop.getDimensionId().equals(dimensionId)) {
                updateHologram(shop, world);
            }
        }
    }

    private static void cleanupOldHolograms(ServerLevel world) {
        world.getEntities(EntityType.TEXT_DISPLAY,
                entity -> entity.entityTags().contains(HOLOGRAM_TAG)).forEach(Entity::discard);
    }

    public static void removeAllHolograms(ServerLevel world) {
        String dimensionId = world.dimension().identifier().toString();

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
