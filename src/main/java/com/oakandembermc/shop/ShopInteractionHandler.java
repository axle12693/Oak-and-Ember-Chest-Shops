package com.oakandembermc.shop;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Handles player interactions with chest shops.
 */
public class ShopInteractionHandler {
    
    /**
     * Registers the block use callback for shop interactions and block break listener.
     */
    public static void register() {
        UseBlockCallback.EVENT.register(ShopInteractionHandler::onUseBlock);
        PlayerBlockBreakEvents.BEFORE.register(ShopInteractionHandler::onBlockBreak);
    }
    
    /**
     * Handles block breaking - cleans up shop and hologram if a shop chest is broken.
     */
    private static boolean onBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, /* nullable */ net.minecraft.block.entity.BlockEntity blockEntity) {
        // Only handle on server side
        if (world.isClient()) {
            return true; // Allow break to proceed
        }
        
        // Only care about chests
        if (!(state.getBlock() instanceof ChestBlock)) {
            return true;
        }
        
        // Check if this is a shop
        ChestShop shop = ShopManager.get().getShopAt(pos, world);
        if (shop == null) {
            return true; // Not a shop, allow normal breaking
        }
        
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        
        // Only owner or ops can break shop chests
        if (!shop.isOwner(player.getUuid()) && !serverPlayer.hasPermissionLevel(2)) {
            serverPlayer.sendMessage(Text.literal("You cannot break someone else's shop!").formatted(Formatting.RED), false);
            return false; // Cancel the break
        }
        
        // Clean up hologram before the shop is deleted
        ShopHologramManager.removeHologram(shop, (ServerWorld) world);
        
        // Delete the shop
        ShopManager.get().deleteShop(shop.getShopId());
        
        serverPlayer.sendMessage(Text.literal("Shop removed.").formatted(Formatting.YELLOW), false);
        
        return true; // Allow the chest to be broken
    }
    
    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        // Only handle on server side
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        
        // Only handle main hand to avoid double-firing
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        
        // Only handle chests
        if (!(state.getBlock() instanceof ChestBlock)) {
            return ActionResult.PASS;
        }
        
        // Check if this is a shop
        ChestShop shop = ShopManager.get().getShopAt(pos, world);
        if (shop == null) {
            return ActionResult.PASS; // Not a shop, let normal chest opening happen
        }
        
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        
        // If owner or op, allow normal chest access
        if (shop.isOwner(player.getUuid()) || serverPlayer.hasPermissionLevel(2)) {
            serverPlayer.sendMessage(Text.literal("Your shop. Use /cs info to manage.").formatted(Formatting.GRAY), true);
            return ActionResult.PASS; // Let owner open the chest normally
        }
        
        // For non-owners, show shop interface and BLOCK chest access
        showShopInterface(serverPlayer, shop, world, pos);
        
        // Block normal chest opening for customers
        return ActionResult.SUCCESS;
    }
    
    /**
     * Shows the shop interface to a customer.
     */
    private static void showShopInterface(ServerPlayerEntity player, ChestShop shop, World world, BlockPos pos) {
        if (!shop.isActive()) {
            player.sendMessage(Text.literal("This shop is currently closed.").formatted(Formatting.YELLOW), false);
            return;
        }
        
        ShopEntry entry = shop.getEntry();
        if (entry == null) {
            player.sendMessage(Text.literal("This shop has no item for sale.").formatted(Formatting.YELLOW), false);
            return;
        }
        
        // Get chest inventory for stock checking
        Inventory chestInventory = null;
        if (world.getBlockEntity(pos) instanceof ChestBlockEntity) {
            chestInventory = ChestBlock.getInventory((ChestBlock) world.getBlockState(pos).getBlock(), 
                    world.getBlockState(pos), world, pos, true);
        }
        
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("═══ " + shop.getOwnerName() + "'s Shop ═══").formatted(Formatting.GOLD), false);
        
        // Show item name
        String itemName = getSimpleItemName(entry.getItemIdString());
        player.sendMessage(Text.literal("Item: ").formatted(Formatting.GRAY)
                .append(Text.literal(itemName).formatted(Formatting.WHITE)), false);
        
        // Show trade info based on mode
        if (entry.canPlayerBuy()) {
            int stock = chestInventory != null ? countItemInInventory(chestInventory, entry.getItem()) : 0;
            Formatting stockColor = stock >= entry.getQuantityPerTransaction() ? Formatting.GREEN : Formatting.RED;
            
            player.sendMessage(Text.literal("BUY: ").formatted(Formatting.GREEN)
                    .append(Text.literal(entry.getQuantityPerTransaction() + "x").formatted(Formatting.WHITE))
                    .append(Text.literal(" for ").formatted(Formatting.GRAY))
                    .append(Text.literal(entry.getTotalPrice() + " ◆").formatted(Formatting.AQUA))
                    .append(Text.literal(" [" + stock + " in stock]").formatted(stockColor)), false);
        }
        
        if (entry.canPlayerSell()) {
            int shopDiamonds = chestInventory != null ? countItemInInventory(chestInventory, Items.DIAMOND) : 0;
            Formatting fundsColor = shopDiamonds >= entry.getTotalPrice() ? Formatting.GREEN : Formatting.RED;
            
            player.sendMessage(Text.literal("SELL: ").formatted(Formatting.YELLOW)
                    .append(Text.literal(entry.getQuantityPerTransaction() + "x").formatted(Formatting.WHITE))
                    .append(Text.literal(" for ").formatted(Formatting.GRAY))
                    .append(Text.literal(entry.getTotalPrice() + " ◆").formatted(Formatting.AQUA))
                    .append(Text.literal(" [" + shopDiamonds + " ◆ available]").formatted(fundsColor)), false);
        }
        
        // Show player's balance
        player.sendMessage(Text.literal("Your diamonds: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(CurrencyHandler.getBalance(player))).formatted(Formatting.AQUA)), false);
        
        // Show commands
        player.sendMessage(Text.literal(""), false);
        if (entry.canPlayerBuy() && entry.canPlayerSell()) {
            player.sendMessage(Text.literal("Use /csbuy or /cssell").formatted(Formatting.GRAY), false);
        } else if (entry.canPlayerBuy()) {
            player.sendMessage(Text.literal("Use /csbuy to purchase").formatted(Formatting.GRAY), false);
        } else if (entry.canPlayerSell()) {
            player.sendMessage(Text.literal("Use /cssell to sell").formatted(Formatting.GRAY), false);
        }
    }
    
    private static String getSimpleItemName(String itemId) {
        // Convert "minecraft:diamond_sword" to "Diamond Sword"
        String name = itemId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }
        // Convert underscores to spaces and capitalize
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
    
    private static int countItemInInventory(Inventory inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
