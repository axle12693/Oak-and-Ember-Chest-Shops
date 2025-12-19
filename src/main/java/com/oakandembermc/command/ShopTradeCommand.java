package com.oakandembermc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.oakandembermc.shop.*;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Commands for customers to buy from and sell to shops.
 */
public class ShopTradeCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        
        // /csbuy - buy from a shop
        dispatcher.register(CommandManager.literal("csbuy")
                .executes(ShopTradeCommand::buyFromShop));
        
        // /cssell - sell to a shop
        dispatcher.register(CommandManager.literal("cssell")
                .executes(ShopTradeCommand::sellToShop));
    }
    
    private static int buyFromShop(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        BlockPos chestPos = getTargetedChest(player, world);
        
        if (chestPos == null) {
            player.sendMessage(Text.literal("You must be looking at a shop chest!").formatted(Formatting.RED), false);
            return 0;
        }
        
        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);
        if (shop == null) {
            player.sendMessage(Text.literal("This chest is not a shop.").formatted(Formatting.RED), false);
            return 0;
        }
        
        if (!shop.isActive()) {
            player.sendMessage(Text.literal("This shop is currently closed.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        ShopEntry entry = shop.getEntry();
        if (entry == null) {
            player.sendMessage(Text.literal("This shop has no item configured.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        if (!entry.canPlayerBuy()) {
            player.sendMessage(Text.literal("This shop is not selling items.").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Get chest inventory
        Inventory chestInventory = getChestInventory(world, chestPos);
        if (chestInventory == null) {
            player.sendMessage(Text.literal("Error: Could not access shop inventory.").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Execute transaction
        TransactionHandler.TransactionResult result = TransactionHandler.executeBuy(player, shop, entry, chestInventory);
        TransactionHandler.sendResultMessage(player, result, entry);
        
        return result == TransactionHandler.TransactionResult.SUCCESS ? 1 : 0;
    }
    
    private static int sellToShop(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        BlockPos chestPos = getTargetedChest(player, world);
        
        if (chestPos == null) {
            player.sendMessage(Text.literal("You must be looking at a shop chest!").formatted(Formatting.RED), false);
            return 0;
        }
        
        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);
        if (shop == null) {
            player.sendMessage(Text.literal("This chest is not a shop.").formatted(Formatting.RED), false);
            return 0;
        }
        
        if (!shop.isActive()) {
            player.sendMessage(Text.literal("This shop is currently closed.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        ShopEntry entry = shop.getEntry();
        if (entry == null) {
            player.sendMessage(Text.literal("This shop has no item configured.").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        if (!entry.canPlayerSell()) {
            player.sendMessage(Text.literal("This shop is not buying items.").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Get chest inventory
        Inventory chestInventory = getChestInventory(world, chestPos);
        if (chestInventory == null) {
            player.sendMessage(Text.literal("Error: Could not access shop inventory.").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Execute transaction
        TransactionHandler.TransactionResult result = TransactionHandler.executeSell(player, shop, entry, chestInventory);
        TransactionHandler.sendResultMessage(player, result, entry);
        
        return result == TransactionHandler.TransactionResult.SUCCESS ? 1 : 0;
    }
    
    private static BlockPos getTargetedChest(ServerPlayerEntity player, ServerWorld world) {
        HitResult hitResult = player.raycast(5.0, 0.0f, false);
        
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return null;
        }
        
        return pos;
    }
    
    private static Inventory getChestInventory(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return null;
        }
        
        return ChestBlock.getInventory((ChestBlock) state.getBlock(), state, world, pos, true);
    }
}
