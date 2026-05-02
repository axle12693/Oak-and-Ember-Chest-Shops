package com.oakandembermc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.oakandembermc.shop.*;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ShopTradeCommand {
    private ShopTradeCommand() {
        /* This utility class should not be instantiated */
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {

        dispatcher.register(Commands.literal("csbuy")
                .executes(ShopTradeCommand::buyFromShop));

        dispatcher.register(Commands.literal("cssell")
                .executes(ShopTradeCommand::sellToShop));
    }

    private static int buyFromShop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        ServerLevel world = source.getLevel();
        BlockPos chestPos = getTargetedChest(player, world);

        if (chestPos == null) {
            player.sendSystemMessage(
                    Component.literal("You must be looking at a shop chest!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);
        if (shop == null) {
            player.sendSystemMessage(Component.literal("This chest is not a shop.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!shop.isActive()) {
            player.sendSystemMessage(
                    Component.literal("This shop is currently closed.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        ShopEntry entry = shop.getEntry();
        if (entry == null) {
            player.sendSystemMessage(
                    Component.literal("This shop has no item configured.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        if (!entry.canPlayerBuy()) {
            player.sendSystemMessage(
                    Component.literal("This shop is not selling items.").withStyle(ChatFormatting.RED));
            return 0;
        }

        Container chestInventory = getChestInventory(world, chestPos);
        if (chestInventory == null) {
            player.sendSystemMessage(
                    Component.literal("Error: Could not access shop inventory.").withStyle(ChatFormatting.RED));
            return 0;
        }

        TransactionHandler.TransactionResult result = TransactionHandler.executeBuy(player, shop, entry,
                chestInventory);
        TransactionHandler.sendResultMessage(player, result, entry);

        return result == TransactionHandler.TransactionResult.SUCCESS ? 1 : 0;
    }

    private static int sellToShop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        ServerLevel world = source.getLevel();
        BlockPos chestPos = getTargetedChest(player, world);

        if (chestPos == null) {
            player.sendSystemMessage(
                    Component.literal("You must be looking at a shop chest!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);
        if (shop == null) {
            player.sendSystemMessage(Component.literal("This chest is not a shop.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!shop.isActive()) {
            player.sendSystemMessage(
                    Component.literal("This shop is currently closed.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        ShopEntry entry = shop.getEntry();
        if (entry == null) {
            player.sendSystemMessage(
                    Component.literal("This shop has no item configured.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        if (!entry.canPlayerSell()) {
            player.sendSystemMessage(Component.literal("This shop is not buying items.").withStyle(ChatFormatting.RED));
            return 0;
        }

        Container chestInventory = getChestInventory(world, chestPos);
        if (chestInventory == null) {
            player.sendSystemMessage(
                    Component.literal("Error: Could not access shop inventory.").withStyle(ChatFormatting.RED));
            return 0;
        }

        TransactionHandler.TransactionResult result = TransactionHandler.executeSell(player, shop, entry,
                chestInventory);
        TransactionHandler.sendResultMessage(player, result, entry);

        return result == TransactionHandler.TransactionResult.SUCCESS ? 1 : 0;
    }

    private static BlockPos getTargetedChest(ServerPlayer player, ServerLevel world) {
        HitResult hitResult = player.pick(5.0, 0.0f, false);

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

    private static Container getChestInventory(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return null;
        }

        return ChestBlock.getContainer((ChestBlock) state.getBlock(), state, world, pos, true);
    }
}
