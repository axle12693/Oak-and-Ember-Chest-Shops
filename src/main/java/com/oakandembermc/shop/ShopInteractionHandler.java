package com.oakandembermc.shop;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ShopInteractionHandler {
    private ShopInteractionHandler() {
        /* This utility class should not be instantiated */
    }

    public static void register() {
        UseBlockCallback.EVENT.register(ShopInteractionHandler::onUseBlock);
        PlayerBlockBreakEvents.BEFORE.register(ShopInteractionHandler::onBlockBreak);
    }

    private static boolean onBlockBreak(Level world, Player player, BlockPos pos, BlockState state,
            BlockEntity blockEntity) {
        if (world.isClientSide()) {
            return true;
        }

        if (!(state.getBlock() instanceof ChestBlock)) {
            return true;
        }

        ChestShop shop = ShopManager.get().getShopAt(pos, world);
        if (shop == null) {
            return true;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (!shop.isOwner(player.getUUID()) && !world.getServer().getPlayerList().isOp(serverPlayer.nameAndId())) {
            serverPlayer.sendSystemMessage(
                    Component.literal("You cannot break someone else's shop!").withStyle(ChatFormatting.RED));
            return false;
        }

        ShopHologramManager.removeHologram(shop, (ServerLevel) world);
        ShopManager.get().deleteShop(shop.getShopId());

        serverPlayer.sendSystemMessage(Component.literal("Shop removed.").withStyle(ChatFormatting.YELLOW));

        return true;
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand,
            BlockHitResult hitResult) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (!(state.getBlock() instanceof ChestBlock)) {
            return InteractionResult.PASS;
        }

        ChestShop shop = ShopManager.get().getShopAt(pos, world);
        if (shop == null) {
            return InteractionResult.PASS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (shop.isOwner(player.getUUID()) || world.getServer().getPlayerList().isOp(serverPlayer.nameAndId())) {
            serverPlayer.sendOverlayMessage(
                    Component.literal("Your shop. Use /cs info to manage.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.PASS;
        }

        showShopInterface(serverPlayer, shop, world, pos);

        return InteractionResult.SUCCESS;
    }

    private static void showShopInterface(ServerPlayer player, ChestShop shop, Level world, BlockPos pos) {
        if (!shop.isActive()) {
            player.sendSystemMessage(
                    Component.literal("This shop is currently closed.").withStyle(ChatFormatting.YELLOW));
            return;
        }

        ShopEntry entry = shop.getEntry();
        if (entry == null) {
            player.sendSystemMessage(
                    Component.literal("This shop has no item for sale.").withStyle(ChatFormatting.YELLOW));
            return;
        }

        Container chestInventory = null;
        if (world.getBlockEntity(pos) instanceof ChestBlockEntity) {
            chestInventory = ChestBlock.getContainer((ChestBlock) world.getBlockState(pos).getBlock(),
                    world.getBlockState(pos), world, pos, true);
        }

        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(
                Component.literal("═══ " + shop.getOwnerName() + "'s Shop ═══").withStyle(ChatFormatting.GOLD));

        String itemName = getSimpleItemName(entry.getItemIdString());
        player.sendSystemMessage(Component.literal("Item: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(itemName).withStyle(ChatFormatting.WHITE)));

        if (entry.canPlayerBuy()) {
            int stock = chestInventory != null ? countItemInInventory(chestInventory, entry.getItem()) : 0;
            ChatFormatting stockColor = stock >= entry.getQuantityPerTransaction() ? ChatFormatting.GREEN
                    : ChatFormatting.RED;

            player.sendSystemMessage(Component.literal("BUY: ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(entry.getQuantityPerTransaction() + "x").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" for ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(entry.getTotalPrice() + " ◆").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" [" + stock + " in stock]").withStyle(stockColor)));
        }

        if (entry.canPlayerSell()) {
            int shopDiamonds = chestInventory != null ? countItemInInventory(chestInventory, Items.DIAMOND) : 0;
            ChatFormatting fundsColor = shopDiamonds >= entry.getTotalPrice() ? ChatFormatting.GREEN
                    : ChatFormatting.RED;

            player.sendSystemMessage(Component.literal("SELL: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(entry.getQuantityPerTransaction() + "x").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" for ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(entry.getTotalPrice() + " ◆").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" [" + shopDiamonds + " ◆ available]").withStyle(fundsColor)));
        }

        player.sendSystemMessage(Component.literal("Your diamonds: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(CurrencyHandler.getBalance(player)))
                        .withStyle(ChatFormatting.AQUA)));

        player.sendSystemMessage(Component.empty());
        if (entry.canPlayerBuy() && entry.canPlayerSell()) {
            player.sendSystemMessage(Component.literal("Use /csbuy or /cssell").withStyle(ChatFormatting.GRAY));
        } else if (entry.canPlayerBuy()) {
            player.sendSystemMessage(Component.literal("Use /csbuy to purchase").withStyle(ChatFormatting.GRAY));
        } else if (entry.canPlayerSell()) {
            player.sendSystemMessage(Component.literal("Use /cssell to sell").withStyle(ChatFormatting.GRAY));
        }
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

    private static int countItemInInventory(Container inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
