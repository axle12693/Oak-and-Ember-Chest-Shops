package com.oakandembermc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.oakandembermc.shop.ChestShop;
import com.oakandembermc.shop.ShopEntry;
import com.oakandembermc.shop.ShopHologramManager;
import com.oakandembermc.shop.ShopManager;
import com.oakandembermc.shop.ShopMode;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Registers and handles chest shop commands.
 */
public class ChestShopCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, 
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        
        dispatcher.register(CommandManager.literal("chestshop")
                .then(CommandManager.literal("create")
                        .executes(ChestShopCommand::createShop))
                .then(CommandManager.literal("delete")
                        .executes(ChestShopCommand::deleteShop))
                .then(CommandManager.literal("info")
                        .executes(ChestShopCommand::shopInfo))
                .then(CommandManager.literal("list")
                        .executes(ChestShopCommand::listShops))
                .then(CommandManager.literal("setitem")
                        .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("quantity", IntegerArgumentType.integer(1, 64))
                                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("sell");
                                                    builder.suggest("buy");
                                                    builder.suggest("both");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ChestShopCommand::setItem)))))
                .then(CommandManager.literal("clearitem")
                        .executes(ChestShopCommand::clearItem))
        );
        
        // Shorter alias
        dispatcher.register(CommandManager.literal("cs")
                .then(CommandManager.literal("create")
                        .executes(ChestShopCommand::createShop))
                .then(CommandManager.literal("delete")
                        .executes(ChestShopCommand::deleteShop))
                .then(CommandManager.literal("info")
                        .executes(ChestShopCommand::shopInfo))
                .then(CommandManager.literal("list")
                        .executes(ChestShopCommand::listShops))
                .then(CommandManager.literal("setitem")
                        .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("quantity", IntegerArgumentType.integer(1, 64))
                                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("sell");
                                                    builder.suggest("buy");
                                                    builder.suggest("both");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ChestShopCommand::setItem)))))
                .then(CommandManager.literal("clearitem")
                        .executes(ChestShopCommand::clearItem))
        );
    }
    
    /**
     * Creates a new chest shop at the chest the player is looking at.
     */
    private static int createShop(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        BlockPos chestPos = getTargetedChest(player, world);
        
        if (chestPos == null) {
            player.sendMessage(Text.literal("You must be looking at a chest!").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Check if shop already exists
        ChestShop existingShop = ShopManager.get().getShopAt(chestPos, world);
        if (existingShop != null) {
            player.sendMessage(Text.literal("A shop already exists at this chest!").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Create the shop
        ChestShop shop = ShopManager.get().createShop(
                player.getUuid(),
                player.getName().getString(),
                chestPos,
                world
        );
        
        if (shop == null) {
            player.sendMessage(Text.literal("Failed to create shop!").formatted(Formatting.RED), false);
            return 0;
        }
        
        player.sendMessage(Text.literal("Shop created! ").formatted(Formatting.GREEN)
                .append(Text.literal("Use /cs setitem to configure.").formatted(Formatting.GRAY)), false);
        
        // Update hologram (will show nothing until item is set)
        ShopHologramManager.updateHologram(shop, world);
        
        return 1;
    }
    
    /**
     * Deletes the shop at the chest the player is looking at.
     */
    private static int deleteShop(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        BlockPos chestPos = getTargetedChest(player, world);
        
        if (chestPos == null) {
            player.sendMessage(Text.literal("You must be looking at a chest!").formatted(Formatting.RED), false);
            return 0;
        }
        
        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);
        
        if (shop == null) {
            player.sendMessage(Text.literal("There is no shop at this chest.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        // Check ownership (or op)
        if (!shop.isOwner(player.getUuid()) && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Remove hologram before deleting shop
        ShopHologramManager.removeHologram(shop, world);
        
        ShopManager.get().deleteShop(shop.getShopId());
        player.sendMessage(Text.literal("Shop deleted.").formatted(Formatting.GREEN), false);
        
        return 1;
    }
    
    /**
     * Shows info about the shop the player is looking at.
     */
    private static int shopInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        BlockPos chestPos = getTargetedChest(player, world);
        
        if (chestPos == null) {
            player.sendMessage(Text.literal("You must be looking at a chest!").formatted(Formatting.RED), false);
            return 0;
        }
        
        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);
        
        if (shop == null) {
            player.sendMessage(Text.literal("There is no shop at this chest.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        player.sendMessage(Text.literal("=== Shop Info ===").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("Owner: ").formatted(Formatting.GRAY)
                .append(Text.literal(shop.getOwnerName()).formatted(Formatting.WHITE)), false);
        player.sendMessage(Text.literal("Location: ").formatted(Formatting.GRAY)
                .append(Text.literal(shop.getPositionString()).formatted(Formatting.WHITE)), false);
        player.sendMessage(Text.literal("Status: ").formatted(Formatting.GRAY)
                .append(Text.literal(shop.isActive() ? "Active" : "Inactive")
                        .formatted(shop.isActive() ? Formatting.GREEN : Formatting.RED)), false);
        
        ShopEntry entry = shop.getEntry();
        if (entry == null) {
            player.sendMessage(Text.literal("Item: ").formatted(Formatting.GRAY)
                    .append(Text.literal("None configured").formatted(Formatting.YELLOW)), false);
        } else {
            String modeName = entry.getMode().name().toLowerCase();
            player.sendMessage(Text.literal("Item: ").formatted(Formatting.GRAY)
                    .append(Text.literal(entry.getItemIdString()).formatted(Formatting.WHITE)), false);
            player.sendMessage(Text.literal("Trade: ").formatted(Formatting.GRAY)
                    .append(Text.literal(entry.getQuantityPerTransaction() + "x for " + entry.getPricePerUnit() + " diamond(s)").formatted(Formatting.WHITE)), false);
            player.sendMessage(Text.literal("Mode: ").formatted(Formatting.GRAY)
                    .append(Text.literal(modeName).formatted(Formatting.AQUA)), false);
        }
        
        return 1;
    }
    
    /**
     * Lists all shops owned by the player.
     */
    private static int listShops(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        List<ChestShop> shops = ShopManager.get().getShopsByOwner(player.getUuid());
        
        if (shops.isEmpty()) {
            player.sendMessage(Text.literal("You don't own any shops.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        player.sendMessage(Text.literal("=== Your Shops (" + shops.size() + ") ===").formatted(Formatting.GOLD), false);
        for (ChestShop shop : shops) {
            String status = shop.isActive() ? "Active" : "Inactive";
            Formatting statusColor = shop.isActive() ? Formatting.GREEN : Formatting.RED;
            
            player.sendMessage(Text.literal("• ")
                    .append(Text.literal(shop.getPositionString()).formatted(Formatting.WHITE))
                    .append(Text.literal(" [" + status + "]").formatted(statusColor))
                    .append(Text.literal(" - " + shop.getEntries().size() + " items").formatted(Formatting.GRAY)), false);
        }
        
        return 1;
    }
    
    /**
     * Sets the item for the shop the player is looking at.
     */
    private static int setItem(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        BlockPos chestPos = getTargetedChest(player, world);
        
        if (chestPos == null) {
            player.sendMessage(Text.literal("You must be looking at a chest!").formatted(Formatting.RED), false);
            return 0;
        }
        
        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);
        
        if (shop == null) {
            player.sendMessage(Text.literal("There is no shop at this chest.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        // Check ownership
        if (!shop.isOwner(player.getUuid()) && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Get item from main hand
        ItemStack heldItem = player.getMainHandStack();
        if (heldItem.isEmpty()) {
            player.sendMessage(Text.literal("You must be holding an item!").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Parse arguments
        int price = IntegerArgumentType.getInteger(context, "price");
        int quantity = IntegerArgumentType.getInteger(context, "quantity");
        String modeStr = StringArgumentType.getString(context, "mode").toLowerCase();
        
        ShopMode mode;
        switch (modeStr) {
            case "sell":
                mode = ShopMode.SELL;
                break;
            case "buy":
                mode = ShopMode.BUY;
                break;
            case "both":
                mode = ShopMode.BOTH;
                break;
            default:
                player.sendMessage(Text.literal("Invalid mode! Use: sell, buy, or both").formatted(Formatting.RED), false);
                return 0;
        }
        
        // Create and set the entry
        ShopEntry entry = new ShopEntry(heldItem.getItem(), price, quantity, mode);
        shop.setEntry(entry);
        ShopManager.get().updateShop(shop);
        
        // Update hologram
        ShopHologramManager.updateHologram(shop, world);
        
        String itemName = Registries.ITEM.getId(heldItem.getItem()).toString();
        player.sendMessage(Text.literal("Set shop item to ").formatted(Formatting.GREEN)
                .append(Text.literal(itemName).formatted(Formatting.WHITE))
                .append(Text.literal(": ").formatted(Formatting.GREEN))
                .append(Text.literal(quantity + "x for " + price + " diamond(s), mode: " + modeStr).formatted(Formatting.GRAY)), false);
        
        return 1;
    }
    
    /**
     * Clears the item from the shop.
     */
    private static int clearItem(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        BlockPos chestPos = getTargetedChest(player, world);
        
        if (chestPos == null) {
            player.sendMessage(Text.literal("You must be looking at a chest!").formatted(Formatting.RED), false);
            return 0;
        }
        
        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);
        
        if (shop == null) {
            player.sendMessage(Text.literal("There is no shop at this chest.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        // Check ownership
        if (!shop.isOwner(player.getUuid()) && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
            return 0;
        }
        
        if (!shop.hasEntry()) {
            player.sendMessage(Text.literal("This shop has no item configured.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        shop.setEntry(null);
        ShopManager.get().updateShop(shop);
        
        // Update hologram (removes it since no item)
        ShopHologramManager.updateHologram(shop, world);
        
        player.sendMessage(Text.literal("Shop item cleared.").formatted(Formatting.GREEN), false);
        
        return 1;
    }
    
    /**
     * Gets the position of the chest the player is looking at.
     * 
     * @param player The player doing the raycast
     * @param world The world to check blocks in
     * @return The chest position, or null if not looking at a chest
     */
    private static BlockPos getTargetedChest(ServerPlayerEntity player, ServerWorld world) {
        // Raycast to find what block the player is looking at
        HitResult hitResult = player.raycast(5.0, 0.0f, false);
        
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        
        // Check if it's a chest
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return null;
        }
        
        return pos;
    }
}
