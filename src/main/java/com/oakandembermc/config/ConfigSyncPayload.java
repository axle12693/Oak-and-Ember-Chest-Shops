package com.oakandembermc.config;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload to sync config options from server to client.
 * Sent when a player joins and when config changes.
 */
public record ConfigSyncPayload(
        boolean chestShopsEnabled,
        int maxShopsPerPlayer,
        int shopCreationCost,
        int transactionTax
) implements CustomPayload {
    
    public static final CustomPayload.Id<ConfigSyncPayload> ID = new CustomPayload.Id<>(
            Identifier.of("chestshop", "config_sync"));

    public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, ConfigSyncPayload::chestShopsEnabled,
            PacketCodecs.INTEGER, ConfigSyncPayload::maxShopsPerPlayer,
            PacketCodecs.INTEGER, ConfigSyncPayload::shopCreationCost,
            PacketCodecs.INTEGER, ConfigSyncPayload::transactionTax,
            ConfigSyncPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
