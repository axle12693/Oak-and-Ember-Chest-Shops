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

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class ChestShopCommand {
    private ChestShopCommand() {
        /* This utility class should not be instantiated */
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {

        dispatcher.register(Commands.literal("chestshop")
                .then(Commands.literal("create")
                        .executes(ChestShopCommand::createShop))
                .then(Commands.literal("delete")
                        .executes(ChestShopCommand::deleteShop))
                .then(Commands.literal("info")
                        .executes(ChestShopCommand::shopInfo))
                .then(Commands.literal("list")
                        .executes(ChestShopCommand::listShops))
                .then(Commands.literal("setitem")
                        .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("sell");
                                                    builder.suggest("buy");
                                                    builder.suggest("both");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ChestShopCommand::setItem)))))
                .then(Commands.literal("clearitem")
                        .executes(ChestShopCommand::clearItem)));

        dispatcher.register(Commands.literal("cs")
                .then(Commands.literal("create")
                        .executes(ChestShopCommand::createShop))
                .then(Commands.literal("delete")
                        .executes(ChestShopCommand::deleteShop))
                .then(Commands.literal("info")
                        .executes(ChestShopCommand::shopInfo))
                .then(Commands.literal("list")
                        .executes(ChestShopCommand::listShops))
                .then(Commands.literal("setitem")
                        .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("sell");
                                                    builder.suggest("buy");
                                                    builder.suggest("both");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ChestShopCommand::setItem)))))
                .then(Commands.literal("clearitem")
                        .executes(ChestShopCommand::clearItem)));
    }

    private static int createShop(CommandContext<CommandSourceStack> context) {
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
                    Component.literal("You must be looking at a chest!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChestShop existingShop = ShopManager.get().getShopAt(chestPos, world);
        if (existingShop != null) {
            player.sendSystemMessage(
                    Component.literal("A shop already exists at this chest!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChestShop shop = ShopManager.get().createShop(
                player.getUUID(),
                player.getName().getString(),
                chestPos,
                world);

        if (shop == null) {
            player.sendSystemMessage(Component.literal("Failed to create shop!").withStyle(ChatFormatting.RED));
            return 0;
        }

        player.sendSystemMessage(Component.literal("Shop created! ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Use /cs setitem to configure.").withStyle(ChatFormatting.GRAY)));

        ShopHologramManager.updateHologram(shop, world);

        return 1;
    }

    private static int deleteShop(CommandContext<CommandSourceStack> context) {
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
                    Component.literal("You must be looking at a chest!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);

        if (shop == null) {
            player.sendSystemMessage(
                    Component.literal("There is no shop at this chest.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        if (!shop.isOwner(player.getUUID()) && !world.getServer().getPlayerList().isOp(player.nameAndId())) {
            player.sendSystemMessage(Component.literal("You don't own this shop!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ShopHologramManager.removeHologram(shop, world);
        ShopManager.get().deleteShop(shop.getShopId());
        player.sendSystemMessage(Component.literal("Shop deleted.").withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int shopInfo(CommandContext<CommandSourceStack> context) {
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
                    Component.literal("You must be looking at a chest!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);

        if (shop == null) {
            player.sendSystemMessage(
                    Component.literal("There is no shop at this chest.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        player.sendSystemMessage(Component.literal("=== Shop Info ===").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(shop.getOwnerName()).withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("Location: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(shop.getPositionString()).withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("Status: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(shop.isActive() ? "Active" : "Inactive")
                        .withStyle(shop.isActive() ? ChatFormatting.GREEN : ChatFormatting.RED)));

        ShopEntry entry = shop.getEntry();
        if (entry == null) {
            player.sendSystemMessage(Component.literal("Item: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("None configured").withStyle(ChatFormatting.YELLOW)));
        } else {
            String modeName = entry.getMode().name().toLowerCase();
            player.sendSystemMessage(Component.literal("Item: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(entry.getItemIdString()).withStyle(ChatFormatting.WHITE)));
            player.sendSystemMessage(Component.literal("Trade: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(
                            entry.getQuantityPerTransaction() + "x for " + entry.getPricePerUnit() + " diamond(s)")
                            .withStyle(ChatFormatting.WHITE)));
            player.sendSystemMessage(Component.literal("Mode: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(modeName).withStyle(ChatFormatting.AQUA)));
        }

        return 1;
    }

    private static int listShops(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        List<ChestShop> shops = ShopManager.get().getShopsByOwner(player.getUUID());

        if (shops.isEmpty()) {
            player.sendSystemMessage(Component.literal("You don't own any shops.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        player.sendSystemMessage(
                Component.literal("=== Your Shops (" + shops.size() + ") ===").withStyle(ChatFormatting.GOLD));
        for (ChestShop shop : shops) {
            String status = shop.isActive() ? "Active" : "Inactive";
            ChatFormatting statusColor = shop.isActive() ? ChatFormatting.GREEN : ChatFormatting.RED;

            player.sendSystemMessage(Component.literal("• ")
                    .append(Component.literal(shop.getPositionString()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" [" + status + "]").withStyle(statusColor))
                    .append(Component.literal(" - " + shop.getEntries().size() + " items")
                            .withStyle(ChatFormatting.GRAY)));
        }

        return 1;
    }

    private static int setItem(CommandContext<CommandSourceStack> context) {
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
                    Component.literal("You must be looking at a chest!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);

        if (shop == null) {
            player.sendSystemMessage(
                    Component.literal("There is no shop at this chest.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        if (!shop.isOwner(player.getUUID()) && !world.getServer().getPlayerList().isOp(player.nameAndId())) {
            player.sendSystemMessage(Component.literal("You don't own this shop!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("You must be holding an item!").withStyle(ChatFormatting.RED));
            return 0;
        }

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
                player.sendSystemMessage(
                        Component.literal("Invalid mode! Use: sell, buy, or both").withStyle(ChatFormatting.RED));
                return 0;
        }

        ShopEntry entry = new ShopEntry(heldItem.getItem(), price, quantity, mode);
        shop.setEntry(entry);
        ShopManager.get().updateShop(shop);

        ShopHologramManager.updateHologram(shop, world);

        String itemName = BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();
        player.sendSystemMessage(Component.literal("Set shop item to ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(itemName).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(": ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(quantity + "x for " + price + " diamond(s), mode: " + modeStr)
                        .withStyle(ChatFormatting.GRAY)));

        return 1;
    }

    private static int clearItem(CommandContext<CommandSourceStack> context) {
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
                    Component.literal("You must be looking at a chest!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ChestShop shop = ShopManager.get().getShopAt(chestPos, world);

        if (shop == null) {
            player.sendSystemMessage(
                    Component.literal("There is no shop at this chest.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        if (!shop.isOwner(player.getUUID()) && !world.getServer().getPlayerList().isOp(player.nameAndId())) {
            player.sendSystemMessage(Component.literal("You don't own this shop!").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!shop.hasEntry()) {
            player.sendSystemMessage(
                    Component.literal("This shop has no item configured.").withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        shop.setEntry(null);
        ShopManager.get().updateShop(shop);

        ShopHologramManager.updateHologram(shop, world);

        player.sendSystemMessage(Component.literal("Shop item cleared.").withStyle(ChatFormatting.GREEN));

        return 1;
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
}
