package com.oakandembermc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.oakandembermc.shop.ChestShop;
import com.oakandembermc.shop.CurrencyHandler;
import com.oakandembermc.shop.ShopEntry;
import com.oakandembermc.shop.ShopManager;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LookupCommand {
    private LookupCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {

        dispatcher.register(Commands.literal("lookup")
                .then(Commands.argument("item", StringArgumentType.greedyString())
                        .suggests(LookupCommand::suggestItems)
                        .executes(LookupCommand::lookupItem)));
    }

    private static CompletableFuture<Suggestions> suggestItems(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();

        BuiltInRegistries.ITEM.keySet().forEach(id -> {
            String fullId = id.toString();
            String path = id.getPath();

            if (fullId.toLowerCase().contains(input) || path.toLowerCase().contains(input)) {
                builder.suggest(fullId);
            }
        });

        return builder.buildFuture();
    }

    private static int lookupItem(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String searchTerm = StringArgumentType.getString(context, "item").toLowerCase();

        Identifier exactId = Identifier.tryParse(searchTerm);

        List<ShopMatch> sellingShops = new ArrayList<>();
        List<ShopMatch> buyingShops = new ArrayList<>();

        for (ChestShop shop : ShopManager.get().getAllShops()) {
            if (!shop.isActive() || !shop.hasEntry()) {
                continue;
            }

            ShopEntry entry = shop.getEntry();
            Identifier itemId = entry.getItemId();

            boolean matches = false;
            if (exactId != null && itemId.equals(exactId)) {
                matches = true;
            } else {
                String itemIdStr = itemId.toString().toLowerCase();
                String itemPath = itemId.getPath().toLowerCase();
                if (itemIdStr.contains(searchTerm) || itemPath.contains(searchTerm)) {
                    matches = true;
                }
            }

            if (matches) {
                ShopMatch match = new ShopMatch(shop, entry);
                if (entry.canPlayerBuy()) {
                    sellingShops.add(match);
                }
                if (entry.canPlayerSell()) {
                    buyingShops.add(match);
                }
            }
        }

        if (sellingShops.isEmpty() && buyingShops.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No shops found for: ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(searchTerm).withStyle(ChatFormatting.WHITE)), false);
            return 0;
        }

        String currencyName = CurrencyHandler.getCurrencyNamePlural();

        source.sendSuccess(() -> Component.literal("=== Shop Lookup: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(searchTerm).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" ===").withStyle(ChatFormatting.GOLD)), false);

        if (!sellingShops.isEmpty()) {
            source.sendSuccess(() -> Component.literal("SELLING ")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .append(Component.literal("(you can buy):").withStyle(ChatFormatting.GREEN)
                            .withStyle(s -> s.withBold(false))),
                    false);

            for (ShopMatch match : sellingShops) {
                source.sendSuccess(() -> formatShopLine(match, currencyName), false);
            }
        }

        if (!buyingShops.isEmpty()) {
            if (!sellingShops.isEmpty()) {
                source.sendSuccess(Component::empty, false);
            }
            source.sendSuccess(() -> Component.literal("BUYING ")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                    .append(Component.literal("(you can sell):").withStyle(ChatFormatting.AQUA)
                            .withStyle(s -> s.withBold(false))),
                    false);

            for (ShopMatch match : buyingShops) {
                source.sendSuccess(() -> formatShopLine(match, currencyName), false);
            }
        }

        return sellingShops.size() + buyingShops.size();
    }

    private static Component formatShopLine(ShopMatch match, String currencyName) {
        ChestShop shop = match.shop;
        ShopEntry entry = match.entry;

        String priceStr = CurrencyHandler.format(entry.getTotalPrice());
        String coords = shop.getPositionString();
        String dimension = formatDimension(shop.getDimensionId());

        return Component.literal("  • ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(shop.getOwnerName()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(coords).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" " + dimension).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(entry.getQuantityPerTransaction() + "x").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" for ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(priceStr).withStyle(ChatFormatting.GOLD));
    }

    private static String formatDimension(String dimensionId) {
        if (dimensionId.equals("minecraft:overworld")) {
            return "[Overworld]";
        } else if (dimensionId.equals("minecraft:the_nether")) {
            return "[Nether]";
        } else if (dimensionId.equals("minecraft:the_end")) {
            return "[End]";
        } else {
            Identifier id = Identifier.tryParse(dimensionId);
            if (id != null) {
                return "[" + id.getPath() + "]";
            }
            return "[" + dimensionId + "]";
        }
    }

    private static class ShopMatch {
        final ChestShop shop;
        final ShopEntry entry;

        ShopMatch(ChestShop shop, ShopEntry entry) {
            this.shop = shop;
            this.entry = entry;
        }
    }
}
