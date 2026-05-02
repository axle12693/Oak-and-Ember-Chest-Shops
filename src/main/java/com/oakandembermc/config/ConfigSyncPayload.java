package com.oakandembermc.config;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload to sync config options from server to client.
 * Sent when a player joins and when config changes.
 */
public record ConfigSyncPayload(
                boolean chestShopsEnabled,
                int maxShopsPerPlayer,
                int shopCreationCost,
                int transactionTax) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ConfigSyncPayload> ID = new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath("chestshop", "config_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> CODEC = StreamCodec.composite(
                        ByteBufCodecs.BOOL, ConfigSyncPayload::chestShopsEnabled,
                        ByteBufCodecs.VAR_INT, ConfigSyncPayload::maxShopsPerPlayer,
                        ByteBufCodecs.VAR_INT, ConfigSyncPayload::shopCreationCost,
                        ByteBufCodecs.VAR_INT, ConfigSyncPayload::transactionTax,
                        ConfigSyncPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
                return ID;
        }
}
